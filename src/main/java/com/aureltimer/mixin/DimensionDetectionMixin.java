package com.aureltimer.mixin;

import com.aureltimer.AurelTimerMod;
import com.aureltimer.handlers.HomeTracker;
import com.aureltimer.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin pour détecter les dimensions et les timers de spawn légendaires
 */
@Mixin(ClientPlayNetworkHandler.class)
public class DimensionDetectionMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("DimensionDetectionMixin");
    private final HomeTracker homeTracker = new HomeTracker();
    
    // Supprimé : maintenant géré par AlertScheduler global
    
    // Pattern pour extraire le temps des messages de spawn légendaire
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s+minutes?\\s+et\\s+(\\d+)\\s+secondes?");
    
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        try {
            Text messageText = packet.content();
            String message = messageText.getString();
            
            // Vérifier si l'utilisateur est autorisé via la whitelist
            if (AurelTimerMod.getWhitelistManager() != null && !AurelTimerMod.getWhitelistManager().isVerified()) {
                // Si pas encore vérifié ou non autorisé, ignorer les messages
                return;
            }
            
            // Traitement normal des messages si autorisé
            processMessage(message);
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message: {}", e.getMessage());
        }
    }
    
    private void processMessage(String message) {
        // Détection des messages HUD pour la dimension - SEULEMENT les vrais messages HUD
        if (message.contains("PGHUD") || message.contains("&f裁")) {
            String dimensionName = extractDimensionFromHudMessage(message);
            if (dimensionName != null) {
                // ✅ GARDE "Dimensions" tel quel (pas de conversion vanilla)
                
                homeTracker.setLastHome(dimensionName);
            }
        }
        
        // Détection des messages de timer de spawn légendaire
        if (message.contains("Prochaine Tentative de Spawn:") || 
            message.contains("Prochaine tentative de spawn:")) {
            processTimerMessage(message);
        }
    }
    
    private String extractDimensionFromHudMessage(String hudMessage) {
        try {
            // Chercher le pattern "&f裁 &fNomDimension: {}"
            if (hudMessage.contains("&f裁")) {
                String[] parts = hudMessage.split("&f裁");
                if (parts.length > 1) {
                    String dimensionPart = parts[1].trim();
                    
                    // Nettoyer les codes de couleur et extraire le nom
                    String cleanDimension = dimensionPart.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
                    
                    if (cleanDimension.contains(":")) {
                        cleanDimension = cleanDimension.split(":")[0].trim();
                    }
                    if (!cleanDimension.isEmpty() && !cleanDimension.contains("minutes") && !cleanDimension.contains("secondes")) {
                        return cleanDimension;
                    }
                }
            }
            
            // Pas de fallback générique pour éviter de capturer les messages de chat
            
        } catch (Exception e) {
            LOGGER.warn("❌ Impossible d'extraire le nom de dimension du message HUD: {}", hudMessage);
        }
        
        LOGGER.warn("⚠️ Aucune dimension valide trouvée dans: '{}'", hudMessage);
        return null;
    }
    
    private String getVanillaDimensionName() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                String dimensionId = client.world.getRegistryKey().getValue().toString();
                if (dimensionId.contains("the_nether")) return "Nether";
                if (dimensionId.contains("the_end")) return "End";
                if (dimensionId.contains("overworld")) return "Overworld";
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la détection de la dimension vanilla: {}", e.getMessage());
        }
        return "Overworld"; // Fallback
    }
    
    private void processTimerMessage(String message) {
        try {
            String timeString = extractTimeFromMessage(message);
            if (timeString != null) {
                String dimensionName = homeTracker.getLastHome();
                
                if (dimensionName != null && !dimensionName.trim().isEmpty()) {
                    // Vérifier que la dimension n'est pas le timeString lui-même
                    if (!dimensionName.equals(timeString) && !dimensionName.contains("minutes") && !dimensionName.contains("secondes")) {
                        AurelTimerMod.getTimerManager().updateTimer(dimensionName, timeString);
                        LOGGER.info("⏰ Timer créé pour {}: {}", dimensionName, timeString);
                        
                        // Programmer l'alerte à 1 minute (système unifié)
                        try {
                            int minutes = Integer.parseInt(timeString.split(" ")[0]);
                            int seconds = Integer.parseInt(timeString.split(" ")[3]); // Index 3 pour "Y" dans "X minutes et Y secondes"
                            int totalSeconds = minutes * 60 + seconds;
                            
                            if (totalSeconds > 60) {
                                int delaySeconds = totalSeconds - 60;
                                com.aureltimer.utils.AlertScheduler.scheduleUniqueAlert(dimensionName, delaySeconds);
                            }
                        } catch (Exception e) {
                            LOGGER.error("Erreur parsing pour alerte: {}", e.getMessage());
                        }
                    } else {
                        LOGGER.error("❌ ERREUR: La dimension détectée '{}' semble être un timer au lieu d'un nom de dimension!", dimensionName);
                    }
                } else {
                    LOGGER.warn("⚠️ Aucune dimension détectée pour le timer: {}", timeString);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du timer: {}", e.getMessage());
        }
    }
    
    private String extractTimeFromMessage(String message) {
        Matcher matcher = TIME_PATTERN.matcher(message);
        if (matcher.find()) {
            String minutes = matcher.group(1);
            String seconds = matcher.group(2);
            return minutes + " minutes et " + seconds + " secondes";
        }
        return null;
    }
    
    private void showSpawnAlert(String dimensionName) {
        com.aureltimer.utils.AlertUtils.showSpawnAlert(dimensionName);
    }
}
