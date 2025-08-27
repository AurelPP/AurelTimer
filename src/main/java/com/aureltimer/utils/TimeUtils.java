package com.aureltimer.utils;

import net.minecraft.client.MinecraftClient;

/**
 * Utilitaires pour gérer les phases du jour Minecraft et prédire les spawns légendaires
 */
public class TimeUtils {
    
    // Phases du jour en ticks Minecraft (0-24000)
    // 0 tick = 06:00, 6000 ticks = 12:00 (midi), 18000 ticks = 00:00 (minuit)
    // Formule: tick = (heure - 6) * 1000, ajustée pour le cycle 24h
    public enum DayPhase {
        DAWN("Dawn", "Aube", 23000, 24000, "05:00-05:59"),           // 05:00-05:59
        MORNING("Morning", "Matin", 0, 5000, "06:00-10:59"),         // 06:00-10:59  
        NOON("Noon", "Midi", 5000, 7000, "11:00-12:59"),             // 11:00-12:59
        AFTERNOON("Afternoon", "Après-midi", 7000, 12000, "13:00-17:59"), // 13:00-17:59
        DUSK("Dusk", "Crépuscule", 12000, 13000, "18:00-18:59"),     // 18:00-18:59
        NIGHT("Night", "Nuit", 13000, 23000, "19:00-04:59"),         // 19:00-04:59
        MIDNIGHT("Midnight", "Minuit", 17000, 19000, "23:00-00:59"), // 23:00-00:59
        DAY("Day", "Jour", 0, 12000, "06:00-17:59");                 // 06:00-17:59 (général)
        
        private final String englishName;
        private final String frenchName;
        private final long startTick;
        private final long endTick;
        private final String timeRange;
        
        DayPhase(String englishName, String frenchName, long startTick, long endTick, String timeRange) {
            this.englishName = englishName;
            this.frenchName = frenchName;
            this.startTick = startTick;
            this.endTick = endTick;
            this.timeRange = timeRange;
        }
        
        public String getEnglishName() { return englishName; }
        public String getFrenchName() { return frenchName; }
        public String getTimeRange() { return timeRange; }
        public long getStartTick() { return startTick; }
        public long getEndTick() { return endTick; }
        
        /**
         * Vérifie si un tick donné correspond à cette phase
         */
        public boolean isInPhase(long tick) {
            // Normaliser le tick (0-23999)
            tick = tick % 24000;
            
            // Gestion spéciale des phases qui traversent minuit
            switch (this) {
                case DAWN:
                    // DAWN: 05:00-05:59 (23000-24000 ticks)
                    return tick >= 23000 && tick < 24000;
                    
                case MIDNIGHT:
                    // MIDNIGHT: 23:00-00:59 (17000-19000 ticks, traverse minuit)
                    return tick >= 17000 && tick < 19000;
                    
                case NIGHT:
                    // NIGHT: 19:00-04:59 (13000-23000 ticks, exclut MIDNIGHT et DAWN)
                    if (tick >= 13000 && tick < 17000) return true; // 19:00-22:59
                    if (tick >= 19000 && tick < 23000) return true; // 01:00-04:59
                    return false;
                    
                default:
                    // Phases normales qui ne traversent pas minuit
                    return tick >= startTick && tick < endTick;
            }
        }
    }
    
    /**
     * Récupère l'heure actuelle du monde Minecraft en ticks
     */
    public static long getCurrentWorldTime() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                return client.world.getTimeOfDay() % 24000;
            }
        } catch (Exception e) {
            // Fallback si erreur
        }
        return 6000; // Midi par défaut
    }
    
    /**
     * Détermine la phase du jour pour un tick donné
     * Ordre de priorité : phases spécifiques avant phases générales
     */
    public static DayPhase getPhaseForTick(long tick) {
        // Phases spécifiques d'abord (ordre de priorité)
        if (DayPhase.DAWN.isInPhase(tick)) return DayPhase.DAWN;
        if (DayPhase.MIDNIGHT.isInPhase(tick)) return DayPhase.MIDNIGHT;
        if (DayPhase.DUSK.isInPhase(tick)) return DayPhase.DUSK;
        if (DayPhase.NOON.isInPhase(tick)) return DayPhase.NOON;
        if (DayPhase.MORNING.isInPhase(tick)) return DayPhase.MORNING;
        if (DayPhase.AFTERNOON.isInPhase(tick)) return DayPhase.AFTERNOON;
        
        // Phases générales
        if (DayPhase.NIGHT.isInPhase(tick)) return DayPhase.NIGHT;
        if (DayPhase.DAY.isInPhase(tick)) return DayPhase.DAY;
        
        return DayPhase.MORNING; // Fallback
    }
    
    /**
     * Prédit la phase du jour quand le timer sera terminé
     */
    public static DayPhase predictSpawnPhase(int minutes, int seconds) {
        long currentTime = getCurrentWorldTime();
        
        // Convertir le timer réel en ticks Minecraft
        // Cycle Minecraft standard: 20 minutes réelles = 24000 ticks = 1 jour Minecraft
        // Donc: 1 minute réelle = 1200 ticks Minecraft
        long totalTimerSeconds = minutes * 60L + seconds;
        long timerTicks = totalTimerSeconds * 20L; // 20 ticks/seconde Minecraft
        
        // MAIS: Le timer compte en temps RÉEL, pas en temps Minecraft
        // Il faut convertir: temps réel → temps Minecraft
        // 1 seconde réelle = 20 ticks Minecraft (dans un serveur normal)
        long futureTime = (currentTime + timerTicks) % 24000;
        
        return getPhaseForTick(futureTime);
    }
    
    /**
     * Formate la phase avec son nom et sa plage horaire
     */
    public static String formatPhase(DayPhase phase) {
        return phase.getFrenchName() + " (" + phase.getTimeRange() + ")";
    }
    
    /**
     * Convertit les ticks en heure lisible (format 24h)
     * Dans Minecraft: 0 tick = 06:00, 6000 ticks = 12:00, 18000 ticks = 00:00
     */
    public static String ticksToTimeString(long ticks) {
        ticks = ticks % 24000;
        
        // Convertir les ticks en heures Minecraft
        // 0 tick = 06:00, donc on ajoute 6 heures à la base
        long totalMinutes = (ticks * 60) / 1000; // Ticks vers minutes Minecraft
        long hours = (6 + totalMinutes / 60) % 24; // Ajouter 6h de base et gérer le cycle 24h
        long minutes = totalMinutes % 60;
        
        return String.format("%02d:%02d", hours, minutes);
    }
}
