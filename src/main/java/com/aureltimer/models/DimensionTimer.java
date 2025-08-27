package com.aureltimer.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DimensionTimer {
    private final String dimensionName;
    private final int initialMinutes;
    private final int initialSeconds;
    private final LocalDateTime spawnTime;
    private final LocalDateTime createdAt;
    
    public DimensionTimer(String dimensionName, int minutes, int seconds, LocalDateTime spawnTime) {
        this.dimensionName = dimensionName;
        this.initialMinutes = minutes;
        this.initialSeconds = seconds;
        this.spawnTime = spawnTime;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getDimensionName() {
        return dimensionName;
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
