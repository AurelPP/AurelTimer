package com.aureltimer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * TimeAuthority - Autorité temporelle synchronisée avec serveur
 * 
 * CORRECTIONS APPLIQUÉES :
 * - Bug 1 : baseClientNow maintenu correctement
 * - Bug 2 : RFC 1123 pour header HTTP Date
 * - Durcissement : recalage monotone, lissage EWMA, bornes sécurité
 * 
 * FONCTIONNALITÉS :
 * - Synchronisation automatique via headers HTTP Date
 * - Lissage EWMA pour éviter les sauts bruités
 * - Bornes de sécurité (±5 minutes max)
 * - Fallback robuste (garde skew précédent si erreur)
 * - Recalage monotone pour stabilité
 */
public final class TimeAuthority {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeAuthority.class);
    private static final TimeAuthority INSTANCE = new TimeAuthority();

    // === CONSTANTES DE SÉCURITÉ ===
    private static final long MAX_ABS_SKEW_MS = 5 * 60_000;  // ±5 minutes max
    private static final double ALPHA = 0.2;                 // Lissage EWMA (20% nouveau, 80% ancien)
    
    // === ÉTAT TEMPOREL (volatile pour thread-safety) ===
    private volatile long skewMillis = 0;                     // serverNow - clientNow (lissé)
    private volatile long baseNano = System.nanoTime();       // Référence nanotime
    private volatile long baseClientNow = System.currentTimeMillis(); // Référence client
    
    // === FORMATTERS ===
    private static final DateTimeFormatter RFC1123 = 
            DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);

    private TimeAuthority() {}

    public static TimeAuthority getInstance() {
        return INSTANCE;
    }

    /**
     * Met à jour le décalage horaire via header HTTP Date
     * 
     * ROBUSTESSE :
     * - Parse RFC 1123 (format HTTP standard)
     * - Borne le skew à ±5 minutes (sécurité)
     * - Lisse avec EWMA (évite sauts bruités)
     * - Recalage monotone (baseNano + baseClientNow ensemble)
     * - Fallback : garde skew précédent si erreur
     * 
     * @param dateHeader Header Date HTTP (ex: "Fri, 30 Aug 2025 11:30:15 GMT")
     */
    public void updateFromHttpDateHeader(String dateHeader) {
        if (dateHeader == null || dateHeader.trim().isEmpty()) {
            LOGGER.debug("⏰ Header Date vide - conservation du skew actuel: {}ms", skewMillis);
            return;
        }
        
        try {
            // Parse RFC 1123 (format HTTP standard) - FIX BUG 2
            Instant serverInstant = ZonedDateTime.parse(dateHeader, RFC1123).toInstant();
            long clientNow = System.currentTimeMillis();
            long rawSkew = serverInstant.toEpochMilli() - clientNow;
            
            // Borne de sécurité : clamp à ±5 minutes
            long clampedSkew = rawSkew;
            if (Math.abs(rawSkew) > MAX_ABS_SKEW_MS) {
                clampedSkew = rawSkew > 0 ? MAX_ABS_SKEW_MS : -MAX_ABS_SKEW_MS;
                LOGGER.warn("⚠️ Skew excessif détecté: {}ms → clampé à {}ms", rawSkew, clampedSkew);
            }
            
            // Lissage EWMA : évite les sauts bruités
            long smoothedSkew = (long) (ALPHA * clampedSkew + (1 - ALPHA) * skewMillis);
            
            // Recalage monotone atomique - FIX BUG 1 : capture ensemble pour cohérence
            long newBaseNano = System.nanoTime();
            long newBaseClient = clientNow;
            
            // Mise à jour atomique (ordre important pour visibilité)
            this.baseNano = newBaseNano;
            this.baseClientNow = newBaseClient;
            this.skewMillis = smoothedSkew;
            
            LOGGER.info("⏰ Sync temporelle - Raw: {}ms, Clamped: {}ms, Smoothed: {}ms (client {})",
                    rawSkew, clampedSkew, smoothedSkew, 
                    (smoothedSkew > 0 ? "en retard" : "en avance"));

        } catch (DateTimeParseException e) {
            LOGGER.debug("⏰ Erreur parsing Date HTTP '{}': {} - conservation skew actuel", 
                        dateHeader, e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("⏰ Erreur inattendue TimeAuthority: {} - conservation skew actuel", 
                       e.getMessage());
        }
    }

    /**
     * Retourne l'heure actuelle synchronisée avec le serveur (UTC)
     * 
     * PRÉCISION :
     * - Utilise System.nanoTime() pour monotonie
     * - Applique skew lissé pour correction serveur
     * - Thread-safe (lectures atomiques volatiles)
     * 
     * @return Instant actuel corrigé par décalage serveur
     */
    public Instant now() {
        // Capture atomique des références volatiles - FIX BUG 1 : baseClientNow maintenu
        long currentBaseNano = baseNano;
        long currentBaseClient = baseClientNow;
        long currentSkew = skewMillis;
        
        // Temps écoulé depuis dernier recalage (monotone)
        long dtNano = System.nanoTime() - currentBaseNano;
        
        // Client "maintenant" approximatif
        long approxClientNow = currentBaseClient + TimeUnit.NANOSECONDS.toMillis(dtNano);
        
        // Application du skew serveur
        return Instant.ofEpochMilli(approxClientNow + currentSkew);
    }

    /**
     * Retourne le skew actuel en millisecondes
     */
    public long getSkewMillis() {
        return skewMillis;
    }
    
    /**
     * Vérifie si TimeAuthority a été synchronisé (skew != 0)
     */
    public boolean isSynchronized() {
        return skewMillis != 0;
    }

    /**
     * Informations de debug détaillées
     */
    public String getDebugInfo() {
        return String.format(
            "TimeAuthority{skew=%dms, baseNano=%d, baseClient=%d, synchronized=%s, now=%s}",
            skewMillis, baseNano, baseClientNow, isSynchronized(), now().toString()
        );
    }
    
    /**
     * Informations de debug compactes
     */
    public String getCompactDebugInfo() {
        return String.format("skew=%dms, sync=%s", skewMillis, isSynchronized());
    }
}