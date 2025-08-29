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
    
    // Tracker pour éviter le spam d'alertes
    private static final Set<String> scheduledAlerts = ConcurrentHashMap.newKeySet();
    private static long lastCleanup = System.currentTimeMillis();
    
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
                // Gestion spéciale pour "Dimensions" (Nether/End)
                if ("Dimensions".equals(dimensionName)) {
                    dimensionName = getVanillaDimensionName();
                }
                
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
                        
                        // Programmer l'alerte à 1 minute
                        scheduleAlertCheck(dimensionName, timeString);
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
    
    private void scheduleAlertCheck(String dimensionName, String timeString) {
        try {
            // Nettoyage périodique (toutes les 5 minutes)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanup > 300000) { // 5 minutes
                scheduledAlerts.clear();
                lastCleanup = currentTime;
                LOGGER.debug("Nettoyage périodique des alertes programmées");
            }
            
            // Créer une clé unique pour cette alerte (dimension + temps approximatif)
            String alertKey = dimensionName + "_" + timeString;
            
            // Vérifier si une alerte est déjà programmée pour cette dimension/temps
            if (scheduledAlerts.contains(alertKey)) {
                LOGGER.debug("Alerte déjà programmée pour {}, ignorée", dimensionName);
                return;
            }
            
            // Extraire le temps total en secondes
            Matcher matcher = TIME_PATTERN.matcher(timeString);
            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                int totalSeconds = minutes * 60 + seconds;
                
                // Programmer l'alerte à 1 minute restante
                if (totalSeconds > 60) {
                    int delaySeconds = totalSeconds - 60;
                    
                    // Marquer cette alerte comme programmée
                    scheduledAlerts.add(alertKey);
                    
                    new Thread(() -> {
                        try {
                            Thread.sleep(delaySeconds * 1000L);
                            showSpawnAlert(dimensionName);
                            
                            // Nettoyer après l'alerte
                            scheduledAlerts.remove(alertKey);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            scheduledAlerts.remove(alertKey);
                        }
                    }).start();
                    
                    LOGGER.info("⏰ Alerte programmée pour {} dans {} secondes", dimensionName, delaySeconds);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la programmation de l'alerte: {}", e.getMessage());
        }
    }
    
    private void showSpawnAlert(String dimensionName) {
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
