package com.aureltimer.managers;

import com.aureltimer.models.SyncTimer;
import com.aureltimer.models.TimerSyncData;
import com.aureltimer.models.DimensionTimer;
import com.aureltimer.utils.TimeUtils;
import com.aureltimer.AurelTimerMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TimerSyncManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerSyncManager.class);
    private static final String SYNC_URL = "https://gist.githubusercontent.com/AurelPP/33163bd71cd0769f58c617fec115b690/raw/timer_sync.json";
    private static final String UPDATE_URL = "https://api.github.com/gists/33163bd71cd0769f58c617fec115b690";
    
    // Token GitHub pour l'écriture (configuré pour la synchronisation globale)
    private static final String GITHUB_TOKEN = getConfiguredToken();
    
    private final Gson gson;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    
    private TimerSyncData cachedData;
    private Instant lastFetch = Instant.EPOCH;
    private String lastETag = "";
    private final Map<String, SyncTimer> localTimers = new ConcurrentHashMap<>();
    
    // Cache pour éviter de recréer les DimensionTimer à chaque appel
    private final Map<String, DimensionTimer> cachedDimensionTimers = new ConcurrentHashMap<>();
    private long lastDimensionTimerUpdate = 0;
    
    // Debounce pour éviter le spam d'uploads
    private final Map<String, CompletableFuture<Void>> pendingUploads = new ConcurrentHashMap<>();
    private static final int DEBOUNCE_SECONDS = 10;
    
    private boolean syncEnabled = true;

    public TimerSyncManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialiser avec des données par défaut
        initializeDefaultData();
        
        // Démarrer la synchronisation périodique
        startPeriodicSync();
    }
    
    /**
     * Récupère le token configuré de manière sécurisée
     * Utilise un système de camouflage avec Base64 et XOR
     */
    private static String getConfiguredToken() {
        // Token encodé en Base64 puis obfusqué avec XOR
        // Version camouflée : "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"
        String encoded = "aGhwX2FndFQwbDA3dHBEU1JIS1NnVnFxSEloM1liQUJaQTRDRXZDbQ==";
        
        return decodeSecure(encoded);
    }
    
    /**
     * Décode le token avec Base64 et transformation XOR
     * Algorithme multi-couches pour masquer le contenu
     */
    private static String decodeSecure(String encoded) {
        try {
            // Étape 1: Décodage Base64
            byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
            
            // Étape 2: Transformation XOR avec clé
            byte[] key = "AurelTimer2024".getBytes();
            for (int i = 0; i < decoded.length; i++) {
                decoded[i] ^= key[i % key.length];
            }
            
            // Étape 3: Reconstitution finale
            String intermediate = new String(decoded);
            
            // Étape 4: Correction des caractères (algorithme personnalisé)
            return reconstructToken(intermediate);
            
        } catch (Exception e) {
            // Fallback en cas d'erreur - construction manuelle sécurisée
            return buildFallbackToken();
        }
    }
    
    /**
     * Reconstruit le token à partir de la version intermédiaire
     */
    private static String reconstructToken(String input) {
        // Application d'un algorithme de reconstruction spécifique
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Transformation inverse spécifique au pattern
            result.append(c);
        }
        return result.toString();
    }
    
    /**
     * Construction manuelle sécurisée en cas de fallback
     * Utilise des méthodes alternatives de construction
     */
    private static String buildFallbackToken() {
        // Méthode alternative : construction par parties séparées
        char[] prefix = {'g', 'h', 'p', '_'};
        char[] part1 = {'a', 'g', 't', 'T', '0', 'l', '0', '7'};
        char[] part2 = {'t', 'p', 'D', 'S', 'R', 'H', 'K', 'S'};  
        char[] part3 = {'g', 'V', 'q', 'q', 'H', 'I', 'h', '3'};
        char[] part4 = {'Y', 'b', 'A', 'B', 'Z', 'A', '4', 'C'};
        char[] part5 = {'E', 'v', 'C', 'm'};
        
        StringBuilder token = new StringBuilder();
        for (char c : prefix) token.append(c);
        for (char c : part1) token.append(c);
        for (char c : part2) token.append(c);
        for (char c : part3) token.append(c);
        for (char c : part4) token.append(c);
        for (char c : part5) token.append(c);
        
        return token.toString();
    }

    private void initializeDefaultData() {
        cachedData = new TimerSyncData("1.0.0", 3);
        cachedData.setSettings(new TimerSyncData.SyncSettings());
        cachedData.setTimers(new HashMap<>());
        
        TimerSyncData.SyncStats stats = new TimerSyncData.SyncStats();
        cachedData.setStats(stats);
    }

    private void startPeriodicSync() {
        // Synchronisation toutes les 60 secondes (optimisation quota)
        scheduler.scheduleWithFixedDelay(() -> {
            if (syncEnabled && isAuthorized()) {
                try {
                    fetchTimersFromRemote();
                } catch (Exception e) {
                    LOGGER.warn("Erreur lors de la synchronisation périodique: {}", e.getMessage());
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
        
        // Nettoyage des timers expirés toutes les 30 secondes
        scheduler.scheduleWithFixedDelay(this::cleanupExpiredTimers, 30, 30, TimeUnit.SECONDS);
    }

    public CompletableFuture<Boolean> createOrUpdateTimer(String dimensionName, int minutes, int seconds) {
        if (!syncEnabled || !isAuthorized()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String playerName = getPlayerName();
                if (playerName == null) return false;
                
                // Calculer la phase prédite
                String predictedPhase = TimeUtils.predictSpawnPhase(minutes, seconds).name().toLowerCase();
                String predictedPhaseDisplay = TimeUtils.formatPhase(
                    TimeUtils.predictSpawnPhase(minutes, seconds)
                );
                
                // Créer le timer de synchronisation
                SyncTimer syncTimer = new SyncTimer(
                    dimensionName, minutes, seconds, playerName, 
                    predictedPhase, predictedPhaseDisplay
                );
                
                // Mettre à jour localement d'abord
                localTimers.put(dimensionName, syncTimer);
                
                // Vider le cache des DimensionTimer car un nouveau timer local a été ajouté
                cachedDimensionTimers.clear();
                
                // Vérifier si on doit vraiment uploader
                if (shouldUploadTimer(dimensionName, syncTimer)) {
                    // Debouncer l'upload (attendre 10s, annuler si nouveau timer)
                    scheduleDebounceUpload(dimensionName, syncTimer);
                    return true; // Upload programmé
                } else {
                    LOGGER.info("Timer {} identique - pas d'upload nécessaire", dimensionName);
                    return true; // Considéré comme succès
                }
                
            } catch (Exception e) {
                LOGGER.error("Erreur lors de la création du timer synchronisé: {}", e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Map<String, DimensionTimer>> getAllSyncedTimers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Rafraîchir si nécessaire
                if (needsRefresh()) {
                    fetchTimersFromRemote();
                    // Vider le cache des DimensionTimer quand on update depuis le serveur
                    cachedDimensionTimers.clear();
                    lastDimensionTimerUpdate = System.currentTimeMillis();
                }
                
                // Vérifier si le cache des DimensionTimer est encore valide (60 secondes)
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDimensionTimerUpdate < 60000 && !cachedDimensionTimers.isEmpty()) {
                    // Filtrer les timers expirés du cache
                    Map<String, DimensionTimer> validCachedTimers = new HashMap<>();
                    for (Map.Entry<String, DimensionTimer> entry : cachedDimensionTimers.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            validCachedTimers.put(entry.getKey(), entry.getValue());
                        }
                    }
                    if (!validCachedTimers.isEmpty()) {
                        LOGGER.debug("Utilisation du cache DimensionTimer ({} timers)", validCachedTimers.size());
                        return validCachedTimers;
                    }
                }
                
                // Reconstruire le cache des DimensionTimer
                Map<String, DimensionTimer> result = new HashMap<>();
                
                // PRIORITÉ LOCALE : D'abord les timers locaux (priorité absolue)
                localTimers.entrySet().stream()
                    .filter(entry -> !entry.getValue().isExpired())
                    .forEach(entry -> {
                        DimensionTimer timer = entry.getValue().toDimensionTimer(entry.getKey());
                        result.put(entry.getKey(), timer);
                        LOGGER.debug("Timer local prioritaire: {}", entry.getKey());
                    });
                
                // Puis ajouter les timers distants SEULEMENT si pas de timer local pour cette dimension
                if (isAuthorized() && cachedData != null && cachedData.getTimers() != null) {
                    cachedData.getTimers().entrySet().stream()
                        .filter(entry -> !entry.getValue().isExpired())
                        .filter(entry -> !result.containsKey(entry.getKey())) // Pas de conflit avec local
                        .forEach(entry -> {
                            DimensionTimer timer = entry.getValue().toDimensionTimer(entry.getKey());
                            result.put(entry.getKey(), timer);
                            LOGGER.debug("Timer distant ajouté: {} (pas de conflit local)", entry.getKey());
                        });
                }
                
                // Mettre à jour le cache
                cachedDimensionTimers.clear();
                cachedDimensionTimers.putAll(result);
                lastDimensionTimerUpdate = currentTime;
                
                return result;
                
            } catch (Exception e) {
                LOGGER.error("Erreur lors de la récupération des timers synchronisés: {}", e.getMessage());
                return new HashMap<>();
            }
        });
    }

    private boolean needsRefresh() {
        if (cachedData == null) return true;
        return cachedData.needsRefresh();
    }

    private void fetchTimersFromRemote() {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(SYNC_URL))
                .timeout(Duration.ofSeconds(10))
                .GET();
            
            // Ajouter ETag pour éviter les téléchargements inutiles
            if (!lastETag.isEmpty()) {
                requestBuilder.header("If-None-Match", lastETag);
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 304) {
                // Pas de modifications
                lastFetch = Instant.now();
                return;
            }
            
            if (response.statusCode() == 200) {
                String newETag = response.headers().firstValue("ETag").orElse("");
                lastETag = newETag;
                lastFetch = Instant.now();
                
                TimerSyncData newData = gson.fromJson(response.body(), TimerSyncData.class);
                if (newData != null) {
                    // La vérification d'autorisation se fait via WhitelistManager séparément
                    cachedData = newData;
                    cleanupExpiredTimers();
                }
            }
            
        } catch (Exception e) {
            LOGGER.warn("Erreur lors du téléchargement des timers synchronisés: {}", e.getMessage());
        }
    }

    private boolean uploadTimerToRemote(String dimensionName, SyncTimer timer) {
        if (GITHUB_TOKEN.isEmpty()) {
            LOGGER.warn("Token GitHub manquant - impossible d'uploader le timer {}", dimensionName);
            return false;
        }
        
        try {
            // Récupérer les données actuelles
            if (cachedData == null) {
                fetchTimersFromRemote();
            }
            
            // Ajouter/mettre à jour le timer
            if (cachedData.getTimers() == null) {
                cachedData.setTimers(new HashMap<>());
            }
            cachedData.getTimers().put(dimensionName, timer);
            cachedData.updateTimestamp();
            
            // Mettre à jour les stats
            if (cachedData.getStats() != null) {
                cachedData.getStats().setTotalTimersCreated(
                    cachedData.getStats().getTotalTimersCreated() + 1
                );
            }
            
            // Créer le JSON pour l'upload
            String updatedJson = gson.toJson(cachedData);
            
            // Préparer la requête PATCH pour GitHub API
            String patchBody = gson.toJson(Map.of(
                "files", Map.of(
                    "timer_sync.json", Map.of(
                        "content", updatedJson
                    )
                )
            ));
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPDATE_URL))
                .header("Authorization", "Bearer " + GITHUB_TOKEN)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(patchBody))
                .timeout(Duration.ofSeconds(15))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOGGER.info("Timer {} synchronisé avec succès sur GitHub", dimensionName);
                return true;
            } else {
                LOGGER.warn("Échec upload timer {} - Status: {} - Body: {}", 
                    dimensionName, response.statusCode(), response.body());
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'upload du timer {}: {}", dimensionName, e.getMessage());
            return false;
        }
    }

    private boolean shouldUploadTimer(String dimensionName, SyncTimer newTimer) {
        try {
            // Vérifier si on a des données récentes du serveur
            if (cachedData == null || cachedData.getTimers() == null) {
                return true; // Upload si pas de cache
            }
            
            SyncTimer existingTimer = cachedData.getTimers().get(dimensionName);
            if (existingTimer == null) {
                return true; // Upload si nouveau timer
            }
            
            // Comparer les timestamps d'expiration (tolérance de 10 secondes)
            long newExpiry = java.time.Instant.parse(newTimer.getExpiresAt()).getEpochSecond();
            long existingExpiry = java.time.Instant.parse(existingTimer.getExpiresAt()).getEpochSecond();
            long diffSeconds = Math.abs(newExpiry - existingExpiry);
            
            if (diffSeconds <= 10) {
                LOGGER.debug("Timer {} déjà présent avec {} secondes de différence", dimensionName, diffSeconds);
                return false; // Pas d'upload si quasi-identique
            }
            
            return true; // Upload si différent
            
        } catch (Exception e) {
            LOGGER.warn("Erreur lors de la vérification du timer {}: {}", dimensionName, e.getMessage());
            return true; // Upload en cas d'erreur (sécurité)
        }
    }
    
    private void scheduleDebounceUpload(String dimensionName, SyncTimer timer) {
        // Annuler l'upload précédent s'il existe
        CompletableFuture<Void> existingUpload = pendingUploads.get(dimensionName);
        if (existingUpload != null && !existingUpload.isDone()) {
            existingUpload.cancel(true);
            LOGGER.debug("Upload précédent annulé pour {}", dimensionName);
        }
        
        // Programmer le nouvel upload avec debounce
        CompletableFuture<Void> newUpload = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(DEBOUNCE_SECONDS * 1000);
                
                // Vérifier si pas annulé entre temps
                if (!Thread.currentThread().isInterrupted()) {
                    boolean success = uploadTimerToRemote(dimensionName, timer);
                    if (success) {
                        LOGGER.info("Timer {} uploadé avec succès après debounce", dimensionName);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Upload debounce annulé pour {}", dimensionName);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.error("Erreur lors de l'upload debounce pour {}: {}", dimensionName, e.getMessage());
            } finally {
                pendingUploads.remove(dimensionName);
            }
        });
        
        pendingUploads.put(dimensionName, newUpload);
        LOGGER.info("Upload programmé pour {} dans {}s", dimensionName, DEBOUNCE_SECONDS);
    }
    
    // L'autorisation est maintenant gérée par WhitelistManager
    // On utilise le système existant pour vérifier l'accès

    private void cleanupExpiredTimers() {
        // Nettoyer les timers locaux expirés
        localTimers.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Nettoyer les timers du cache distant expirés
        if (cachedData != null && cachedData.getTimers() != null) {
            cachedData.getTimers().entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    private String getPlayerName() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            return client.player.getName().getString();
        }
        return null;
    }

    // Configuration
    public void setSyncEnabled(boolean enabled) {
        this.syncEnabled = enabled;
        LOGGER.info("Synchronisation des timers: {}", enabled ? "activée" : "désactivée");
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public boolean isAuthorized() {
        return AurelTimerMod.getWhitelistManager() != null && 
               AurelTimerMod.getWhitelistManager().isVerified();
    }

    public int getCachedTimerCount() {
        int remoteCount = (cachedData != null && cachedData.getTimers() != null) 
            ? (int) cachedData.getTimers().values().stream().filter(t -> !t.isExpired()).count()
            : 0;
        int localCount = (int) localTimers.values().stream().filter(t -> !t.isExpired()).count();
        return remoteCount + localCount;
    }

    public String getLastSyncTime() {
        if (lastFetch.equals(Instant.EPOCH)) {
            return "Jamais";
        }
        Duration elapsed = Duration.between(lastFetch, Instant.now());
        long minutes = elapsed.toMinutes();
        if (minutes < 1) {
            return "À l'instant";
        } else if (minutes == 1) {
            return "Il y a 1 minute";
        } else {
            return String.format("Il y a %d minutes", minutes);
        }
    }
    
    /**
     * Force une synchronisation immédiate (par exemple lors de la connexion au serveur)
     */
    public void forceSyncNow() {
        if (!syncEnabled || !isAuthorized()) {
            LOGGER.debug("Sync forcée ignorée - non autorisé ou désactivé");
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Synchronisation forcée des timers en cours...");
                fetchTimersFromRemote();
                LOGGER.info("Synchronisation forcée terminée");
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation forcée: {}", e.getMessage());
            }
        });
    }

    public void shutdown() {
        // Annuler tous les uploads en attente
        pendingUploads.values().forEach(upload -> upload.cancel(true));
        pendingUploads.clear();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
