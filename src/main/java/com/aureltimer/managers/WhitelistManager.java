package com.aureltimer.managers;

import com.aureltimer.models.WhitelistData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire de la whitelist dynamique
 */
public class WhitelistManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("WhitelistManager");
    private static final String WHITELIST_URL = "https://gist.githubusercontent.com/AurelPP/33163bd71cd0769f58c617fec115b690/raw/whitelist.json";
    private static final int TIMEOUT_MS = 10000; // 10 secondes
    
    private final Gson gson;
    private final ScheduledExecutorService executor;
    
    private WhitelistData currentWhitelist;
    private long lastUpdateTime = 0;
    private boolean isVerified = false;
    private boolean hasChecked = false;
    
    public WhitelistManager() {
        this.gson = new GsonBuilder().create();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WhitelistManager");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Initialise le gestionnaire et démarre les vérifications
     */
    public void initialize() {
        LOGGER.info("🔐 Initialisation du système de whitelist dynamique...");
        
        // Première vérification immédiate
        updateWhitelist();
        
        // Planifier les mises à jour périodiques (toutes les 30 minutes par défaut)
        executor.scheduleAtFixedRate(this::updateWhitelist, 30, 30, TimeUnit.MINUTES);
    }
    
    /**
     * Met à jour la whitelist depuis le serveur distant
     */
    public void updateWhitelist() {
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("📥 Téléchargement de la whitelist...");
                
                URL url = new URL(WHITELIST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", "AurelTimer/1.3.0");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    String jsonResponse = readInputStream(connection.getInputStream());
                    WhitelistData newWhitelist = gson.fromJson(jsonResponse, WhitelistData.class);
                    
                    if (newWhitelist != null) {
                        currentWhitelist = newWhitelist;
                        lastUpdateTime = System.currentTimeMillis();
                        
                        // Vérifier le joueur actuel
                        checkCurrentPlayer();
                        
                        LOGGER.info("✅ Whitelist mise à jour: {}", currentWhitelist);
                        
                        // Planifier la prochaine mise à jour selon le TTL
                        if (currentWhitelist.ttl_minutes > 0) {
                            rescheduleUpdate(currentWhitelist.ttl_minutes);
                        }
                    } else {
                        LOGGER.error("❌ Impossible de parser la whitelist JSON");
                        handleWhitelistFailure();
                    }
                } else {
                    LOGGER.error("❌ Erreur HTTP lors du téléchargement: {}", responseCode);
                    handleWhitelistFailure();
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                LOGGER.error("❌ Erreur lors de la mise à jour de la whitelist: {}", e.getMessage());
                handleWhitelistFailure();
            }
        }, executor);
    }
    
    /**
     * Vérifie le joueur actuel contre la whitelist
     */
    private void checkCurrentPlayer() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSession() != null) {
                String username = client.getSession().getUsername();
                String uuid = client.getSession().getUuidOrNull() != null ? 
                    client.getSession().getUuidOrNull().toString() : null;
                
                boolean authorized = isPlayerAuthorized(username, uuid);
                isVerified = authorized;
                hasChecked = true;
                
                if (authorized) {
                    LOGGER.info("✅ Joueur autorisé: {}", username);
                } else {
                    LOGGER.warn("⛔ Joueur non autorisé: {}", username);
                }
            }
        } catch (Exception e) {
            LOGGER.error("❌ Erreur lors de la vérification du joueur: {}", e.getMessage());
        }
    }
    
    /**
     * Gère les échecs de téléchargement de whitelist
     */
    private void handleWhitelistFailure() {
        hasChecked = true;
        
        if (currentWhitelist == null) {
            // Pas de whitelist du tout - refuser l'accès
            isVerified = false;
            LOGGER.error("🚫 Aucune whitelist disponible - Accès refusé");
        } else {
            // Garder la dernière whitelist valide
            LOGGER.warn("⚠️ Utilisation de la dernière whitelist connue");
        }
    }
    
    /**
     * Replanifie la prochaine mise à jour selon le TTL
     */
    private void rescheduleUpdate(int ttlMinutes) {
        // Annuler les tâches précédentes et en créer une nouvelle
        executor.schedule(this::updateWhitelist, ttlMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * Vérifie si un joueur est autorisé
     */
    public boolean isPlayerAuthorized(String username, String uuid) {
        if (currentWhitelist == null) {
            return false;
        }
        return currentWhitelist.isPlayerAuthorized(username, uuid);
    }
    
    /**
     * Vérifie si le joueur actuel est vérifié
     */
    public boolean isVerified() {
        return isVerified;
    }
    
    /**
     * Vérifie si une vérification a déjà été effectuée
     */
    public boolean hasChecked() {
        return hasChecked;
    }
    
    /**
     * Récupère le message d'erreur personnalisé
     */
    public String getUnauthorizedMessage() {
        if (currentWhitelist != null) {
            return currentWhitelist.getUnauthorizedMessage();
        }
        return "⛔ Accès refusé";
    }
    
    /**
     * Récupère les informations de la whitelist actuelle
     */
    public WhitelistData getCurrentWhitelist() {
        return currentWhitelist;
    }
    
    /**
     * Ferme le gestionnaire et arrête les tâches
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    /**
     * Lit un InputStream et retourne le contenu en String
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
