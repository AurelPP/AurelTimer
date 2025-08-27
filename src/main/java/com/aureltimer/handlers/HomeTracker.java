package com.aureltimer.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracker le nom de la dimension actuelle pour nommer les timers
 */
public class HomeTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("HomeTracker");
    private static String lastHomeName = null;

    /**
     * Définit le nom de la dimension actuelle
     */
    public static void setLastHome(String dimensionName) {
        if (dimensionName != null && !dimensionName.trim().isEmpty()) {
            String cleanName = dimensionName.trim();
            
            // Dimensions vanilla toujours autorisées
            if ("Nether".equals(cleanName) || "End".equals(cleanName) || "Overworld".equals(cleanName)) {
                lastHomeName = cleanName;
                LOGGER.info("🌍 DIMENSION ACTUELLE: '{}'", lastHomeName);
                return;
            }
            
            // Vérifier que ce n'est pas un timer qui a été confondu avec une dimension
            if (cleanName.contains("minutes") || cleanName.contains("secondes") || 
                cleanName.contains("et") || cleanName.matches("\\d+\\s+(minutes?|secondes?).*")) {
                LOGGER.error("❌ ERREUR: Tentative de définir un timer '{}' comme nom de dimension!", cleanName);
                return;
            }
            
            lastHomeName = cleanName;
            LOGGER.info("🌍 DIMENSION ACTUELLE: '{}'", lastHomeName);
        }
    }

    /**
     * Récupère le nom de la dimension actuelle
     */
    public static String getLastHome() {
        return lastHomeName;
    }

    /**
     * Vérifie si une dimension a été définie
     */
    public static boolean hasHome() {
        return lastHomeName != null && !lastHomeName.isEmpty();
    }
}
