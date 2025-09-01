package com.aureltimer;

import com.aureltimer.config.ModConfig;
import com.aureltimer.gui.ConfigScreen;
import com.aureltimer.gui.TimerOverlay;
import com.aureltimer.handlers.ChatHandler;
import com.aureltimer.managers.TimerManager;
import com.aureltimer.managers.WhitelistManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AurelTimerMod implements ClientModInitializer {

    public static final String MOD_ID = "aurel-timer";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openTimerKey;
    private static KeyBinding openConfigKey;
    private static TimerManager timerManager;
    private static TimerOverlay timerOverlay;
    private static WhitelistManager whitelistManager;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initialisation d'Aurel Timer Mod...");

        // Initialiser la configuration
        ModConfig.getInstance();

        // Initialiser le système de whitelist
        whitelistManager = new WhitelistManager();
        whitelistManager.initialize();

        // Initialiser les composants
        timerManager = new TimerManager();
        timerOverlay = new TimerOverlay(timerManager, whitelistManager);
        
        // Appliquer la configuration de synchronisation
        timerManager.setSyncEnabled(ModConfig.getInstance().shouldSyncTimers());

        // Configurer le ChatHandler avec le TimerManager
        ChatHandler.setTimerManager(timerManager);

        // Enregistrer le raccourci clavier pour l'interface des timers
        openTimerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.aureltimer.toggleoverlay",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "category.aureltimer.general"
        ));

        // Enregistrer le raccourci clavier pour la configuration
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.aureltimer.openconfig",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.aureltimer.general"
        ));

        // Enregistrer le gestionnaire de touches
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openTimerKey.wasPressed()) {
                timerOverlay.toggleVisibility();
            }
            
            if (openConfigKey.wasPressed()) {
                openConfigScreen(client);
            }
        });

        // Synchronisation lors de la connexion au serveur
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.info("Connexion au serveur détectée - synchronisation des timers...");
            if (timerManager != null && timerManager.isSyncEnabled()) {
                // La sync se fera automatiquement avec le nouveau système
                LOGGER.info("✅ Sync automatique activée pour la connexion serveur");
            }
        });

        // ✅ ARRÊT PROPRE lors de la déconnexion
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("🛑 Déconnexion détectée - arrêt propre des managers");
            if (timerManager != null) {
                timerManager.close();
            }
        });

        LOGGER.info("Aurel Timer Mod initialisé avec succès !");
        LOGGER.info("🌍 Le mod détectera automatiquement le nom de la dimension depuis les messages HUD pour nommer les timers");
        LOGGER.info("⚙️ Appuyez sur K pour ouvrir la configuration");
        LOGGER.info("📊 Appuyez sur L pour ouvrir l'interface des timers");
    }

    private void openConfigScreen(MinecraftClient client) {
        if (client.currentScreen == null) {
            client.setScreen(new ConfigScreen(null));
        }
    }

    public static TimerOverlay getTimerOverlay() {
        return timerOverlay;
    }

    public static TimerManager getTimerManager() {
        return timerManager;
    }

    public static WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }
}
