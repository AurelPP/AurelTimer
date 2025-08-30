package com.aureltimer.utils;

import com.aureltimer.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitaires pour les alertes de spawn
 */
public class AlertUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertUtils.class);
    
    /**
     * Affiche une alerte de spawn avec son et message selon la configuration
     */
    public static void showSpawnAlert(String dimensionName) {
        try {
            ModConfig config = ModConfig.getInstance();
            
            // Jouer le son si activé
            if (config.shouldPlaySound()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    client.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
            
            // Afficher le message si activé
            if (config.shouldShowAlert() && config.shouldShowInChat()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    String alertMessage = "§6§l⚠ SPAWN DE LÉGENDAIRE DANS 1 MINUTE EN " + dimensionName + " ⚠";
                    client.player.sendMessage(Text.literal(alertMessage));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'affichage de l'alerte: {}", e.getMessage());
        }
    }
}

