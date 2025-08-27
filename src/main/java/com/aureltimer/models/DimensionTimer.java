package com.aureltimer.models;

import com.aureltimer.utils.TimeUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DimensionTimer {
    private final String dimensionName;
    private final int initialMinutes;
    private final int initialSeconds;
    private final LocalDateTime spawnTime;
    private final LocalDateTime createdAt;
    private final TimeUtils.DayPhase predictedPhase;
    
    public DimensionTimer(String dimensionName, int minutes, int seconds, LocalDateTime spawnTime) {
        this.dimensionName = dimensionName;
        this.initialMinutes = minutes;
        this.initialSeconds = seconds;
        this.spawnTime = spawnTime;
        this.createdAt = LocalDateTime.now();
        // Prédire la phase du jour au moment du spawn
        this.predictedPhase = TimeUtils.predictSpawnPhase(minutes, seconds);
    }
    
    public String getDimensionName() {
        return dimensionName;
    }
    
    /**
     * Récupère la phase prédite du jour au moment du spawn
     */
    public TimeUtils.DayPhase getPredictedPhase() {
        return predictedPhase;
    }
    
    /**
     * Récupère le nom de la dimension avec la phase prédite
     * Format: "Ressource2 - Midi (6h-12h)"
     */
    public String getDimensionNameWithPhase() {
        return dimensionName + " - " + TimeUtils.formatPhase(predictedPhase);
    }
    
    public int getInitialMinutes() {
        return initialMinutes;
    }
    
    public int getInitialSeconds() {
        return initialSeconds;
    }
    
    public LocalDateTime getSpawnTime() {
        return spawnTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public String getFormattedTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(spawnTime)) {
            return "Terminé";
        }
        
        long totalSeconds = java.time.Duration.between(now, spawnTime).getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(spawnTime);
    }
    
    public long getSecondsRemaining() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(spawnTime)) {
            return 0;
        }
        return java.time.Duration.between(now, spawnTime).getSeconds();
    }
    
    @Override
    public String toString() {
        return String.format("DimensionTimer{dimension='%s', timeRemaining='%s'}", 
            dimensionName, getFormattedTimeRemaining());
    }
}
