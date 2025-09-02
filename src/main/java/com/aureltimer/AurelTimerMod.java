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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AurelTimerMod implements ClientModInitializer {

    public static final String MOD_ID = "aurel-timer";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openTimerKey;
    private static KeyBinding openConfigKey;
    private static TimerManager timerManager;
    private static TimerOverlay timerOverlay;
    private static WhitelistManager whitelistManager;
    
    // Délai de grâce pour éviter les arrêts prématurés (Velocity proxy)
    private static ScheduledExecutorService disconnectGraceExecutor;
    private static volatile boolean isDisconnectScheduled = false;
    private static volatile boolean isConnectionGracePeriod = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initialisation d'Aurel Timer Mod...");

        // Initialiser la configuration
        ModConfig.getInstance();

        // Initialiser l'executor pour le délai de grâce
        disconnectGraceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AurelTimer-DisconnectGrace");
            t.setDaemon(true);
            return t;
        });

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
            
            // Annuler toute déconnexion en cours (transition Velocity → serveur final)
            if (isDisconnectScheduled) {
                LOGGER.info("🔄 Reconnexion détectée - annulation de l'arrêt prévu");
                isDisconnectScheduled = false;
            }
            
            // Période de grâce après connexion (5 secondes)
            isConnectionGracePeriod = true;
            disconnectGraceExecutor.schedule(() -> {
                isConnectionGracePeriod = false;
                LOGGER.info("✅ Période de grâce après connexion terminée");
            }, 5, TimeUnit.SECONDS);
            
            if (timerManager != null && timerManager.isSyncEnabled()) {
                // La sync se fera automatiquement avec le nouveau système
                LOGGER.info("✅ Sync automatique activée pour la connexion serveur");
            }
        });

        // ✅ ARRÊT PROPRE avec délai de grâce pour Velocity
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Ignorer les déconnexions pendant la période de grâce après connexion
            if (isConnectionGracePeriod) {
                LOGGER.info("🔄 Déconnexion ignorée - période de grâce après connexion active");
                return;
            }
            
            LOGGER.info("🛑 Déconnexion détectée - délai de grâce de 30 secondes...");
            
            // Programmer l'arrêt avec délai de grâce
            isDisconnectScheduled = true;
            disconnectGraceExecutor.schedule(() -> {
                if (isDisconnectScheduled) {
                    LOGGER.info("🛑 Délai de grâce écoulé - arrêt propre des managers");
                    if (timerManager != null) {
                        timerManager.close();
                    }
                    isDisconnectScheduled = false;
                } else {
                    LOGGER.info("✅ Arrêt annulé - reconnexion détectée");
                }
            }, 30, TimeUnit.SECONDS);
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
    
    /**
     * Arrêt propre du mod (appelé lors de la fermeture du jeu)
     */
    public static void shutdown() {
        if (disconnectGraceExecutor != null && !disconnectGraceExecutor.isShutdown()) {
            disconnectGraceExecutor.shutdown();
            try {
                if (!disconnectGraceExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    disconnectGraceExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                disconnectGraceExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (timerManager != null) {
            timerManager.close();
        }
    }
}
