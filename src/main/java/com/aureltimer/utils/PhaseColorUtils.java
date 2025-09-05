package com.aureltimer.utils;

import com.aureltimer.config.ModConfig;
import net.minecraft.util.Formatting;

/**
 * Utilitaire pour les couleurs de phase du jour
 */
public class PhaseColorUtils {
    
    /**
     * Obtient la couleur Minecraft pour une phase du jour
     * @param phase La phase du jour
     * @return La couleur Minecraft Formatting
     */
    public static Formatting getPhaseColor(TimeUtils.DayPhase phase) {
        if (!ModConfig.getInstance().isPhaseColorsEnabled()) {
            return Formatting.WHITE; // Blanc par défaut si désactivé
        }
        
        switch (phase) {
            case DAWN:
                return Formatting.GOLD; // Jaune pour l'aube
            case MORNING:
                return Formatting.YELLOW; // Jaune clair pour le matin
            case NOON:
                return Formatting.GRAY; // Gris clair pour midi
            case AFTERNOON:
                return Formatting.AQUA; // Bleu clair pour l'après-midi
            case DAY:
                return Formatting.GREEN; // Vert pour le jour
            case DUSK:
                return Formatting.LIGHT_PURPLE; // Violet pour le crépuscule
            case NIGHT:
                return Formatting.BLUE; // Bleu foncé pour la nuit
            case MIDNIGHT:
                return Formatting.DARK_BLUE; // Bleu très foncé pour minuit
            default:
                return Formatting.WHITE; // Blanc par défaut
        }
    }
    
    /**
     * Obtient le nom d'affichage de la phase
     * @param phase La phase du jour
     * @return Le nom d'affichage
     */
    public static String getPhaseDisplayName(TimeUtils.DayPhase phase) {
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
