package com.aureltimer.handlers;

import com.aureltimer.managers.TimerManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gère l'interception des messages de chat pour détecter les timers de spawn légendaires
 */
@Mixin(net.minecraft.client.gui.hud.ChatHud.class)
public class ChatHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("ChatHandler");

    // Patterns pour détecter les réponses de /legendaryspawn
    private static final Pattern TIMER_PATTERN_1 = Pattern.compile("(\\d+)\\s*minutes?\\s*et\\s*(\\d+)\\s*secondes?");
    private static final Pattern TIMER_PATTERN_2 = Pattern.compile("(\\d+):(\\d+)");

    private static TimerManager timerManager;

    public static void setTimerManager(TimerManager manager) {
        timerManager = manager;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onChatMessage(Text message, CallbackInfo ci) {
        try {
            String messageText = message.getString();
            
            // Détecter les réponses de /legendaryspawn
            if (messageText.toLowerCase().contains("legendary") ||
                messageText.toLowerCase().contains("spawn") ||
                messageText.toLowerCase().contains("tentative") ||
                messageText.toLowerCase().contains("prochaine") ||
                messageText.toLowerCase().contains("prochain") ||
                messageText.toLowerCase().contains("temps") ||
                messageText.toLowerCase().contains("minutes") ||
                messageText.toLowerCase().contains("secondes")) {

                processTimerMessage(messageText);
            }

        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message: {}", e.getMessage());
        }
    }

    private void processTimerMessage(String message) {
        try {
            // Utiliser le nom de la dimension détectée au lieu du home
            String dimensionName = com.aureltimer.handlers.HomeTracker.getLastHome();
            if (dimensionName == null || dimensionName.isEmpty()) {
                dimensionName = "Overworld";
            }

            String timeRemaining = extractTimeFromMessage(message);
            if (timeRemaining != null) {
                String[] timeParts = timeRemaining.split(":");
                if (timeParts.length == 2) {
                    int minutes = Integer.parseInt(timeParts[0]);
                    int seconds = Integer.parseInt(timeParts[1]);

                    // Créer le nom du timer avec le nom de la dimension
                    String timerName = dimensionName;
                    timerManager.updateTimer(timerName, minutes, seconds);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Erreur lors du traitement du message de timer: {}", e.getMessage());
        }
    }

    private String extractTimeFromMessage(String message) {
        try {
            // Essayer le premier pattern (X minutes et Y secondes)
            Matcher matcher = TIMER_PATTERN_1.matcher(message);
            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                return minutes + ":" + seconds;
            }
            
            // Essayer le deuxième pattern (X:Y)
            matcher = TIMER_PATTERN_2.matcher(message);
            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                return minutes + ":" + seconds;
            }

        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'extraction du temps: {}", e.getMessage());
        }

        return null;
    }
}
