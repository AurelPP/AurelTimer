package com.aureltimer.managers;

import com.aureltimer.models.DimensionTimer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime spawnTime = now.plusMinutes(minutes).plusSeconds(seconds);
        
        DimensionTimer timer = new DimensionTimer(dimensionName, minutes, seconds, spawnTime);
        dimensionTimers.put(dimensionName, timer);
        
        // Synchroniser avec les autres utilisateurs si activé
        if (syncManager.isSyncEnabled() && syncManager.isAuthorized()) {
            syncManager.createOrUpdateTimer(dimensionName, minutes, seconds)
                .thenAccept(success -> {
                    if (success) {
                        LOGGER.info("Timer {} synchronisé avec succès", dimensionName);
                    } else {
                        LOGGER.warn("Échec de la synchronisation du timer {}", dimensionName);
                    }
                });
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
    
    public Map<String, DimensionTimer> getAllTimers() {
        Map<String, DimensionTimer> allTimers = new HashMap<>(dimensionTimers);
        
        // Ajouter les timers synchronisés si la sync est activée (asynchrone pour éviter les blocages)
        if (syncManager.isSyncEnabled() && syncManager.isAuthorized()) {
            try {
                // Utiliser un timeout court pour éviter les blocages
                Map<String, DimensionTimer> syncedTimers = syncManager.getAllSyncedTimers()
                    .orTimeout(100, TimeUnit.MILLISECONDS) // Timeout de 100ms
                    .exceptionally(throwable -> {
                        // En cas d'erreur ou timeout, retourner map vide
                        return new HashMap<>();
                    })
                    .get();
                
                // Fusionner les timers (priorité aux locaux en cas de conflit)
                for (Map.Entry<String, DimensionTimer> entry : syncedTimers.entrySet()) {
                    if (!allTimers.containsKey(entry.getKey())) {
                        allTimers.put(entry.getKey(), entry.getValue());
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
        int localCount = dimensionTimers.size();
        int syncedCount = syncManager.getCachedTimerCount();
        return Math.max(localCount, syncedCount); // Éviter de compter les doublons
    }
    
    // Méthodes pour gérer la synchronisation
    public TimerSyncManager getSyncManager() {
        return syncManager;
    }
    
    public boolean isSyncEnabled() {
        return syncManager.isSyncEnabled();
    }
    
    public void setSyncEnabled(boolean enabled) {
        syncManager.setSyncEnabled(enabled);
    }
    
    public boolean isSyncAuthorized() {
        return syncManager.isAuthorized();
    }
    
    public String getLastSyncTime() {
        return syncManager.getLastSyncTime();
    }
    
    public void shutdown() {
        if (syncManager != null) {
            syncManager.shutdown();
        }
    }
}
