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
import java.nio.charset.StandardCharsets;
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
    private static final String SYNC_URL_BASE = "https://gist.githubusercontent.com/AurelPP/33163bd71cd0769f58c617fec115b690/raw/timer_sync.json";
    
    private String getSyncUrl() {
        // Ajouter timestamp par tranche de 5 minutes pour équilibrer cache vs fraîcheur
        long timestampBlock = (System.currentTimeMillis() / (5 * 60 * 1000)) * (5 * 60 * 1000);
        return SYNC_URL_BASE + "?t=" + timestampBlock;
    }
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
     * Retourne le token GitHub configuré pour la synchronisation globale
     * Cryptage sécurisé avec substitution par tableaux et XOR
     */
    private static String getConfiguredToken() {
        // Données encodées par substitution avancée
        int[] encoded = {
            12, 48, 50, 9, 30, 23, 23, 63, 116, 121, 
            49, 78, 62, 47, 117, 76, 89, 14, 114, 38, 
            68, 117, 67, 99, 119, 113, 112, 115, 32, 223,
            239, 223, 224, 130, 207, 197, 230, 141, 170, 191
        };
        
        String key = "AurelTimer2024Sync";
        StringBuilder result = new StringBuilder();
        
        try {
            for (int i = 0; i < encoded.length; i++) {
                // Décodage par XOR avec clé rotative
                int keyChar = key.charAt(i % key.length());
                int decoded = encoded[i] ^ keyChar ^ (42 + i * 3);
                result.append((char) decoded);
            }
            return result.toString();
        } catch (Exception e) {
            LOGGER.error("Erreur décodage token sécurisé", e);
            return "";
        }
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
        LOGGER.info("🔄 Tentative de création timer: {} ({}m {}s), sync={}, auth={}", 
            dimensionName, minutes, seconds, syncEnabled, isAuthorized());
        
        if (!syncEnabled || !isAuthorized()) {
            LOGGER.warn("❌ Timer non créé - sync={}, auth={}", syncEnabled, isAuthorized());
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String playerName = getPlayerName();
                if (playerName == null) return false;
                
                // Vérifier si un timer existant a déjà une phase prédite à préserver
                SyncTimer existingTimer = localTimers.get(dimensionName);
                String predictedPhase;
                String predictedPhaseDisplay;
                
                if (existingTimer != null) {
                    // Préserver la phase existante
                    predictedPhase = existingTimer.getPredictedPhase();
                    predictedPhaseDisplay = existingTimer.getPredictedPhaseDisplay();
                    LOGGER.info("🔄 Préservation phase existante pour {}: {}", dimensionName, predictedPhase);
                } else {
                    // Nouveau timer - calculer la phase
                    predictedPhase = TimeUtils.predictSpawnPhase(minutes, seconds).name().toLowerCase();
                    predictedPhaseDisplay = TimeUtils.formatPhase(
                        TimeUtils.predictSpawnPhase(minutes, seconds)
                    );
                    LOGGER.info("🆕 Nouvelle phase calculée pour {}: {}", dimensionName, predictedPhase);
                }
                
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
                
                // Vérifier si le cache des DimensionTimer est encore valide (60 secondes pour quota GitHub)
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
                
                // Note: Les vrais timers locaux sont gérés par TimerManager.getAllTimers()
                // Ici on ne gère que les timers distants depuis le Gist
                
                // Puis ajouter les timers distants SEULEMENT si pas de timer local pour cette dimension
                if (isAuthorized() && cachedData != null && cachedData.getTimers() != null) {
                    cachedData.getTimers().entrySet().stream()
                        .filter(entry -> !entry.getValue().isExpired())
                        .filter(entry -> !result.containsKey(entry.getKey())) // Pas de conflit avec local
                        .forEach(entry -> {
                            DimensionTimer timer = entry.getValue().toDimensionTimer(entry.getKey());
                            result.put(entry.getKey(), timer);
                            
                            // Programmer l'alerte pour ce timer distant
                            scheduleAlertForSyncedTimer(entry.getKey(), timer);
                            
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
        String syncUrl = getSyncUrl();
        LOGGER.info("📥 Tentative de téléchargement depuis: {}", syncUrl);
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(syncUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .GET();
            
            // Ajouter ETag pour éviter les téléchargements inutiles
            if (!lastETag.isEmpty()) {
                requestBuilder.header("If-None-Match", lastETag);
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 304) {
                // Pas de modifications selon l'ETag
                LOGGER.debug("📋 HTTP 304 - Pas de modifications depuis la dernière sync");
                lastFetch = Instant.now();
                return;
            }
            
            LOGGER.info("📡 Réponse HTTP: {} - Taille: {} bytes", 
                response.statusCode(), response.body().length());
            LOGGER.debug("📄 Contenu JSON brut (500 premiers chars): {}", response.body().substring(0, Math.min(500, response.body().length())));
            
            if (response.statusCode() == 200) {
                String newETag = response.headers().firstValue("ETag").orElse("");
                lastETag = newETag;
                lastFetch = Instant.now();
                
                TimerSyncData newData = gson.fromJson(response.body(), TimerSyncData.class);

                if (newData != null) {
                    // La vérification d'autorisation se fait via WhitelistManager séparément
                    cachedData = newData;
                    cleanupExpiredTimers();
                    
                    // Vider le cache des DimensionTimer pour forcer la reconversion
                    cachedDimensionTimers.clear();
                    lastDimensionTimerUpdate = System.currentTimeMillis();
                    
                    LOGGER.info("✅ Données synchronisées: {} timers", 
                        newData.getTimers() != null ? newData.getTimers().size() : 0);
                    
                    // Forcer le rafraîchissement du cache de l'interface pour afficher immédiatement
                    try {
                        com.aureltimer.AurelTimerMod.getTimerOverlay().refreshCache();
                        LOGGER.debug("🔄 Cache interface rafraîchi après sync");
                    } catch (Exception e) {
                        LOGGER.warn("Erreur rafraîchissement cache interface: {}", e.getMessage());
                    }
                } else {
                    LOGGER.debug("📋 JSON parsé mais newData == null");
                }
            } else {
                LOGGER.warn("❌ Échec du téléchargement - Status: {}", response.statusCode());
            }
            
        } catch (Exception e) {
            LOGGER.warn("Erreur lors du téléchargement des timers synchronisés: {}", e.getMessage());
        }
    }

    private boolean uploadTimerToRemote(String dimensionName, SyncTimer timer) {
        String token = getConfiguredToken();
        // Upload timer vers le Gist
        
        if (token.isEmpty()) {
            LOGGER.warn("❌ GitHub Token vide - impossible d'uploader le timer {}", dimensionName);
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
                .header("Authorization", "Bearer " + token)
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
    
    /**
     * Programme une alerte pour un timer synchronisé si nécessaire
     */
    private void scheduleAlertForSyncedTimer(String dimensionName, DimensionTimer timer) {
        try {
            long totalSecondsRemaining = timer.getSecondsRemaining();
            int totalSeconds = (int) totalSecondsRemaining;
            
            // Programmer l'alerte à 1 minute restante
            if (totalSeconds > 60) {
                int delaySeconds = totalSeconds - 60;
                
                // Utiliser le scheduler unifié pour éviter les doublons
                com.aureltimer.utils.AlertScheduler.scheduleUniqueAlert(dimensionName, delaySeconds);
                
                LOGGER.debug("Alerte programmée pour timer synchronisé {} dans {}s", dimensionName, delaySeconds);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la programmation d'alerte pour timer synchronisé: {}", e.getMessage());
        }
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
