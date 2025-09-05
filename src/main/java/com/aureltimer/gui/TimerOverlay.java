package com.aureltimer.gui;

import com.aureltimer.managers.TimerManager;
import com.aureltimer.managers.WhitelistManager;
import com.aureltimer.models.DimensionTimer;
import com.aureltimer.config.ModConfig;
import com.aureltimer.utils.PhaseColorUtils;
import com.aureltimer.utils.TimeUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
    
    // Cache pour optimiser les performances et quota GitHub
    private Map<String, DimensionTimer> cachedTimers = null;
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION_MS = 30000; // 30 secondes de cache
    
    // Position et déplacement de l'interface
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    // Couleurs et style
    private static final int BORDER_COLOR = 0xFF4CAF50; // Vert
    private static final int TITLE_COLOR = 0xFFFFFFFF; // Blanc
    private static final int TEXT_COLOR = 0xFFCCCCCC; // Gris clair
    private static final int TIMER_COLOR = 0xFFFFFF00; // Jaune
    private static final int EXPIRED_COLOR = 0xFFFF4444; // Rouge
    private static final int ERROR_COLOR = 0xFFFF5555; // Rouge d'erreur
    
    /**
     * Obtient la couleur de fond avec transparence configurée
     */
    private int getBackgroundColor() {
        float transparency = ModConfig.getInstance().getNormalizedTransparency();
        int alpha = (int) (transparency * 0x88); // Base 0x88 (136) avec facteur de transparence
        return (alpha << 24) | 0x000000; // Noir avec transparence
    }
    
    /**
     * Obtient une couleur avec transparence appliquée
     */
    private int getColorWithTransparency(int baseColor) {
        float transparency = ModConfig.getInstance().getNormalizedTransparency();
        // S'assurer que l'alpha est dans la plage valide [0, 255]
        int alpha = Math.max(0, Math.min(255, (int) (transparency * 0xFF)));
        return (alpha << 24) | (baseColor & 0x00FFFFFF); // Couleur avec transparence
    }
    
    
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
        
        // Position et taille de l'interface (adaptative selon le nombre de timers)
        int overlayWidth = 300;
        ModConfig config = ModConfig.getInstance();
        int maxDisplayed = config.getMaxDisplayedTimers();
        int overlayHeight = 80 + (maxDisplayed * 25) + (maxDisplayed < 6 ? 20 : 0); // Hauteur adaptative
        
        // Utiliser position sauvegardée ou centrer par défaut
        int x, y;
        if (config.getOverlayX() == -1 || config.getOverlayY() == -1) {
            // Position par défaut (centrée)
            x = (screenWidth - overlayWidth) / 2;
            y = (screenHeight - overlayHeight) / 2;
        } else {
            // Position sauvegardée depuis la config
            x = Math.max(0, Math.min(config.getOverlayX(), screenWidth - overlayWidth));
            y = Math.max(0, Math.min(config.getOverlayY(), screenHeight - overlayHeight));
        }
        
        // Fond principal avec transparence configurée
        context.fill(x, y, x + overlayWidth, y + overlayHeight, getBackgroundColor());
        
        // Bordure (différente couleur si en train de déplacer)
        int borderColor = isDragging ? getColorWithTransparency(0xFFFFFFFF) : getColorWithTransparency(BORDER_COLOR); // Blanc si drag, vert sinon
        context.fill(x, y, x + overlayWidth, y + 2, borderColor); // Bordure supérieure
        context.fill(x, y, x + 2, y + overlayHeight, borderColor); // Bordure gauche
        context.fill(x + overlayWidth - 2, y, x + overlayWidth, y + overlayHeight, borderColor); // Bordure droite
        context.fill(x, y + overlayHeight - 2, x + overlayWidth, y + overlayHeight, borderColor); // Bordure inférieure
        
        // Vérifier si l'opacité est trop faible pour afficher le texte
        boolean shouldHideText = ModConfig.getInstance().getInterfaceTransparency() <= 1;
        
        if (!shouldHideText) {
            // Titre
            Text title = Text.literal("Timers Légendaires");
            int titleWidth = client.textRenderer.getWidth(title);
            context.drawText(client.textRenderer, title, x + (overlayWidth - titleWidth) / 2, y + 10, getColorWithTransparency(TITLE_COLOR), true);
            
            // Récupérer la vraie hotkey pour fermer l'interface
            String closeKey = getCloseKeyName();
            
            // Instructions
            Text instructions = Text.literal("Appuie sur " + closeKey + " pour fermer");
            int instructionsWidth = client.textRenderer.getWidth(instructions);
            context.drawText(client.textRenderer, instructions, x + (overlayWidth - instructionsWidth) / 2, y + 25, getColorWithTransparency(TEXT_COLOR), true);
        }
        
        // Séparateur
        context.fill(x + 20, y + 45, x + overlayWidth - 20, y + 47, getColorWithTransparency(BORDER_COLOR));
        
        // Liste des timers (seulement si l'opacité est suffisante)
        if (!shouldHideText) {
            renderTimers(context, x + 20, y + 60, overlayWidth - 40);
        }
    }
    
    private void renderTimers(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Utiliser le cache pour éviter les appels fréquents
        Map<String, DimensionTimer> timers = getCachedTimers();
        
        if (timers.isEmpty()) {
            Text noTimers = Text.literal("Aucun timer actif");
            context.drawText(client.textRenderer, noTimers, x + (width - client.textRenderer.getWidth(noTimers)) / 2, y, getColorWithTransparency(TEXT_COLOR), true);
            
            Text instruction = Text.literal("Utilise /legendaryspawn dans chaque dimension");
            context.drawText(client.textRenderer, instruction, x + (width - client.textRenderer.getWidth(instruction)) / 2, y + 20, getColorWithTransparency(TEXT_COLOR), true);
            return;
        }
        
        // Trier les timers par temps restant
        List<DimensionTimer> sortedTimers = timers.values().stream()
            .sorted((t1, t2) -> Long.compare(t1.getSecondsRemaining(), t2.getSecondsRemaining()))
            .collect(Collectors.toList());
        
        // Limiter le nombre de timers affichés selon la configuration
        ModConfig config = ModConfig.getInstance();
        int maxDisplayed = config.getMaxDisplayedTimers();
        List<DimensionTimer> limitedTimers = sortedTimers.stream()
            .limit(maxDisplayed)
            .collect(Collectors.toList());
        
        int currentY = y;
        for (DimensionTimer timer : limitedTimers) {
            renderTimer(context, timer, x, currentY, width);
            currentY += 25;
        }
        
        // Afficher un indicateur s'il y a plus de timers
        if (sortedTimers.size() > maxDisplayed) {
            int hiddenCount = sortedTimers.size() - maxDisplayed;
            Text moreText = Text.literal("... et " + hiddenCount + " autre(s)");
            context.drawText(client.textRenderer, moreText, x + (width - client.textRenderer.getWidth(moreText)) / 2, currentY, getColorWithTransparency(TEXT_COLOR), true);
        }
    }
    
    private void renderTimer(DrawContext context, DimensionTimer timer, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Nom de la dimension avec phase prédite colorée
        String phaseDisplay = TimeUtils.getPhaseDisplay(timer.getPredictedPhase());
        String dimensionName = timer.getDimensionName() + " - " + phaseDisplay;
        
        // Appliquer la couleur de phase au nom de dimension si activé
        if (ModConfig.getInstance().isPhaseColorsEnabled()) {
            // Utiliser Text.formatted() pour les couleurs de phase
            Text dimensionText = Text.literal(timer.getDimensionName() + " - ")
                .append(Text.literal(phaseDisplay).formatted(PhaseColorUtils.getPhaseColor(timer.getPredictedPhase())));
            context.drawText(client.textRenderer, dimensionText, x, y, getColorWithTransparency(TITLE_COLOR), true);
        } else {
            context.drawText(client.textRenderer, dimensionName, x, y, getColorWithTransparency(TITLE_COLOR), true);
        }
        
        // Timer avec couleur de phase
        String timeText = timer.getFormattedTimeRemaining();
        
        // Appliquer la couleur de phase au temps si activé
        if (ModConfig.getInstance().isPhaseColorsEnabled() && !timer.isExpired()) {
            // Utiliser Text.formatted() pour les couleurs de phase
            Text timerText = Text.literal(timeText).formatted(PhaseColorUtils.getPhaseColor(timer.getPredictedPhase()));
            int timerWidth = client.textRenderer.getWidth(timerText);
            context.drawText(client.textRenderer, timerText, x + width - timerWidth, y, getColorWithTransparency(TIMER_COLOR), true);
        } else {
            int timeColor = timer.isExpired() ? getColorWithTransparency(EXPIRED_COLOR) : getColorWithTransparency(TIMER_COLOR);
            int timerWidth = client.textRenderer.getWidth(timeText);
            context.drawText(client.textRenderer, timeText, x + width - timerWidth, y, timeColor, true);
        }
        
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
        
        // Timer : 100% (pleine) au début → 0% (vide) à la fin
        float progress = (float) remainingSeconds / totalSeconds;
        int progressWidth = (int) (width * progress);
        
        // Barre de fond (gris foncé, plus clair que le fond de l'interface)
        context.fill(x, y, x + width, y + barHeight, getColorWithTransparency(0x66888888));
        
        // Barre de progression
        int progressColor = progress > 0.5f ? 0xFF4CAF50 : progress > 0.2f ? 0xFFFF9800 : 0xFFFF4444;
        context.fill(x, y, x + progressWidth, y + barHeight, getColorWithTransparency(progressColor));
    }
    
    /**
     * Récupère les timers avec cache pour optimiser les performances
     */
    private Map<String, DimensionTimer> getCachedTimers() {
        long currentTime = System.currentTimeMillis();
        
        // Vérifier si le cache est encore valide
        if (cachedTimers != null && (currentTime - lastCacheUpdate) < CACHE_DURATION_MS) {
            return cachedTimers;
        }
        
        // Mettre à jour le cache
        try {
            cachedTimers = timerManager.getAllTimers();
            lastCacheUpdate = currentTime;
        } catch (Exception e) {
            LOGGER.warn("Erreur lors de la récupération des timers: {}", e.getMessage());
            // Retourner le cache précédent en cas d'erreur
            if (cachedTimers == null) {
                cachedTimers = new HashMap<>();
            }
        }
        
        return cachedTimers;
    }
    
    /**
     * Force le rafraîchissement du cache
     */
    public void refreshCache() {
        cachedTimers = null;
        lastCacheUpdate = 0;
    }
    
    /**
     * Gestion des événements de souris pour le drag & drop
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        // Autoriser le drag & drop SEULEMENT si le chat est ouvert
        if (client.currentScreen == null || !isChatScreen(client.currentScreen)) {
            return false;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int overlayWidth = 300;
        int overlayHeight = 200;
        
        // Calculer la position actuelle depuis la config
        ModConfig config = ModConfig.getInstance();
        int x = config.getOverlayX() == -1 ? (screenWidth - overlayWidth) / 2 : config.getOverlayX();
        int y = config.getOverlayY() == -1 ? (screenHeight - overlayHeight) / 2 : config.getOverlayY();
        
        // Vérifier si le clic est dans la zone de titre (pour drag)
        if (button == 0 && mouseX >= x && mouseX <= x + overlayWidth && 
            mouseY >= y && mouseY <= y + 40) { // Zone de titre étendue
            
            isDragging = true;
            dragOffsetX = (int) (mouseX - x);
            dragOffsetY = (int) (mouseY - y);
            return true;
        }
        
        return false;
    }
    
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (isDragging && button == 0) {
            isDragging = false;
            
            // Sauvegarder la position finale à la fin du drag
            ModConfig config = ModConfig.getInstance();
            config.setOverlayPosition(config.getOverlayX(), config.getOverlayY());
            
            return true;
        }
        return false;
    }
    
    public boolean handleMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isDragging) return false;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        // Arrêter le dragging si le chat se ferme
        if (client.currentScreen == null || !isChatScreen(client.currentScreen)) {
            isDragging = false;
            return false;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int overlayWidth = 300;
        int overlayHeight = 200;
        
        // Calculer nouvelle position
        int newX = (int) (mouseX - dragOffsetX);
        int newY = (int) (mouseY - dragOffsetY);
        
        // Contraindre à l'écran et sauvegarder temporairement (sans sauvegarder le fichier)
        int newOverlayX = Math.max(0, Math.min(newX, screenWidth - overlayWidth));
        int newOverlayY = Math.max(0, Math.min(newY, screenHeight - overlayHeight));
        
        // Mise à jour temporaire sans sauvegarde fichier (trop fréquent)
        ModConfig config = ModConfig.getInstance();
        config.setOverlayXTemporary(newOverlayX);
        config.setOverlayYTemporary(newOverlayY);
        
        return true;
    }
    
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
     * Vérifie si l'écran ouvert est le chat
     */
    private boolean isChatScreen(Screen screen) {
        if (screen == null) return false;
        
        String className = screen.getClass().getSimpleName();
        String fullClassName = screen.getClass().getName();
        
        // Vérifier plusieurs variantes possibles du chat
        return className.equals("ChatScreen") || 
               className.contains("Chat") ||
               fullClassName.contains("chat") ||
               fullClassName.contains("Chat") ||
               // Nom obfusqué spécifique du chat en Fabric 1.21.1
               className.equals("class_408");
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
