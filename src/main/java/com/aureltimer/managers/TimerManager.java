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
        LocalDateTime newSpawnTime = now.plusMinutes(minutes).plusSeconds(seconds);
        
        // Vérifier si un timer existe déjà (local ou distant)
        Map<String, DimensionTimer> allExistingTimers = getAllTimers();
        DimensionTimer existingTimer = allExistingTimers.get(dimensionName);
        
        DimensionTimer timer;
        if (existingTimer != null && !existingTimer.isExpired()) {
            // Timer existant encore valide - vérifier si c'est une mise à jour mineure
            long existingRemainingSeconds = existingTimer.getSecondsRemaining();
            long newTotalSeconds = minutes * 60L + seconds;
            
            // Si la différence est petite (< 30 secondes), probablement une mise à jour du même timer
            if (Math.abs(existingRemainingSeconds - newTotalSeconds) < 30) {
                LOGGER.info("Timer {} existe déjà avec progression similaire - préservation COMPLÈTE", dimensionName);
                LOGGER.info("Existant: {}min {}s restants, Nouveau: {}min {}s → Préservation", 
                    existingRemainingSeconds / 60, existingRemainingSeconds % 60, minutes, seconds);
                
                // Créer un nouveau timer qui préserve TOUT : durée originale, progression, phase ET createdAt
                timer = new DimensionTimer(
                    dimensionName, 
                    existingTimer.getInitialMinutes(),  // ✅ Préserver durée originale
                    existingTimer.getInitialSeconds(),  // ✅ Préserver durée originale  
                    existingTimer.getSpawnTime(), 
                    existingTimer.getPredictedPhase(),
                    existingTimer.getCreatedAt()
                );
            } else {
                LOGGER.info("Timer {} mis à jour avec nouveau temps - reset progression", dimensionName);
                // Nouveau timer complètement différent
                timer = new DimensionTimer(dimensionName, minutes, seconds, newSpawnTime);
            }
        } else {
            // Nouveau timer ou timer expiré
            LOGGER.info("Nouveau timer créé pour {}: {} minutes et {} secondes", dimensionName, minutes, seconds);
            timer = new DimensionTimer(dimensionName, minutes, seconds, newSpawnTime);
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
        
        // Synchroniser avec les autres utilisateurs si activé
        if (syncManager.isSyncEnabled() && syncManager.isAuthorized()) {
            syncManager.createOrUpdateTimer(dimensionName, minutes, seconds)
                .thenAccept(success -> {
                    if (success) {
                        LOGGER.info("Timer {} synchronisé avec succès", dimensionName);
                        // Forcer refresh du cache après sync réussie
                        try {
                            com.aureltimer.gui.TimerOverlay overlay = com.aureltimer.AurelTimerMod.getTimerOverlay();
                            if (overlay != null) {
                                overlay.refreshCache();
                            }
                        } catch (Exception e) {
                            // Ignorer silencieusement
                        }
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
                // Utiliser un timeout raisonnable pour éviter les blocages tout en permettant la sync
                Map<String, DimensionTimer> syncedTimers = syncManager.getAllSyncedTimers()
                    .orTimeout(2000, TimeUnit.MILLISECONDS) // Timeout de 2 secondes
                    .exceptionally(throwable -> {
                        // En cas d'erreur ou timeout, retourner map vide
                        LOGGER.warn("Timeout/erreur récupération timers synchronisés: {}", throwable.getMessage());
                        return new HashMap<>();
                    })
                    .get();
                
                // Fusionner les timers (priorité absolue aux locaux en cas de conflit)
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
