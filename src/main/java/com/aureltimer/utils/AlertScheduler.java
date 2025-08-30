package com.aureltimer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;

/**
 * Gestionnaire global des alertes pour éviter les doublons
 */
public class AlertScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertScheduler.class);
    private static final Set<String> scheduledAlerts = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static long lastCleanup = System.currentTimeMillis();
    
    /**
     * Programme une alerte unique pour un timer
     * @param dimensionName nom de la dimension
     * @param delaySeconds délai en secondes avant l'alerte
     * @return true si alerte programmée, false si déjà existante
     */
    public static boolean scheduleUniqueAlert(String dimensionName, int delaySeconds) {
        try {
            // Nettoyage périodique (toutes les 5 minutes)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanup > 300000) { // 5 minutes
                scheduledAlerts.clear();
                lastCleanup = currentTime;
                LOGGER.debug("Nettoyage périodique des alertes programmées");
            }
            
            // Créer une clé unique basée sur la dimension ET le moment approximatif
            // Arrondir à la minute près pour éviter les doublons
            long alertTime = (currentTime + delaySeconds * 1000L) / 60000L; // Minutes depuis epoch
            String alertKey = dimensionName + "_" + alertTime;
            
            // Vérifier si une alerte est déjà programmée
            if (scheduledAlerts.contains(alertKey)) {
                LOGGER.debug("Alerte déjà programmée pour {} à t+{}s, ignorée", dimensionName, delaySeconds);
                return false;
            }
            
            // Marquer cette alerte comme programmée
            scheduledAlerts.add(alertKey);
            
            // Programmer l'alerte
            scheduler.schedule(() -> {
                try {
                    AlertUtils.showSpawnAlert(dimensionName);
                    LOGGER.info("Alerte unique exécutée pour: {}", dimensionName);
                    
                    // Nettoyer après exécution
                    scheduledAlerts.remove(alertKey);
                } catch (Exception e) {
                    LOGGER.error("Erreur lors de l'exécution de l'alerte: {}", e.getMessage());
                    scheduledAlerts.remove(alertKey);
                }
            }, delaySeconds, TimeUnit.SECONDS);
            
            LOGGER.debug("Alerte unique programmée pour {} dans {}s", dimensionName, delaySeconds);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la programmation d'alerte: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Arrête le scheduler
     */
    public static void shutdown() {
        scheduler.shutdown();
        scheduledAlerts.clear();
    }
}

