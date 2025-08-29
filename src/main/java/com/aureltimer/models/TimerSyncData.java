package com.aureltimer.models;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.List;

public class TimerSyncData {
    private String version;
    private String lastUpdated;
    private int ttlMinutes;
    private SyncSettings settings;
    private Map<String, SyncTimer> timers;
    private SyncStats stats;

    // Constructeurs
    public TimerSyncData() {}

    public TimerSyncData(String version, int ttlMinutes) {
        this.version = version;
        this.ttlMinutes = ttlMinutes;
        this.lastUpdated = Instant.now().toString();
    }

    // Méthodes utilitaires
    public boolean needsRefresh() {
        if (lastUpdated == null) return true;
        
        try {
            Instant lastUpdate = Instant.parse(lastUpdated);
            Duration elapsed = Duration.between(lastUpdate, Instant.now());
            return elapsed.toMinutes() >= ttlMinutes;
        } catch (Exception e) {
            return true; // En cas d'erreur, forcer le refresh
        }
    }

    public void updateTimestamp() {
        this.lastUpdated = Instant.now().toString();
    }

    // Getters et Setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public int getTtlMinutes() { return ttlMinutes; }
    public void setTtlMinutes(int ttlMinutes) { this.ttlMinutes = ttlMinutes; }

    public SyncSettings getSettings() { return settings; }
    public void setSettings(SyncSettings settings) { this.settings = settings; }

    public Map<String, SyncTimer> getTimers() { return timers; }
    public void setTimers(Map<String, SyncTimer> timers) { this.timers = timers; }

    public SyncStats getStats() { return stats; }
    public void setStats(SyncStats stats) { this.stats = stats; }

    // Classe interne pour les paramètres de sync
    public static class SyncSettings {
        private boolean autoCleanupExpired = true;
        private int maxTimersPerDimension = 1;
        private boolean syncEnabled = true;

        // Getters et Setters
        public boolean isAutoCleanupExpired() { return autoCleanupExpired; }
        public void setAutoCleanupExpired(boolean autoCleanupExpired) { 
            this.autoCleanupExpired = autoCleanupExpired; 
        }

        public int getMaxTimersPerDimension() { return maxTimersPerDimension; }
        public void setMaxTimersPerDimension(int maxTimersPerDimension) { 
            this.maxTimersPerDimension = maxTimersPerDimension; 
        }

        public boolean isSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }
    }

    // Classe interne pour les statistiques
    public static class SyncStats {
        private int totalTimersCreated = 0;
        private int activeUsers24h = 0;

        // Getters et Setters
        public int getTotalTimersCreated() { return totalTimersCreated; }
        public void setTotalTimersCreated(int totalTimersCreated) { 
            this.totalTimersCreated = totalTimersCreated; 
        }

        public int getActiveUsers24h() { return activeUsers24h; }
        public void setActiveUsers24h(int activeUsers24h) { 
            this.activeUsers24h = activeUsers24h; 
        }
    }
}
