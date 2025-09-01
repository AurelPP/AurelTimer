package com.aureltimer.models;

import com.aureltimer.utils.TimeUtils;

/**
 * DimensionTimer - Couche UI/Display pour timers
 * 
 * RESPONSABILITÉ UNIQUE : Présentation et compatibilité UI
 * - Wrappeur autour de TimerData
 * - Méthodes de formatage pour l'affichage
 * - Compatibilité avec l'ancienne API
 * 
 * DESIGN PATTERN : Adapter/Facade
 * - Cache un TimerData en interne
 * - Expose l'ancienne interface publique
 * - Toute la logique est déléguée à TimerData
 */
public final class DimensionTimer {
    
    // === DONNÉES CORE ===
    private final TimerData timerData;  // Délégation vers modèle de données pur
    
    // === CONSTRUCTEURS ===
    
    /**
     * Constructeur principal - Wrapping de TimerData
     */
    public DimensionTimer(TimerData timerData) {
        this.timerData = timerData;
    }
    
    /**
     * Constructeur depuis composants (pour compatibilité)
     */
    public DimensionTimer(String dimensionName, java.time.Instant expiresAtUtc, 
                         java.time.Duration initialDuration, TimeUtils.DayPhase predictedPhase, 
                         java.time.Instant createdAtUtc, String createdBy) {
        this.timerData = new TimerData(dimensionName, expiresAtUtc, initialDuration,
                                     predictedPhase, createdAtUtc, createdBy);
    }
    
    /**
     * Factory depuis durée (recommandé)
     */
    public static DimensionTimer createFromDuration(String dimensionName, java.time.Duration duration,
                                                   String createdBy) {
        TimerData data = TimerData.createFromDuration(dimensionName, duration, createdBy);
        return new DimensionTimer(data);
    }
    
    /**
     * Factory depuis minutes/secondes (compatibilité)
     */
    public static DimensionTimer createFromMinutesSeconds(String dimensionName, int minutes, 
                                                        int seconds, String createdBy) {
        TimerData data = TimerData.createFromMinutesSeconds(dimensionName, minutes, seconds, createdBy);
        return new DimensionTimer(data);
    }
    
    // === CONSTRUCTEURS DE COMPATIBILITÉ (dépréciés) ===
    
    @Deprecated
    public DimensionTimer(String dimensionName, int minutes, int seconds, 
                         java.time.LocalDateTime spawnTime) {
        this.timerData = TimerData.createFromMinutesSeconds(dimensionName, minutes, seconds, "local");
    }
    
    @Deprecated
    public DimensionTimer(String dimensionName, int minutes, int seconds,
                         java.time.LocalDateTime spawnTime, TimeUtils.DayPhase predictedPhase,
                         java.time.LocalDateTime createdAt) {
        this.timerData = TimerData.createFromMinutesSeconds(dimensionName, minutes, seconds, "local");
    }
    
    // === MÉTHODES UI/DISPLAY ===
    
    /**
     * Formatage du temps restant pour affichage
     */
    public String getDisplayText() {
        long seconds = timerData.getSecondsRemaining();
        if (seconds <= 0) return "Expiré";
        
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", remainingSeconds);
        }
    }
    
    /**
     * Affichage complet avec phase prédite
     */
    public String getFullDisplayText() {
        String timeText = getDisplayText();
        if (timerData.getPredictedPhase() != null) {
            String phaseDisplay = TimeUtils.getPhaseDisplay(timerData.getPredictedPhase());
            return timeText + " - " + phaseDisplay;
        }
        return timeText;
    }
    
    /**
     * Formatage temps restant pour compatibilité UI
     */
    public String getFormattedTimeRemaining() {
        return getDisplayText();
    }
    
    /**
     * Nom dimension avec phase pour compatibilité UI
     */
    public String getDimensionNameWithPhase() {
        String phaseDisplay = TimeUtils.getPhaseDisplay(timerData.getPredictedPhase());
        return timerData.getDimensionName() + " - " + phaseDisplay;
    }
    
    // === DÉLÉGATION VERS TimerData ===
    
    public String getDimensionName() {
        return timerData.getDimensionName();
    }
    
    public long getSecondsRemaining() {
        return timerData.getSecondsRemaining();
    }
    
    public java.time.Duration getTimeRemaining() {
        return timerData.getTimeRemaining();
    }
    
    public boolean isExpired() {
        return timerData.isExpired();
    }
    
    public double getProgressPercentage() {
        return timerData.getProgressPercentage();
    }
    
    public TimeUtils.DayPhase getPredictedPhase() {
        return timerData.getPredictedPhase();
    }
    
    public java.time.Instant getExpiresAtUtc() {
        return timerData.getExpiresAtUtc();
    }
    
    public java.time.Duration getInitialDuration() {
        return timerData.getInitialDuration();
    }
    
    public java.time.Instant getCreatedAtUtc() {
        return timerData.getCreatedAtUtc();
    }
    
    public String getCreatedBy() {
        return timerData.getCreatedBy();
    }
    
    // === ACCÈS AU MODÈLE DE DONNÉES ===
    
    /**
     * Accès direct au TimerData (pour convertisseurs)
     */
    public TimerData getTimerData() {
        return timerData;
    }
    
    // === MÉTHODES DE COMPATIBILITÉ (dépréciées) ===
    
    @Deprecated
    public int getInitialMinutes() {
        return (int) timerData.getInitialDuration().toMinutes();
    }
    
    @Deprecated
    public int getInitialSeconds() {
        return (int) (timerData.getInitialDuration().getSeconds() % 60);
    }
    
    @Deprecated
    public java.time.LocalDateTime getSpawnTime() {
        // Conversion approximative pour compatibilité
        return java.time.LocalDateTime.now().plusSeconds(getSecondsRemaining());
    }
    
    @Deprecated
    public java.time.LocalDateTime getCreatedAt() {
        // Conversion approximative pour compatibilité
        return java.time.LocalDateTime.now();
    }
    
    @Deprecated
    public int getMinutesRemaining() {
        return (int) (getSecondsRemaining() / 60);
    }
    
    // === COMPARAISON ET HASH ===
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DimensionTimer other = (DimensionTimer) obj;
        return timerData.equals(other.timerData);
    }
    
    @Override
    public int hashCode() {
        return timerData.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("DimensionTimer{dimension='%s', remaining='%s', phase='%s'}",
                getDimensionName(), getDisplayText(), getPredictedPhase());
    }
    
    /**
     * Informations de debug détaillées
     */
    public String getDebugInfo() {
        return "DimensionTimer{" + timerData.getDebugInfo() + "}";
    }
}