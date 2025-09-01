package com.aureltimer.models;

import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * SyncData - Modèle JSON pur pour sérialisation/désérialisation
 * 
 * RESPONSABILITÉ UNIQUE : Transport JSON avec le Gist GitHub
 * - Pas de logique métier
 * - Pas de calculs temporels
 * - Juste mapping JSON ↔ Java
 * 
 * SIMPLE : Structure plate qui correspond exactement au JSON
 * GSON-FRIENDLY : Propriétés publiques pour auto-mapping
 */
public class SyncData {
    
    // === PROPRIÉTÉS JSON (publiques pour Gson) ===
    public String expiresAt;                    // ISO-8601 UTC timestamp
    public String createdBy;                    // Nom utilisateur créateur
    public String createdAt;                    // ISO-8601 UTC timestamp création
    public long initialDurationSeconds;        // Durée originale en secondes
    public String predictedPhase;               // Phase Minecraft (enum string)
    public String predictedPhaseDisplay;       // Affichage user-friendly de la phase
    
    // === CONSTRUCTEURS ===
    
    /**
     * Constructeur vide pour Gson
     */
    public SyncData() {}
    
    /**
     * Constructeur depuis TimerData (conversion pour upload)
     */
    public SyncData(TimerData timerData) {
        this.expiresAt = timerData.getExpiresAtUtc().toString();
        this.createdBy = timerData.getCreatedBy();
        this.createdAt = timerData.getCreatedAtUtc().toString();
        this.initialDurationSeconds = timerData.getInitialDuration().getSeconds();
        this.predictedPhase = timerData.getPredictedPhase() != null ? 
                             timerData.getPredictedPhase().name().toLowerCase() : null;
        this.predictedPhaseDisplay = timerData.getPredictedPhase() != null ?
                                   com.aureltimer.utils.TimeUtils.getPhaseDisplay(timerData.getPredictedPhase()) : null;
    }
    
    /**
     * Constructeur legacy (compatibilité avec ancien SyncTimer)
     */
    public SyncData(String expiresAt, String createdBy, String createdAt, 
                   long initialDurationSeconds, String predictedPhase, String predictedPhaseDisplay) {
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.initialDurationSeconds = initialDurationSeconds;
        this.predictedPhase = predictedPhase;
        this.predictedPhaseDisplay = predictedPhaseDisplay;
    }
    
    // === CONVERSION VERS TimerData ===
    
    /**
     * Convertit ce SyncData vers TimerData (après download du JSON)
     */
    public TimerData toTimerData(String dimensionName) {
        try {
            // Parse des timestamps UTC
            Instant expiresAtUtc = parseInstant(expiresAt);
            Instant createdAtUtc = parseInstant(createdAt);
            
            // Parse de la durée
            Duration initialDuration = Duration.ofSeconds(initialDurationSeconds);
            
            // Parse de la phase
            com.aureltimer.utils.TimeUtils.DayPhase phase = parsePhase(predictedPhase);
            
            // Création du TimerData
            return new TimerData(dimensionName, expiresAtUtc, initialDuration, 
                               phase, createdAtUtc, createdBy);
                               
        } catch (Exception e) {
            // Fallback en cas d'erreur de parsing
            throw new IllegalArgumentException(
                "Impossible de convertir SyncData vers TimerData: " + e.getMessage(), e);
        }
    }
    
    // === MÉTHODES UTILITAIRES PRIVÉES ===
    
    /**
     * Parse un timestamp ISO-8601 avec fallback
     */
    private Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return com.aureltimer.utils.TimeAuthority.getInstance().now();
        }
        
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            // Log l'erreur mais continue avec un fallback
            System.err.println("Erreur parsing timestamp '" + timestamp + "': " + e.getMessage());
            return com.aureltimer.utils.TimeAuthority.getInstance().now();
        }
    }
    
    /**
     * Parse une phase avec fallback
     */
    private com.aureltimer.utils.TimeUtils.DayPhase parsePhase(String phaseStr) {
        if (phaseStr == null || phaseStr.trim().isEmpty()) {
            return com.aureltimer.utils.TimeUtils.DayPhase.MORNING; // Fallback par défaut
        }
        
        try {
            return com.aureltimer.utils.TimeUtils.DayPhase.valueOf(phaseStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log l'erreur mais continue avec un fallback
            System.err.println("Erreur parsing phase '" + phaseStr + "': " + e.getMessage());
            return com.aureltimer.utils.TimeUtils.DayPhase.MORNING;
        }
    }
    
    // === VALIDATION ===
    
    /**
     * ✅ VALIDATION JSON STRICTE - Vérifie intégrité complète des données
     * 
     * CRITÈRES STRICTS :
     * - expiresAt : parsable + futur (ou proche passé raisonnable)
     * - createdAt : parsable + cohérent avec expiresAt 
     * - initialDurationSeconds : > 0 et < 1 semaine
     * - predictedPhase : enum valide
     * - createdBy : non vide et alphanumérique
     */
    public ValidationResult validateStrict() {
        ValidationResult result = new ValidationResult();
        
        // 1. Validation expiresAt
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            result.addError("expiresAt manquant");
        } else {
            try {
                Instant expires = Instant.parse(expiresAt);
                Instant now = com.aureltimer.utils.TimeAuthority.getInstance().now();
                
                // Accepter ±12 heures pour gérer décalages timezone
                long deltaHours = java.time.Duration.between(now, expires).toHours();
                if (deltaHours < -12) {
                    result.addError("expiresAt trop ancien: " + expiresAt + " (delta: " + deltaHours + "h)");
                } else if (deltaHours > 7 * 24) { // Max 1 semaine
                    result.addError("expiresAt trop futur: " + expiresAt + " (delta: " + deltaHours + "h)");
                }
            } catch (Exception e) {
                result.addError("expiresAt invalide: " + expiresAt + " (" + e.getMessage() + ")");
            }
        }
        
        // 2. Validation createdAt
        if (createdAt == null || createdAt.trim().isEmpty()) {
            result.addError("createdAt manquant");
        } else {
            try {
                Instant created = Instant.parse(createdAt);
                Instant now = com.aureltimer.utils.TimeAuthority.getInstance().now();
                
                // CreatedAt ne peut pas être futur de plus de 1 heure
                if (created.isAfter(now.plusSeconds(3600))) {
                    result.addError("createdAt futur impossible: " + createdAt);
                }
                
                // CreatedAt ne peut pas être trop ancien (> 30 jours)
                if (created.isBefore(now.minusSeconds(30 * 24 * 3600))) {
                    result.addError("createdAt trop ancien: " + createdAt);
                }
                
                // Cohérence createdAt vs expiresAt
                if (expiresAt != null) {
                    try {
                        Instant expires = Instant.parse(expiresAt);
                        if (created.isAfter(expires)) {
                            result.addError("createdAt après expiresAt: " + createdAt + " > " + expiresAt);
                        }
                    } catch (Exception ignored) {} // Déjà traité plus haut
                }
            } catch (Exception e) {
                result.addError("createdAt invalide: " + createdAt + " (" + e.getMessage() + ")");
            }
        }
        
        // 3. Validation initialDurationSeconds
        if (initialDurationSeconds <= 0) {
            result.addError("initialDurationSeconds doit être > 0: " + initialDurationSeconds);
        } else if (initialDurationSeconds > 7 * 24 * 3600) { // Max 1 semaine
            result.addError("initialDurationSeconds trop long: " + initialDurationSeconds + "s (max 1 semaine)");
        }
        
        // ✅ 4. Validation predictedPhase TOLÉRANTE
        if (predictedPhase != null && !predictedPhase.trim().isEmpty()) {
            try {
                com.aureltimer.utils.TimeUtils.DayPhase.valueOf(predictedPhase.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ✅ TOLÉRANCE : garde string brute + WARN (forward-compat)
                result.addWarning("predictedPhase non-standard (toléré): " + predictedPhase);
            }
        }
        
        // 5. Validation createdBy
        if (createdBy == null || createdBy.trim().isEmpty()) {
            result.addError("createdBy manquant");
        } else if (createdBy.length() > 50) {
            result.addError("createdBy trop long: " + createdBy.length() + " chars (max 50)");
        } else if (!createdBy.matches("^[a-zA-Z0-9_\\- ]+$")) {
            result.addError("createdBy contient caractères invalides: " + createdBy);
        }
        
        return result;
    }
    
    /**
     * Validation simple (ancien comportement)
     */
    public boolean isValid() {
        return validateStrict().isValid();
    }
    
    /**
     * Classe pour résultats de validation
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }
        
        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
        
        @Override
        public String toString() {
            return isValid() ? "VALID" : "INVALID: " + getErrorsAsString();
        }
    }
    
    /**
     * Vérifie si le timer est expiré (basé sur JSON seulement)
     */
    public boolean isExpiredInJson() {
        try {
            Instant expiresAtUtc = Instant.parse(expiresAt);
            Instant now = com.aureltimer.utils.TimeAuthority.getInstance().now();
            return now.isAfter(expiresAtUtc);
        } catch (Exception e) {
            return true; // Si erreur parsing, considérer comme expiré
        }
    }
    
    /**
     * Temps restant basé sur JSON (pour cleanup sans conversion complète)
     */
    public Duration getTimeRemainingFromJson() {
        try {
            Instant expiresAtUtc = Instant.parse(expiresAt);
            Instant now = com.aureltimer.utils.TimeAuthority.getInstance().now();
            Duration remaining = Duration.between(now, expiresAtUtc);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }
    
    // === DEBUG ET MONITORING ===
    
    @Override
    public String toString() {
        return String.format(
            "SyncData{expiresAt='%s', createdBy='%s', initialDuration=%ds, phase='%s'}",
            expiresAt, createdBy, initialDurationSeconds, predictedPhase
        );
    }
    
    /**
     * Affichage debug détaillé
     */
    public String getDebugInfo() {
        return String.format(
            "SyncData{expiresAt=%s, createdBy=%s, createdAt=%s, " +
            "initialDurationSeconds=%d, predictedPhase=%s, predictedPhaseDisplay=%s, " +
            "valid=%s, expiredInJson=%s}",
            expiresAt, createdBy, createdAt, initialDurationSeconds, 
            predictedPhase, predictedPhaseDisplay, isValid(), isExpiredInJson()
        );
    }
    
    // === COMPARAISON ===
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SyncData other = (SyncData) obj;
        return java.util.Objects.equals(expiresAt, other.expiresAt) &&
               java.util.Objects.equals(createdBy, other.createdBy) &&
               initialDurationSeconds == other.initialDurationSeconds;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(expiresAt, createdBy, initialDurationSeconds);
    }
}
