package com.aureltimer.gui;

import com.aureltimer.managers.TimerManager;
import com.aureltimer.managers.WhitelistManager;
import com.aureltimer.models.DimensionTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interface d'affichage des timers de spawn légendaires
 */
public class TimerOverlay {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("TimerOverlay");
    private final TimerManager timerManager;
    private final WhitelistManager whitelistManager;
    private boolean isVisible = false;
    
    // Couleurs et style
    private static final int BACKGROUND_COLOR = 0x88000000; // Noir semi-transparent
    private static final int BORDER_COLOR = 0xFF4CAF50; // Vert
    private static final int TITLE_COLOR = 0xFFFFFFFF; // Blanc
    private static final int TEXT_COLOR = 0xFFCCCCCC; // Gris clair
    private static final int TIMER_COLOR = 0xFFFFFF00; // Jaune
    private static final int EXPIRED_COLOR = 0xFFFF4444; // Rouge
    private static final int ERROR_COLOR = 0xFFFF5555; // Rouge d'erreur
    
    public TimerOverlay(TimerManager timerManager, WhitelistManager whitelistManager) {
        this.timerManager = timerManager;
        this.whitelistManager = whitelistManager;
    }
    
    public void toggleVisibility() {
        // Vérifier l'autorisation avant d'afficher
        if (!whitelistManager.isVerified()) {
            if (!whitelistManager.hasChecked()) {
                // En cours de vérification
                sendChatMessage("§e§lAurel Timer : §eVérification en cours...");
                return;
            } else {
                // Vérification échouée
                sendChatMessage("§c§lAurel Timer : §c" + whitelistManager.getUnauthorizedMessage());
                return;
            }
        }
        
        isVisible = !isVisible;
        LOGGER.info("Interface des timers: {}", isVisible ? "visible" : "masquée");
    }
    
    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public void render(DrawContext context) {
        if (!isVisible) return;
        
        // Vérifier l'autorisation avant de rendre
        if (!whitelistManager.isVerified()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Position et taille de l'interface (centrée)
        int overlayWidth = 300;
        int overlayHeight = 200;
        int x = (screenWidth - overlayWidth) / 2;
        int y = (screenHeight - overlayHeight) / 2;
        
        // Fond principal
        context.fill(x, y, x + overlayWidth, y + overlayHeight, BACKGROUND_COLOR);
        
        // Bordure
        context.fill(x, y, x + overlayWidth, y + 2, BORDER_COLOR); // Bordure supérieure
        context.fill(x, y, x + 2, y + overlayHeight, BORDER_COLOR); // Bordure gauche
        context.fill(x + overlayWidth - 2, y, x + overlayWidth, y + overlayHeight, BORDER_COLOR); // Bordure droite
        context.fill(x, y + overlayHeight - 2, x + overlayWidth, y + overlayHeight, BORDER_COLOR); // Bordure inférieure
        
        // Titre
        Text title = Text.literal("⏰ Timers Légendaires");
        int titleWidth = client.textRenderer.getWidth(title);
        context.drawText(client.textRenderer, title, x + (overlayWidth - titleWidth) / 2, y + 10, TITLE_COLOR, true);
        
        // Récupérer la vraie hotkey pour fermer l'interface
        String closeKey = getCloseKeyName();
        
        // Instructions
        Text instructions = Text.literal("Appuie sur " + closeKey + " pour fermer");
        int instructionsWidth = client.textRenderer.getWidth(instructions);
        context.drawText(client.textRenderer, instructions, x + (overlayWidth - instructionsWidth) / 2, y + 25, TEXT_COLOR, true);
        
        // Séparateur
        context.fill(x + 20, y + 45, x + overlayWidth - 20, y + 47, BORDER_COLOR);
        
        // Liste des timers
        renderTimers(context, x + 20, y + 60, overlayWidth - 40);
    }
    
    private void renderTimers(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        Map<String, DimensionTimer> timers = timerManager.getAllTimers();
        
        if (timers.isEmpty()) {
            Text noTimers = Text.literal("Aucun timer actif");
            context.drawText(client.textRenderer, noTimers, x + (width - client.textRenderer.getWidth(noTimers)) / 2, y, TEXT_COLOR, true);
            
            Text instruction = Text.literal("Utilise /legendaryspawn dans chaque dimension");
            context.drawText(client.textRenderer, instruction, x + (width - client.textRenderer.getWidth(instruction)) / 2, y + 20, TEXT_COLOR, true);
            return;
        }
        
        // Trier les timers par temps restant
        List<DimensionTimer> sortedTimers = timers.values().stream()
            .sorted((t1, t2) -> Long.compare(t1.getSecondsRemaining(), t2.getSecondsRemaining()))
            .collect(Collectors.toList());
        
        int currentY = y;
        for (DimensionTimer timer : sortedTimers) {
            if (currentY > y + 120) break; // Limiter le nombre de timers affichés
            
            renderTimer(context, timer, x, currentY, width);
            currentY += 25;
        }
    }
    
    private void renderTimer(DrawContext context, DimensionTimer timer, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Nom de la dimension avec phase prédite
        Text dimensionText = Text.literal("🌍 " + timer.getDimensionNameWithPhase());
        context.drawText(client.textRenderer, dimensionText, x, y, TITLE_COLOR, true);
        
        // Timer
        String timeText = timer.getFormattedTimeRemaining();
        int timeColor = timer.isExpired() ? EXPIRED_COLOR : TIMER_COLOR;
        Text timerText = Text.literal(timeText);
        
        int timerWidth = client.textRenderer.getWidth(timerText);
        context.drawText(client.textRenderer, timerText, x + width - timerWidth, y, timeColor, true);
        
        // Barre de progression
        if (!timer.isExpired()) {
            renderProgressBar(context, x, y + 15, width, timer);
        }
    }
    
    private void renderProgressBar(DrawContext context, int x, int y, int width, DimensionTimer timer) {
        int barHeight = 3;
        int totalSeconds = timer.getInitialMinutes() * 60 + timer.getInitialSeconds();
        int remainingSeconds = (int) timer.getSecondsRemaining();
        
        if (totalSeconds <= 0) return;
        
        float progress = (float) remainingSeconds / totalSeconds;
        int progressWidth = (int) (width * progress);
        
        // Barre de fond
        context.fill(x, y, x + width, y + barHeight, 0x44FFFFFF);
        
        // Barre de progression
        int progressColor = progress > 0.5f ? 0xFF4CAF50 : progress > 0.2f ? 0xFFFF9800 : 0xFFFF4444;
        context.fill(x, y, x + progressWidth, y + barHeight, progressColor);
    }
    
    /**
     * Récupère le nom de la touche configurée pour fermer l'interface
     */
    private String getCloseKeyName() {
        try {
            // Chercher la hotkey "toggleoverlay" dans les options Minecraft
            for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
                if (keyBinding.getTranslationKey().equals("key.aureltimer.toggleoverlay")) {
                    return keyBinding.getBoundKeyLocalizedText().getString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la récupération de la hotkey: {}", e.getMessage());
        }
        
        // Fallback si on ne trouve pas la hotkey
        return "L";
    }
    
    /**
     * Envoie un message dans le chat
     */
    private void sendChatMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message));
        }
    }
}
