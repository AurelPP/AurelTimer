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
            // Vérifier que ce n'est pas un timer qui a été confondu avec une dimension
            if (dimensionName.contains("minutes") || dimensionName.contains("secondes") || 
                dimensionName.contains("et") || dimensionName.matches("\\d+\\s+(minutes?|secondes?).*")) {
                LOGGER.error("❌ ERREUR: Tentative de définir un timer '{}' comme nom de dimension!", dimensionName);
                return;
            }
            
            lastHomeName = dimensionName.trim();
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
