package com.aureltimer.utils;

import com.aureltimer.config.ModConfig;

/**
 * Utilitaire pour les couleurs de phase du jour
 */
public class PhaseColorUtils {
    
    /**
     * Obtient la couleur Minecraft pour une phase du jour
     * @param phase La phase du jour
     * @return Le code couleur Minecraft (ex: "&6" pour jaune)
     */
    public static String getPhaseColor(TimeUtils.DayPhase phase) {
        if (!ModConfig.getInstance().isPhaseColorsEnabled()) {
            return "&f"; // Blanc par défaut si désactivé
        }
        
        switch (phase) {
            case DAWN:
                return "&6"; // Jaune pour l'aube
            case MORNING:
                return "&e"; // Jaune clair pour le matin
            case NOON:
                return "&7"; // Gris clair pour midi
            case AFTERNOON:
                return "&b"; // Bleu clair pour l'après-midi
            case DAY:
                return "&a"; // Vert pour le jour
            case DUSK:
                return "&5"; // Violet pour le crépuscule
            case NIGHT:
                return "&9"; // Bleu foncé pour la nuit
            case MIDNIGHT:
                return "&1"; // Bleu très foncé pour minuit
            default:
                return "&f"; // Blanc par défaut
        }
    }
    
    /**
     * Applique la couleur de phase à un texte
     * @param text Le texte à colorer
     * @param phase La phase du jour
     * @return Le texte avec les codes couleur
     */
    public static String colorizeText(String text, TimeUtils.DayPhase phase) {
        return getPhaseColor(phase) + text;
    }
    
    /**
     * Obtient le nom de la phase avec sa couleur
     * @param phase La phase du jour
     * @return Le nom coloré de la phase
     */
    public static String getColoredPhaseName(TimeUtils.DayPhase phase) {
        String color = getPhaseColor(phase);
        String phaseName = getPhaseDisplayName(phase);
        return color + phaseName;
    }
    
    /**
     * Obtient le nom d'affichage de la phase
     * @param phase La phase du jour
     * @return Le nom d'affichage
     */
    private static String getPhaseDisplayName(TimeUtils.DayPhase phase) {
        switch (phase) {
            case DAWN:
                return "Aube";
            case MORNING:
                return "Matin";
            case NOON:
                return "Midi";
            case AFTERNOON:
                return "Après-midi";
            case DAY:
                return "Jour";
            case DUSK:
                return "Crépuscule";
            case NIGHT:
                return "Nuit";
            case MIDNIGHT:
                return "Minuit";
            default:
                return "Inconnu";
        }
    }
}
