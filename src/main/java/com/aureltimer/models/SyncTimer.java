package com.aureltimer.models;

import java.time.Instant;
import java.time.Duration;
import com.aureltimer.utils.TimeAuthority;

public class SyncTimer {
    private String expiresAt;
    private String createdBy;
    private String createdAt;
    private long initialDurationSeconds;
    private String predictedPhase;
    private String predictedPhaseDisplay;

    // Constructeurs
    public SyncTimer() {}

    public SyncTimer(String dimension, int minutes, int seconds, String createdBy, 
                     String predictedPhase, String predictedPhaseDisplay) {
        Instant now = TimeAuthority.getInstance().now();
        this.expiresAt = now.plusSeconds(minutes * 60L + seconds).toString();
        this.createdBy = createdBy;
        this.createdAt = now.toString();
        this.initialDurationSeconds = minutes * 60L + seconds;
        this.predictedPhase = predictedPhase;
        this.predictedPhaseDisplay = predictedPhaseDisplay;
    }

    // Méthodes utilitaires
    public Duration getTimeRemaining() {
        try {
            Instant expiry = Instant.parse(expiresAt);
            Instant now = TimeAuthority.getInstance().now();
            Duration remaining = Duration.between(now, expiry);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    public boolean isExpired() {
        try {
            Instant now = TimeAuthority.getInstance().now();
            Instant expiry = Instant.parse(expiresAt);
            boolean expired = now.isAfter(expiry);
            

            
            return expired;
        } catch (Exception e) {
            System.out.println("DEBUG isExpired ERROR for " + expiresAt + ": " + e.getMessage());
            return true; // Si erreur de parsing, considérer comme expiré
        }
    }

    public String getDisplayText() {
        Duration remaining = getTimeRemaining();
        if (remaining.isZero()) return "Expiré";
        
        long minutes = remaining.toMinutes();
        long seconds = remaining.getSeconds() % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    public String getFullDisplayText() {
        String timeText = getDisplayText();
        if (predictedPhaseDisplay != null && !predictedPhaseDisplay.isEmpty()) {
            return timeText + " - " + predictedPhaseDisplay;
        }
        return timeText;
    }

    public long getSecondsRemaining() {
        return getTimeRemaining().getSeconds();
    }

    public int getMinutesRemaining() {
        return (int) getTimeRemaining().toMinutes();
    }

    // Conversion vers DimensionTimer serveur-autoritaire
    public com.aureltimer.models.DimensionTimer toDimensionTimer(String dimensionName) {
        // Parse de l'heure d'expiration UTC (source de vérité)
        Instant expiresAtUtc;
        try {
            expiresAtUtc = Instant.parse(expiresAt);
        } catch (Exception e) {
            // Fallback si parsing échoue
            expiresAtUtc = TimeAuthority.getInstance().now().plusSeconds(initialDurationSeconds);
        }
        
        // Parse de l'heure de création UTC
        Instant createdAtUtc;
        try {
            createdAtUtc = Instant.parse(createdAt);
        } catch (Exception e) {
            // Fallback si parsing échoue
            createdAtUtc = TimeAuthority.getInstance().now().minus(
                java.time.Duration.ofSeconds(initialDurationSeconds)
            );
        }
        
        // Durée initiale
        java.time.Duration initialDuration = java.time.Duration.ofSeconds(initialDurationSeconds);
        
        // Phase prédite stockée (sans recalculer !)
        com.aureltimer.utils.TimeUtils.DayPhase storedPhase;
        try {
            storedPhase = com.aureltimer.utils.TimeUtils.DayPhase.valueOf(predictedPhase.toUpperCase());
        } catch (Exception e) {
            // Fallback si la phase n'est pas valide
            int originalMinutes = (int) (initialDurationSeconds / 60);
            int originalSeconds = (int) (initialDurationSeconds % 60);
            storedPhase = com.aureltimer.utils.TimeUtils.predictSpawnPhase(originalMinutes, originalSeconds);
        }
        
        // Création du TimerData puis wrapping en DimensionTimer
        com.aureltimer.models.TimerData timerData = new com.aureltimer.models.TimerData(
            dimensionName,
            expiresAtUtc,      // Source de vérité temporelle
            initialDuration,   // Durée originale
            storedPhase,       // Phase pré-calculée
            createdAtUtc,      // Timestamp création
            createdBy != null ? createdBy : "unknown"  // Créateur
        );
        
        return new com.aureltimer.models.DimensionTimer(timerData);
    }

    // Getters et Setters
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public long getInitialDurationSeconds() { return initialDurationSeconds; }
    public void setInitialDurationSeconds(long initialDurationSeconds) { 
        this.initialDurationSeconds = initialDurationSeconds; 
    }

    public String getPredictedPhase() { return predictedPhase; }
    public void setPredictedPhase(String predictedPhase) { this.predictedPhase = predictedPhase; }

    public String getPredictedPhaseDisplay() { return predictedPhaseDisplay; }
    public void setPredictedPhaseDisplay(String predictedPhaseDisplay) { 
        this.predictedPhaseDisplay = predictedPhaseDisplay; 
    }

    @Override
    public String toString() {
        return String.format("SyncTimer{dimension='%s', remaining='%s', createdBy='%s'}", 
                           "unknown", getDisplayText(), createdBy);
    }
}
