package com.aureltimer.managers;

import com.aureltimer.models.DimensionTimer;
import com.aureltimer.models.TimerData;
import com.aureltimer.utils.TimeAuthority;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TimerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerManager.class);
    private final Map<String, DimensionTimer> dimensionTimers = new ConcurrentHashMap<>();
    private final TimerSyncManager syncManager;
    
    public TimerManager() {
        this.syncManager = new TimerSyncManager();
    }
    
    public void updateTimer(String dimensionName, int minutes, int seconds) {
        // Vérifier si un timer existe déjà (local ou distant)
        Map<String, DimensionTimer> allExistingTimers = getAllTimers();
        DimensionTimer existingTimer = allExistingTimers.get(dimensionName);
        
        DimensionTimer timer;
        if (existingTimer != null && !existingTimer.isExpired()) {
            // Timer existant encore valide - comparer les heures d'expiration (plus robuste)
            
            // Calculer la nouvelle heure d'expiration candidat
            TimeAuthority timeAuth = TimeAuthority.getInstance();
            Instant newExpiresAt = timeAuth.now().plusSeconds(minutes * 60L + seconds);
            
            // Comparer avec l'heure d'expiration existante
            long deltaSeconds = Math.abs(java.time.Duration.between(
                existingTimer.getExpiresAtUtc(), newExpiresAt).getSeconds());
            
            // Si la différence est petite (< 30 secondes), c'est probablement le même événement
            if (deltaSeconds < 30) {
                LOGGER.info("Timer {} : même événement détecté - PRÉSERVATION TOTALE", dimensionName);
                LOGGER.info("Existant expire à: {}, Nouveau expirerait à: {} → Δ={}s → Garde existant", 
                    existingTimer.getExpiresAtUtc(), newExpiresAt, deltaSeconds);
                
                // ✅ GARDE LE TIMER EXISTANT TEL QUEL - Pas de recalcul !
                timer = existingTimer;
                
            } else {
                LOGGER.info("Timer {} : événement différent détecté - NOUVEAU TIMER", dimensionName);
                LOGGER.info("Existant expire à: {}, Nouveau expirerait à: {} → Δ={}s → Nouveau timer", 
                    existingTimer.getExpiresAtUtc(), newExpiresAt, deltaSeconds);
                // Événement complètement différent - créer nouveau timer
                timer = DimensionTimer.createFromMinutesSeconds(dimensionName, minutes, seconds, getCurrentUser());
            }
        } else {
            // Nouveau timer ou timer expiré
            LOGGER.info("Nouveau timer créé pour {}: {} minutes et {} secondes", dimensionName, minutes, seconds);
            timer = DimensionTimer.createFromMinutesSeconds(dimensionName, minutes, seconds, getCurrentUser());
        }
        
        dimensionTimers.put(dimensionName, timer);
        
        // Forcer le refresh immédiat du cache de l'overlay pour éviter le retard d'affichage
        try {
            com.aureltimer.gui.TimerOverlay overlay = com.aureltimer.AurelTimerMod.getTimerOverlay();
            if (overlay != null) {
                overlay.refreshCache();
            }
        } catch (Exception e) {
            // Ignorer silencieusement si pas trouvé
        }
        
        // Programmer l'alerte pour TOUS les timers (chat ET sync)
        try {
            int totalSeconds = minutes * 60 + seconds;
            if (totalSeconds > 60) {
                int delaySeconds = totalSeconds - 60;
                boolean alertScheduled = com.aureltimer.utils.AlertScheduler.scheduleUniqueAlert(dimensionName, delaySeconds);
                if (alertScheduled) {
                    LOGGER.info("🔔 Alerte programmée pour {} dans {}s", dimensionName, delaySeconds);
                } else {
                    LOGGER.debug("🔔 Alerte déjà programmée pour {}", dimensionName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la programmation d'alerte: {}", e.getMessage());
        }
        
        // Synchroniser avec les autres utilisateurs si activé
        LOGGER.info("🔍 Debug sync - syncEnabled: {}", syncManager.getSyncEnabled());
        if (syncManager.getSyncEnabled()) {
            LOGGER.info("✅ Déclenchement sync pour {}", dimensionName);
            
            // Créer TimerData depuis DimensionTimer pour la sync
            TimerData timerData = timer.getTimerData();
            syncManager.createOrUpdateTimer(dimensionName, timerData);
            
            LOGGER.debug("Timer {} programmé pour sync (upload différé)", dimensionName);
            
            // Forcer refresh du cache après création locale
            try {
                com.aureltimer.gui.TimerOverlay overlay = com.aureltimer.AurelTimerMod.getTimerOverlay();
                if (overlay != null) {
                    overlay.refreshCache();
                }
            } catch (Exception e) {
                // Ignorer silencieusement
            }
        } else {
            LOGGER.warn("❌ Sync non déclenchée - syncEnabled: {}", syncManager.getSyncEnabled());
        }
        
        LOGGER.info("Timer mis à jour pour {}: {} minutes et {} secondes", dimensionName, minutes, seconds);
    }
    
    public void updateTimer(String dimensionName, String timeString) {
        // Parse du format "X minutes et Y secondes"
        try {
            String[] parts = timeString.split(" et ");
            int minutes = 0;
            int seconds = 0;
            
            if (parts.length >= 1) {
                String minutesPart = parts[0].trim();
                if (minutesPart.contains("minute")) {
                    minutes = Integer.parseInt(minutesPart.replaceAll("[^0-9]", ""));
                }
            }
            
            if (parts.length >= 2) {
                String secondsPart = parts[1].trim();
                if (secondsPart.contains("seconde")) {
                    seconds = Integer.parseInt(secondsPart.replaceAll("[^0-9]", ""));
                }
            }
            
            updateTimer(dimensionName, minutes, seconds);
        } catch (Exception e) {
            LOGGER.error("Erreur lors du parsing du timer: {}", timeString, e);
        }
    }
    
    /**
     * Obtient l'utilisateur actuel pour la création de timers
     */
    private String getCurrentUser() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            return client.player.getGameProfile().getName();
        }
        return "local";
    }
    
    public Map<String, DimensionTimer> getAllTimers() {
        Map<String, DimensionTimer> allTimers = new HashMap<>(dimensionTimers);
        
        // Ajouter les timers synchronisés si la sync est activée
        if (syncManager.getSyncEnabled()) {
            try {
                Map<String, TimerData> syncedTimers = syncManager.getAllTimers();
                
                // Convertir TimerData vers DimensionTimer et fusionner
                for (Map.Entry<String, TimerData> entry : syncedTimers.entrySet()) {
                    if (!allTimers.containsKey(entry.getKey())) {
                        DimensionTimer dimTimer = new DimensionTimer(entry.getValue());
                        allTimers.put(entry.getKey(), dimTimer);
                    }
                }
            } catch (Exception e) {
                // Ignorer silencieusement pour ne pas affecter les performances
                LOGGER.debug("Timers synchronisés non disponibles: {}", e.getMessage());
            }
        }
        
        return allTimers;
    }
    
    public DimensionTimer getTimer(String dimensionName) {
        return dimensionTimers.get(dimensionName);
    }
    
    public void clearTimer(String dimensionName) {
        dimensionTimers.remove(dimensionName);
        LOGGER.info("Timer supprimé pour {}", dimensionName);
    }
    
    public void clearAllTimers() {
        dimensionTimers.clear();
        LOGGER.info("Tous les timers ont été supprimés");
    }
    
    public boolean hasTimer(String dimensionName) {
        return dimensionTimers.containsKey(dimensionName);
    }
    
    public int getActiveTimerCount() {
        return getAllTimers().size();
    }
    
    // Méthodes pour gérer la synchronisation
    public TimerSyncManager getSyncManager() {
        return syncManager;
    }
    
    public boolean isSyncEnabled() {
        return syncManager.getSyncEnabled();
    }
    
    public void setSyncEnabled(boolean enabled) {
        syncManager.setSyncEnabled(enabled);
    }
    
    /**
     * ✅ MÉTRIQUES DEBUG pour diagnostics
     */
    public String getDebugMetrics() {
        return syncManager.getDebugMetrics();
    }
    
    /**
     * ✅ FERMETURE PROPRE DU TIMER MANAGER
     */
    public void close() {
        if (syncManager != null) {
            syncManager.close();
        }
        LOGGER.info("🛑 TimerManager fermé proprement");
    }
    
    /**
     * @deprecated Utiliser close() à la place
     */
    @Deprecated
    public void shutdown() {
        close();
    }
}
