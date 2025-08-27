package com.aureltimer.handlers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Vérifie l'appartenance à la guilde Aether
 */
public class GuildVerifier {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("GuildVerifier");
    private static final String GUILD_NAME = "Aether";
    private static final String DATA_LOADED_MESSAGE = "Your data has been loaded successfully";
    
    private boolean isVerified = false;
    private boolean hasChecked = false;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static GuildVerifier instance;
    
    private GuildVerifier() {}
    
    public static GuildVerifier getInstance() {
        if (instance == null) {
            instance = new GuildVerifier();
        }
        return instance;
    }
    
    /**
     * Vérifie si le joueur appartient à la guilde Aether
     */
    public boolean isVerified() {
        return isVerified;
    }
    
    /**
     * Vérifie si la vérification a déjà été effectuée
     */
    public boolean hasChecked() {
        return hasChecked;
    }
    
    /**
     * Traite le message de chargement des données
     */
    public void processDataLoadedMessage() {
        if (hasChecked) {
            return; // Déjà vérifié
        }
        
        LOGGER.info("🔍 Message de chargement des données détecté, lancement de la vérification de guilde...");
        
        // Exécuter /t info après un court délai pour laisser le temps au serveur
        scheduler.schedule(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                // Exécuter la commande /t info
                client.player.networkHandler.sendCommand("t info");
                LOGGER.info("📋 Commande /t info exécutée pour vérifier la guilde");
            }
        }, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Traite la réponse de /t info pour extraire le nom de guilde
     */
    public void processTownInfoResponse(String message) {
        if (hasChecked) {
            return; // Déjà vérifié
        }
        
        LOGGER.info("📨 Réponse /t info reçue: {}", message);
        
        // Chercher le nom de guilde dans la réponse
        if (message.contains("Overview of " + GUILD_NAME + ":")) {
            isVerified = true;
            hasChecked = true;
            
            // Message de confirmation dans le chat
            sendChatMessage("§a§lAurel Timer : §aAppartenance à la guilde " + GUILD_NAME + " vérifiée");
            LOGGER.info("✅ Appartenance à la guilde {} vérifiée", GUILD_NAME);
            
        } else if (message.contains("Overview of ")) {
            // Autre guilde détectée
            isVerified = false;
            hasChecked = true;
            
            // Message de refus dans le chat
            sendChatMessage("§c§lAurel Timer : §cVous n'appartenez pas à la guilde " + GUILD_NAME + ", accès refusé");
            LOGGER.warn("❌ Appartenance à la guilde {} refusée", GUILD_NAME);
            
        } else if (message.contains("You are not in a town") || 
                   message.contains("Vous n'êtes pas dans une ville")) {
            // Pas de guilde (messages en anglais et français)
            isVerified = false;
            hasChecked = true;
            
            // Message de refus dans le chat
            sendChatMessage("§c§lAurel Timer : §cVous n'appartenez pas à la guilde " + GUILD_NAME + ", accès refusé");
            LOGGER.warn("❌ Aucune guilde détectée, accès refusé");
        }
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
    
    /**
     * Force la vérification (pour les tests)
     */
    public void forceVerification() {
        isVerified = true;
        hasChecked = true;
        LOGGER.info("🔓 Vérification forcée pour les tests");
    }
    
    /**
     * Réinitialise la vérification
     */
    public void reset() {
        isVerified = false;
        hasChecked = false;
        LOGGER.info("🔄 Vérification réinitialisée");
    }
}
