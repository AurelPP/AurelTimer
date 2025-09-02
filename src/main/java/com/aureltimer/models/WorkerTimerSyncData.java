package com.aureltimer.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map;
import java.util.HashMap;

/**
 * ✅ WORKER TIMER SYNC DATA - STRUCTURE JSON GLOBALE
 * 
 * Structure pour le JSON complet qui contient tous les timers
 * Compatible avec le format Worker existant (différente de l'ancienne TimerSyncData)
 */
public class WorkerTimerSyncData {
    
    // ===== MÉTADONNÉES =====
    public String version = "1.0.0";
    public String lastUpdated;
    public int ttlMinutes = 60;
    
    // ===== SETTINGS =====
    public Settings settings = new Settings();
    
    // ===== TIMERS =====
    public Map<String, SyncTimer> timers = new HashMap<>();
    
    // ===== STATS =====
    public Stats stats = new Stats();
    
    // ETag pour cache
    private String etag;
    
    // Gson instance
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    

    
    // Getters/Setters
    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
    
    public Map<String, SyncTimer> getTimers() { return timers; }
    
    /**
     * Parse JSON vers WorkerTimerSyncData
     */
    public static WorkerTimerSyncData fromJson(String json) {
        try {
            return GSON.fromJson(json, WorkerTimerSyncData.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convertit vers JSON
     */
    public String toJson() {
        return GSON.toJson(this);
    }
    
    /**
     * ✅ TIMER INDIVIDUEL DANS LE JSON
     */
    public static class SyncTimer {
        public String expiresAt;
        public String createdBy;
        public String createdAt;
        public int initialDurationSeconds;
        public String predictedPhase;
        public String predictedPhaseDisplay;
        
        // Constructeur vide pour Gson
        public SyncTimer() {}
        
        // Constructeur depuis SyncData
        public SyncTimer(SyncData syncData) {
            this.expiresAt = syncData.expiresAt;
            this.createdBy = syncData.createdBy;
            this.createdAt = syncData.createdAt;
            this.initialDurationSeconds = (int) syncData.initialDurationSeconds;
            this.predictedPhase = syncData.predictedPhase;
            this.predictedPhaseDisplay = syncData.predictedPhaseDisplay;
        }
        
        // Constructeur depuis TimerData
        public SyncTimer(TimerData timerData) {
            this.expiresAt = timerData.getExpiresAtUtc().toString();
            this.createdBy = timerData.getCreatedBy();
            this.createdAt = timerData.getCreatedAtUtc().toString();
            this.initialDurationSeconds = (int) timerData.getInitialDuration().getSeconds();
            this.predictedPhase = timerData.getPredictedPhase() != null ? 
                                 timerData.getPredictedPhase().name().toLowerCase() : "unknown";
            this.predictedPhaseDisplay = timerData.getPredictedPhase() != null ?
                                       com.aureltimer.utils.TimeUtils.getPhaseDisplay(timerData.getPredictedPhase()) : "Unknown";
        }
        
        // Conversion vers TimerData
        public TimerData toTimerData(String dimensionName) {
            try {
                java.time.Instant expiresAtUtc = java.time.Instant.parse(this.expiresAt);
                java.time.Instant createdAtUtc = java.time.Instant.parse(this.createdAt);
                java.time.Duration initialDuration = java.time.Duration.ofSeconds(this.initialDurationSeconds);
                
                com.aureltimer.utils.TimeUtils.DayPhase phase = null;
                try {
                    phase = com.aureltimer.utils.TimeUtils.DayPhase.valueOf(this.predictedPhase.toUpperCase());
                } catch (Exception e) {
                    phase = com.aureltimer.utils.TimeUtils.DayPhase.DAY; // default
                }
                
                return new TimerData(dimensionName, expiresAtUtc, initialDuration, phase, createdAtUtc, this.createdBy);
            } catch (Exception e) {
                throw new RuntimeException("Erreur conversion SyncTimer vers TimerData", e);
            }
        }
    }
    
    /**
     * ✅ SETTINGS DU JSON
     */
    public static class Settings {
        public boolean autoCleanupExpired = true;
        public int maxTimersPerDimension = 1;
        public boolean syncEnabled = true;
    }
    
    /**
     * ✅ STATS DU JSON
     */
    public static class Stats {
        public int totalTimersCreated = 0;
        public int activeUsers24h = 0;
    }
    
    /**
     * Constructeur vide pour Gson
     */
    public WorkerTimerSyncData() {
        this.lastUpdated = java.time.Instant.now().toString();
    }
    
    /**
     * ✅ CONVERSION VERS/DEPUIS SYNCDATA (pour compatibilité)
     */
    public SyncData getSyncDataForDimension(String dimensionName) {
        SyncTimer timer = timers.get(dimensionName);
        if (timer == null) return null;
        
        SyncData syncData = new SyncData();
        syncData.expiresAt = timer.expiresAt;
        syncData.createdBy = timer.createdBy;
        syncData.createdAt = timer.createdAt;
        syncData.initialDurationSeconds = timer.initialDurationSeconds;
        syncData.predictedPhase = timer.predictedPhase;
        syncData.predictedPhaseDisplay = timer.predictedPhaseDisplay;
        
        return syncData;
    }
    
    /**
     * ✅ AJOUTER/METTRE À JOUR UN TIMER
     */
    public void putTimer(String dimensionName, TimerData timerData) {
        timers.put(dimensionName, new SyncTimer(timerData));
        lastUpdated = java.time.Instant.now().toString();
        stats.totalTimersCreated++;
    }
    
    /**
     * ✅ SUPPRIMER UN TIMER
     */
    public void removeTimer(String dimensionName) {
        timers.remove(dimensionName);
        lastUpdated = java.time.Instant.now().toString();
    }
    
    /**
     * ✅ CLEANUP EXPIRED TIMERS
     */
    public void cleanupExpired() {
        if (!settings.autoCleanupExpired) return;
        
        java.time.Instant now = com.aureltimer.utils.TimeAuthority.getInstance().now();
        
        timers.entrySet().removeIf(entry -> {
            try {
                java.time.Instant expires = java.time.Instant.parse(entry.getValue().expiresAt);
                return expires.isBefore(now);
            } catch (Exception e) {
                // En cas d'erreur de parsing, supprimer
                return true;
            }
        });
        
        lastUpdated = now.toString();
    }
    
    /**
     * ✅ COPIE POUR IMMUTABILITÉ
     */
    public WorkerTimerSyncData copy() {
        WorkerTimerSyncData copy = new WorkerTimerSyncData();
        copy.version = this.version;
        copy.lastUpdated = this.lastUpdated;
        copy.ttlMinutes = this.ttlMinutes;
        
        // Deep copy settings
        copy.settings = new Settings();
        copy.settings.autoCleanupExpired = this.settings.autoCleanupExpired;
        copy.settings.maxTimersPerDimension = this.settings.maxTimersPerDimension;
        copy.settings.syncEnabled = this.settings.syncEnabled;
        
        // Deep copy timers
        copy.timers = new HashMap<>(this.timers);
        
        // Deep copy stats
        copy.stats = new Stats();
        copy.stats.totalTimersCreated = this.stats.totalTimersCreated;
        copy.stats.activeUsers24h = this.stats.activeUsers24h;
        
        return copy;
    }
}