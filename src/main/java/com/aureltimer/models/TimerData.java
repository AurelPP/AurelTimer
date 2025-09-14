package com.aureltimer.models;

import com.aureltimer.utils.TimeUtils;
import com.aureltimer.utils.TimeAuthority;
import java.time.Instant;
import java.time.Duration;

/**
 * TimerData - Modèle de données pur pour timers
 * 
 * RESPONSABILITÉ UNIQUE : Stocker et calculer les données temporelles
 * - Pas de logique UI
 * - Pas de conversion JSON  
 * - Juste les données et calculs purs
 * 
 * IMMUTABLE : Une fois créé, les données ne changent pas
 * THREAD-SAFE : Toutes les méthodes sont stateless ou atomic
 */
public final class TimerData {
    
    // === DONNÉES CORE (immutables) ===
    private final String dimensionName;
    private final Instant expiresAtUtc;           // Source de vérité temporelle
    private final Duration initialDuration;       // Durée originale
    private final TimeUtils.DayPhase predictedPhase; // Phase calculée
    private final Instant createdAtUtc;           // Timestamp création
    private final String createdBy;               // Créateur du timer
    
    // === RÉFÉRENCE TEMPORELLE ===
    private final TimeAuthority timeAuthority;
    
    /**
     * Constructeur principal - Données complètes
     */
    public TimerData(String dimensionName, Instant expiresAtUtc, Duration initialDuration,
                     TimeUtils.DayPhase predictedPhase, Instant createdAtUtc, String createdBy) {
        this.dimensionName = dimensionName;
        this.expiresAtUtc = expiresAtUtc;
        this.initialDuration = initialDuration;
        this.predictedPhase = predictedPhase;
        this.createdAtUtc = createdAtUtc;
        this.createdBy = createdBy;
        this.timeAuthority = TimeAuthority.getInstance();
    }
    
    /**
     * Factory method - Créer depuis une durée
     */
    public static TimerData createFromDuration(String dimensionName, Duration duration, 
                                             String createdBy) {
        TimeAuthority timeAuth = TimeAuthority.getInstance();
        Instant now = timeAuth.now();
        Instant expiresAt = now.plus(duration);
        
        // Calculer la phase prédite
        int minutes = (int) duration.toMinutes();
        int seconds = (int) (duration.getSeconds() % 60);
        TimeUtils.DayPhase phase = TimeUtils.predictSpawnPhase(minutes, seconds);
        
        return new TimerData(dimensionName, expiresAt, duration, phase, now, createdBy);
    }
    
    /**
     * Factory method - Créer depuis minutes/secondes (compatibilité)
     */
    public static TimerData createFromMinutesSeconds(String dimensionName, int minutes, int seconds,
                                                   String createdBy) {
        Duration duration = Duration.ofMinutes(minutes).plusSeconds(seconds);
        return createFromDuration(dimensionName, duration, createdBy);
    }
    
    // === CALCULS TEMPORELS (thread-safe, stateless) ===
    
    /**
     * Temps restant en secondes
     * SERVEUR-AUTORITAIRE : Basé sur heure fixe d'expiration
     */
    public long getSecondsRemaining() {
        Instant now = timeAuthority.now();
        Duration remaining = Duration.between(now, expiresAtUtc);
        
        // Si le timer est expiré, retourner 0
        if (remaining.isNegative() || remaining.isZero()) {
            return 0;
        }
        
        // Sinon, retourner le temps restant réel
        return remaining.getSeconds();
    }
    
    /**
     * Temps restant comme Duration
     */
    public Duration getTimeRemaining() {
        return Duration.ofSeconds(getSecondsRemaining());
    }
    
    /**
     * Vérifie si expiré
     */
    public boolean isExpired() {
        return timeAuthority.now().isAfter(expiresAtUtc);
    }
    
    /**
     * Pourcentage de progression (0-100)
     * STABLE : Basé sur durée initiale fixe
     */
    public double getProgressPercentage() {
        long remainingSeconds = getSecondsRemaining();
        long totalSeconds = Math.max(1, initialDuration.getSeconds());
        long elapsedSeconds = Math.max(0, totalSeconds - remainingSeconds);
        
        return Math.min(100.0, (elapsedSeconds * 100.0) / totalSeconds);
    }
    
    /**
     * Temps écoulé depuis création
     */
    public Duration getElapsedTime() {
        Instant now = timeAuthority.now();
        return Duration.between(createdAtUtc, now);
    }
    
    /**
     * Temps total du timer (même si expiré)
     */
    public Duration getTotalDuration() {
        return Duration.between(createdAtUtc, expiresAtUtc);
    }
    
    // === GETTERS (immutables, thread-safe) ===
    
    public String getDimensionName() {
        return dimensionName;
    }
    
    public Instant getExpiresAtUtc() {
        return expiresAtUtc;
    }
    
    public Duration getInitialDuration() {
        return initialDuration;
    }
    
    public TimeUtils.DayPhase getPredictedPhase() {
        return predictedPhase;
    }
    
    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Vérifie si le timer a été créé par un utilisateur spécifique
     */
    public boolean isCreatedBy(String username) {
        return createdBy != null && createdBy.equals(username);
    }
    
    /**
     * Vérifie si le timer expire dans les X secondes
     */
    public boolean expiresWithin(Duration duration) {
        return getTimeRemaining().compareTo(duration) <= 0;
    }
    
    /**
     * Création d'une copie avec nouvelle expiration (pour updates)
     */
    public TimerData withNewExpiration(Instant newExpiresAt) {
        return new TimerData(dimensionName, newExpiresAt, initialDuration, 
                           predictedPhase, createdAtUtc, createdBy);
    }
    
    /**
     * Création d'une copie avec nouvelle durée (pour updates)
     */
    public TimerData withNewDuration(Duration newDuration) {
        Instant newExpiresAt = createdAtUtc.plus(newDuration);
        return new TimerData(dimensionName, newExpiresAt, newDuration,
                           predictedPhase, createdAtUtc, createdBy);
    }
    
    // === COMPARAISON ET HASH ===
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TimerData other = (TimerData) obj;
        return dimensionName.equals(other.dimensionName) &&
               expiresAtUtc.equals(other.expiresAtUtc) &&
               initialDuration.equals(other.initialDuration);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(dimensionName, expiresAtUtc, initialDuration);
    }
    
    @Override
    public String toString() {
        return String.format(
            "TimerData{dimension='%s', expiresAt='%s', remaining=%ds, progress=%.1f%%, phase='%s'}",
            dimensionName, expiresAtUtc, getSecondsRemaining(), 
            getProgressPercentage(), predictedPhase
        );
    }
    
    /**
     * Informations debug détaillées
     */
    public String getDebugInfo() {
        return String.format(
            "TimerData{dimension=%s, expiresAt=%s, initialDuration=%s, remaining=%ds, " +
            "progress=%.2f%%, phase=%s, createdBy=%s, createdAt=%s, elapsed=%s, timeAuth=%s}",
            dimensionName, expiresAtUtc, initialDuration, getSecondsRemaining(),
            getProgressPercentage(), predictedPhase, createdBy, createdAtUtc,
            getElapsedTime(), timeAuthority.getDebugInfo()
        );
    }
}

