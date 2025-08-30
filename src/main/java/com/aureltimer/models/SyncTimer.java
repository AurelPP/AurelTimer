package com.aureltimer.models;

import java.time.Instant;
import java.time.Duration;

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
        Instant now = Instant.now();
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
            Instant now = Instant.now();
            Duration remaining = Duration.between(now, expiry);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    public boolean isExpired() {
        try {
            return Instant.now().isAfter(Instant.parse(expiresAt));
        } catch (Exception e) {
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

    // Conversion vers DimensionTimer existant
    public com.aureltimer.models.DimensionTimer toDimensionTimer(String dimensionName) {
        Duration remaining = getTimeRemaining();
        int minutesRemaining = (int) remaining.toMinutes();
        int secondsRemaining = (int) (remaining.getSeconds() % 60);
        
        // Calculer le spawnTime basé sur le temps restant
        java.time.LocalDateTime spawnTime = java.time.LocalDateTime.now()
            .plusMinutes(minutesRemaining)
            .plusSeconds(secondsRemaining);
        
        // Utiliser la durée ORIGINALE pour la barre de progression (pas le temps restant)
        int originalMinutes = (int) (initialDurationSeconds / 60);
        int originalSeconds = (int) (initialDurationSeconds % 60);
        
        // Récupérer la phase prédite stockée (sans recalculer !)
        com.aureltimer.utils.TimeUtils.DayPhase storedPhase;
        try {
            storedPhase = com.aureltimer.utils.TimeUtils.DayPhase.valueOf(predictedPhase.toUpperCase());
        } catch (Exception e) {
            // Fallback si la phase n'est pas valide
            storedPhase = com.aureltimer.utils.TimeUtils.predictSpawnPhase(originalMinutes, originalSeconds);
        }
        
        com.aureltimer.models.DimensionTimer timer = new com.aureltimer.models.DimensionTimer(
            dimensionName, originalMinutes, originalSeconds, spawnTime, storedPhase
        );
        
        return timer;
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
