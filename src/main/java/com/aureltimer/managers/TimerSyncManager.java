package com.aureltimer.managers;

import com.aureltimer.models.WorkerTimerSyncData;
import com.aureltimer.models.TimerData;
import com.aureltimer.sync.CloudflareClient;
import com.aureltimer.utils.Actor;
import com.aureltimer.utils.ShortId;
import com.aureltimer.utils.TimeAuthority;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

/**
 * 🚀 NOUVEAU TIMER SYNC MANAGER - CLOUDFLARE
 * 
 * Manager principal de synchronisation utilisant :
 * - CloudflareClient pour GET/POST (Worker read + write proxy)
 * - Actor pattern unique pour toutes les mutations
 * - Circuit breakers séparés READ/WRITE
 * - Merge déterministe sur conflits
 */
public class TimerSyncManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerSyncManager.class);
    
    // Configuration
    private static final Duration PERIODIC_SYNC_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DEBOUNCE_DELAY = Duration.ofSeconds(12);
    private static final Duration SANITY_CHECK_DELAY = Duration.ofSeconds(3);
    
    // Client unique
    private final CloudflareClient cloudflareClient;
    private final Gson gson;
    
    // Actor pattern
    private final Actor syncActor;
    
    // État atomique
    private final AtomicReference<WorkerTimerSyncData> currentData;
    private final AtomicReference<String> currentETag;
    
    // Anti-double déclenchement
    private final ConcurrentHashMap<String, Long> processedEvents;
    private static final long EVENT_TTL_MS = 30_000; // 30 secondes
    
    // Flags
    private volatile boolean syncEnabled = true;
    private volatile boolean inFlightGet = false;
    private volatile boolean shutdown = false;
    
    public TimerSyncManager() {
        LOGGER.info("🚀 Initialisation nouveau TimerSyncManager (Cloudflare Worker Proxy)");
        
        this.cloudflareClient = new CloudflareClient();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        this.syncActor = new Actor("TimerSync");
        
        this.currentData = new AtomicReference<>(createEmptyData());
        this.currentETag = new AtomicReference<>(null);
        
        this.processedEvents = new ConcurrentHashMap<>();
        
        // Démarrage immédiat
        startPeriodicTasks();
        performInitialLoad();
        
        LOGGER.info("✅ TimerSyncManager initialisé et prêt");
    }
    
    // ================== INTERFACE PUBLIQUE ==================
    
    /**
     * Crée ou met à jour un timer
     */
    public void createOrUpdateTimer(String dimensionName, TimerData timerData) {
        if (!syncEnabled || shutdown) {
            LOGGER.debug("⏸️ Sync désactivé ou arrêté - timer ignoré");
            return;
        }
        
        String eventId = generateEventId(dimensionName, timerData);
        if (!shouldProcessEvent(eventId)) {
            LOGGER.debug("🔄 Événement déjà traité - ignoré: {}", eventId);
            return;
        }
        
        String opId = "CREATE-" + ShortId.newId();
        LOGGER.info("📝 Programmation timer: {} [{}]", dimensionName, opId);
        
        // Scheduling via Actor avec debounce
        syncActor.schedule(() -> {
            performCreateOrUpdate(dimensionName, timerData, opId);
        }, DEBOUNCE_DELAY);
        
        LOGGER.info("⏳ Upload programmé dans {}s [{}]", DEBOUNCE_DELAY.getSeconds(), opId);
    }
    
    /**
     * Obtient un timer pour une dimension
     */
    public TimerData getTimer(String dimensionName) {
        WorkerTimerSyncData data = currentData.get();
        if (data == null || data.timers == null) {
            return null;
        }
        
        WorkerTimerSyncData.SyncTimer syncTimer = data.timers.get(dimensionName);
        if (syncTimer == null) {
            return null;
        }
        
        return syncTimer.toTimerData(dimensionName);
    }
    
    /**
     * Obtient tous les timers actifs
     */
    public Map<String, TimerData> getAllTimers() {
        WorkerTimerSyncData data = currentData.get();
        if (data == null || data.timers == null) {
            return Map.of();
        }
        
        Map<String, TimerData> result = new ConcurrentHashMap<>();
        data.timers.forEach((dimension, syncTimer) -> {
            TimerData timerData = syncTimer.toTimerData(dimension);
            if (!timerData.isExpired()) {
                result.put(dimension, timerData);
            }
        });
        
        return result;
    }
    
    /**
     * Active/désactive la synchronisation
     */
    public void setSyncEnabled(boolean enabled) {
        this.syncEnabled = enabled;
        LOGGER.info("🔧 Synchronisation: {}", enabled ? "ACTIVÉE" : "DÉSACTIVÉE");
    }
    
    /**
     * Vérifie si la synchronisation est activée
     */
    public boolean getSyncEnabled() {
        return syncEnabled;
    }
    
    /**
     * Métriques de debug
     */
    public String getDebugMetrics() {
        WorkerTimerSyncData data = currentData.get();
        int timerCount = (data != null && data.timers != null) ? data.timers.size() : 0;
        
        return String.format("TimerSync[enabled=%s, timers=%d, etag=%s, cf=%s]",
            syncEnabled, timerCount, 
            currentETag.get() != null ? currentETag.get().substring(0, Math.min(8, currentETag.get().length())) + "..." : "null",
            cloudflareClient.getDebugMetrics()
        );
    }
    
    // ================== IMPLÉMENTATION INTERNE ==================
    
    private void startPeriodicTasks() {
        // Sync périodique toutes les 30s
        syncActor.scheduleAtFixedRate(() -> {
            if (!shutdown && syncEnabled) {
                performPeriodicSync();
            }
        }, PERIODIC_SYNC_INTERVAL, PERIODIC_SYNC_INTERVAL);
        
        // Nettoyage des événements expirés toutes les 60s
        syncActor.scheduleAtFixedRate(() -> {
            cleanupExpiredEvents();
        }, Duration.ofSeconds(60), Duration.ofSeconds(60));
        
        LOGGER.info("⏰ Tâches périodiques démarrées (sync={}s)", PERIODIC_SYNC_INTERVAL.getSeconds());
    }
    
    private void performInitialLoad() {
        syncActor.schedule(() -> {
            String opId = "INIT-" + ShortId.newId();
            LOGGER.info("🔄 Chargement initial... [{}]", opId);
            
            performWorkerGet(null, opId + "-INIT");
        }, Duration.ofSeconds(1));
    }
    
    private void performPeriodicSync() {
        if (inFlightGet) {
            LOGGER.debug("⏳ GET déjà en cours - sync périodique ignorée");
            return;
        }
        
        String opId = "SYNC-" + ShortId.newId();
        LOGGER.debug("🔄 Sync périodique... [{}]", opId);
        
        performWorkerGet(currentETag.get(), opId);
    }
    
    private void performCreateOrUpdate(String dimensionName, TimerData timerData, String opId) {
        LOGGER.info("🚀 Démarrage pipeline creation/update [{}] (Thread: {})", opId, Thread.currentThread().getName());
        
        try {
            // 1. Revalidation GET avant POST
            LOGGER.debug("📥 1. Revalidation GET... [{}]", opId);
            String currentETagValue = currentETag.get();
            CloudflareClient.GetResult getResult = cloudflareClient.getTimers(currentETagValue, opId + "-REVAL");
            
            if (getResult.isSuccess() && getResult.isNewContent()) {
                // Nouvelles données - merge
                LOGGER.info("🔄 Nouvelles données détectées - merge... [{}]", opId);
                WorkerTimerSyncData remoteData = gson.fromJson(getResult.getJsonBody(), WorkerTimerSyncData.class);
                performDeterministicMerge(remoteData, getResult.getEtag(), opId);
            }
            
            // 2. Mise à jour locale
            LOGGER.debug("📝 2. Mise à jour locale... [{}]", opId);
            WorkerTimerSyncData data = currentData.get();
            data = data.copy(); // Copie pour immutabilité
            data.putTimer(dimensionName, timerData);
            data.lastUpdated = TimeAuthority.getInstance().now().toString();
            currentData.set(data);
            
            // 3. POST Worker (write proxy)
            LOGGER.info("📤 3. POST Worker proxy... [{}]", opId);
            String jsonToUpload = gson.toJson(data);
            CloudflareClient.PostResult postResult = cloudflareClient.postTimers(jsonToUpload, currentETagValue, opId + "-POST");
            
            if (postResult.isSuccess()) {
                LOGGER.info("✅ POST Worker réussi - ETag: {} [{}]", postResult.getEtag(), opId);
                
                // 4. Sanity check après 3s
                syncActor.schedule(() -> {
                    performSanityCheck(postResult.getEtag(), opId + "-SANITY");
                }, SANITY_CHECK_DELAY);
                
            } else if (postResult.isPreconditionFailed()) {
                LOGGER.warn("⚠️ POST Worker - conflit 412, relance revalidation [{}]", opId);
                // Laisser le retry handler s'en occuper
                
            } else {
                LOGGER.error("❌ POST Worker échoué: {} [{}]", postResult.getErrorMessage(), opId);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Erreur pipeline creation/update [{}]", opId, e);
        }
    }
    
    private void performWorkerGet(String ifNoneMatchETag, String opId) {
        if (inFlightGet) {
            LOGGER.debug("⏳ GET déjà en cours - ignoré [{}]", opId);
            return;
        }
        
        inFlightGet = true;
        try {
            CloudflareClient.GetResult result = cloudflareClient.getTimers(ifNoneMatchETag, opId);
            
            if (result.isSuccess() && result.isNewContent()) {
                LOGGER.info("📥 Nouvelles données Worker reçues [{}]", opId);
                WorkerTimerSyncData newData = gson.fromJson(result.getJsonBody(), WorkerTimerSyncData.class);
                performDeterministicMerge(newData, result.getEtag(), opId);
                
            } else if (result.isNotModified()) {
                LOGGER.debug("304 - Pas de changement [{}]", opId);
                
            } else {
                LOGGER.warn("⚠️ GET Worker échoué: {} [{}]", result.getErrorMessage(), opId);
            }
            
        } finally {
            inFlightGet = false;
        }
    }
    
    private void performDeterministicMerge(WorkerTimerSyncData remoteData, String newETag, String opId) {
        LOGGER.debug("🔄 Merge déterministe... [{}]", opId);
        
        WorkerTimerSyncData localData = currentData.get();
        
        // Pour l'instant, simple remplacement par remote (later: vraie logique de merge)
        // TODO: Implémenter merge déterministe selon les règles définies
        
        currentData.set(remoteData);
        currentETag.set(newETag);
        
        LOGGER.info("✅ Merge terminé - {} timers, ETag: {} [{}]", 
                   remoteData.timers.size(), 
                   newETag.substring(0, Math.min(8, newETag.length())) + "...",
                   opId);
    }
    
    private void performSanityCheck(String expectedETag, String opId) {
        LOGGER.debug("🔍 Sanity check... [{}]", opId);
        
        CloudflareClient.GetResult result = cloudflareClient.getTimers(null, opId);
        
        if (result.isSuccess()) {
            String actualETag = result.getEtag();
            if (expectedETag.equals(actualETag)) {
                LOGGER.info("✅ Sanity check OK - propagation confirmée [{}]", opId);
            } else {
                LOGGER.warn("⚠️ Sanity check: ETag différent (expected={}, actual={}) [{}]", 
                           expectedETag.substring(0, 8) + "...", 
                           actualETag.substring(0, 8) + "...", opId);
            }
        } else {
            LOGGER.warn("⚠️ Sanity check échoué: {} [{}]", result.getErrorMessage(), opId);
        }
    }
    
    // ================== UTILITAIRES ==================
    
    private WorkerTimerSyncData createEmptyData() {
        WorkerTimerSyncData data = new WorkerTimerSyncData();
        data.version = "1.0.0";
        data.lastUpdated = TimeAuthority.getInstance().now().toString();
        data.ttlMinutes = 60;
        data.timers = new ConcurrentHashMap<>();
        return data;
    }
    
    private String generateEventId(String dimensionName, TimerData timerData) {
        return dimensionName + "@" + timerData.getExpiresAtUtc().getEpochSecond();
    }
    
    private boolean shouldProcessEvent(String eventId) {
        long now = System.currentTimeMillis();
        Long lastProcessed = processedEvents.get(eventId);
        
        if (lastProcessed != null && (now - lastProcessed) < EVENT_TTL_MS) {
            return false; // Déjà traité récemment
        }
        
        processedEvents.put(eventId, now);
        return true;
    }
    
    private void cleanupExpiredEvents() {
        long now = System.currentTimeMillis();
        processedEvents.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > EVENT_TTL_MS);
    }
    
    @Override
    public void close() {
        LOGGER.info("🛑 Arrêt TimerSyncManager...");
        shutdown = true;
        
        if (syncActor != null) {
            syncActor.shutdown();
        }
        if (cloudflareClient != null) {
            cloudflareClient.close();
        }
        
        LOGGER.info("✅ TimerSyncManager arrêté");
    }
}
