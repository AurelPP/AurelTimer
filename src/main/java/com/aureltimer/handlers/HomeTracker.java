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
            lastHomeName = dimensionName.trim();
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
