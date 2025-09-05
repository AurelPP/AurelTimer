package com.aureltimer.sync;

import com.aureltimer.models.WorkerTimerSyncData;
import com.aureltimer.utils.TimeAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ✅ CLIENT CLOUDFLARE (Worker + R2)
 * 
 * Client HTTP natif pour éviter les dépendances AWS SDK.
 * Utilise HttpURLConnection pour GET/POST vers Cloudflare Workers.
 */
public class CloudflareClient {
    
    // Classes de résultat pour compatibilité
    public static class GetResult {
        public final WorkerTimerSyncData data;
        public final String etag;
        public final boolean success;
        public final String jsonBody;
        
        public GetResult(WorkerTimerSyncData data, String etag, boolean success) {
            this.data = data;
            this.etag = etag;
            this.success = success;
            this.jsonBody = data != null ? data.toJson() : null;
        }
        
        public boolean isSuccess() { return success; }
        public boolean isNewContent() { return data != null; }
        public boolean isNotModified() { return success && data == null; }
        public String getEtag() { return etag; }
        public String getJsonBody() { return jsonBody; }
        public String getErrorMessage() { return success ? null : "Erreur GET"; }
    }
    
    public static class PostResult {
        public final boolean success;
        public final String error;
        public final String etag;
        
        public PostResult(boolean success, String error) {
            this.success = success;
            this.error = error;
            this.etag = null;
        }
        
        public PostResult(boolean success, String error, String etag) {
            this.success = success;
            this.error = error;
            this.etag = etag;
        }
        
        public boolean isSuccess() { return success; }
        public boolean isPreconditionFailed() { return !success && error != null && error.contains("412"); }
        public String getEtag() { return etag; }
        public String getErrorMessage() { return error; }
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareClient.class);
    
    // Circuit breakers séparés pour lecture/écriture
    private static final AtomicLong readCircuitBreakerUntil = new AtomicLong(0);
    private static final AtomicLong writeCircuitBreakerUntil = new AtomicLong(0);
    private static final AtomicInteger readFailures = new AtomicInteger(0);
    private static final AtomicInteger writeFailures = new AtomicInteger(0);
    
    // Configuration
    private static final int MAX_FAILURES = 3;
    private static final Duration CIRCUIT_BREAKER_DURATION = Duration.ofMinutes(2);
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    
    /**
     * GET timers depuis Cloudflare Worker
     */
    public GetResult getTimers(String etag, String opId) {
        if (isCircuitBreakerOpen(readCircuitBreakerUntil.get())) {
            LOGGER.warn("🔴 Circuit breaker lecture ouvert - skip GET [{}]", opId);
            return new GetResult(null, null, false);
        }
        
        LOGGER.debug("🔍 Tentative GET timers... [{}]", opId);
        
        try {
            URL url = new URL(CloudflareConfig.WORKER_TIMERS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Configuration
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "AurelTimer/1.4.3");
            
            // ETag pour cache
            if (etag != null && !etag.isEmpty()) {
                conn.setRequestProperty("If-None-Match", etag);
            }
            
            // Headers pour TimeAuthority
            conn.setRequestProperty("Date", Instant.now().toString());
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 304) {
                LOGGER.debug("📋 Cache hit (304) - pas de changement");
                readFailures.set(0);
                return new GetResult(null, etag, true); // Pas de changement
            }
            
            if (responseCode == 200) {
                // Lire la réponse
                String responseBody = readResponse(conn);
                String newEtag = conn.getHeaderField("ETag");
                
                // Mettre à jour TimeAuthority
                String dateHeader = conn.getHeaderField("Date");
                if (dateHeader != null) {
                    TimeAuthority.updateFromHttpDate(dateHeader);
                }
                
                // Parser JSON
                WorkerTimerSyncData data = WorkerTimerSyncData.fromJson(responseBody);
                if (data != null) {
                    data.setEtag(newEtag);
                    LOGGER.info("📥 GET réussi - {} timers, ETag: {}", 
                        data.getTimers().size(), newEtag);
                    readFailures.set(0);
                    return new GetResult(data, newEtag, true);
                }
            }
            
            // Gestion des erreurs
            handleHttpError("GET", responseCode, conn);
            readFailures.incrementAndGet();
            
            LOGGER.warn("❌ GET échoué - Code: {}, Échecs: {}/{} [{}]", 
                       responseCode, readFailures.get(), MAX_FAILURES, opId);
            
            if (readFailures.get() >= MAX_FAILURES) {
                openCircuitBreaker(readCircuitBreakerUntil);
                LOGGER.warn("🔴 Circuit breaker lecture ouvert après {} échecs [{}]", MAX_FAILURES, opId);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Erreur GET timers", e);
            readFailures.incrementAndGet();
            
            if (readFailures.get() >= MAX_FAILURES) {
                openCircuitBreaker(readCircuitBreakerUntil);
                LOGGER.warn("🔴 Circuit breaker lecture ouvert après {} échecs", MAX_FAILURES);
            }
        }
        
        return new GetResult(null, null, false);
    }
    
    /**
     * POST timers vers Cloudflare Worker
     */
    public PostResult postTimers(String jsonData, String ifMatch, String opId) {
        if (isCircuitBreakerOpen(writeCircuitBreakerUntil.get())) {
            LOGGER.warn("🔴 Circuit breaker écriture ouvert - skip POST");
            return new PostResult(false, "Circuit breaker ouvert");
        }
        
        try {
            URL url = new URL(CloudflareConfig.WORKER_TIMERS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Configuration
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "AurelTimer/1.4.3");
            conn.setRequestProperty("Authorization", "Bearer " + CloudflareConfig.getWriteToken());
            
            // If-Match pour concurrence optimiste
            if (ifMatch != null && !ifMatch.isEmpty()) {
                conn.setRequestProperty("If-Match", ifMatch);
            }
            
            // Headers pour TimeAuthority
            conn.setRequestProperty("Date", Instant.now().toString());
            
            // Corps de la requête
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonData.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200 || responseCode == 201) {
                LOGGER.info("📤 POST réussi - timers synchronisés");
                writeFailures.set(0);
                return new PostResult(true, null);
            }
            
            if (responseCode == 412) {
                LOGGER.warn("⚠️ POST 412 - conflit de version, retry nécessaire");
                writeFailures.incrementAndGet();
                return new PostResult(false, "Conflit de version (412)");
            }
            
            if (responseCode == 409) {
                LOGGER.warn("⚠️ POST 409 - conflit, retry nécessaire");
                writeFailures.incrementAndGet();
                return new PostResult(false, "Conflit (409)");
            }
            
            // Gestion des erreurs
            handleHttpError("POST", responseCode, conn);
            writeFailures.incrementAndGet();
            
            if (writeFailures.get() >= MAX_FAILURES) {
                openCircuitBreaker(writeCircuitBreakerUntil);
                LOGGER.warn("🔴 Circuit breaker écriture ouvert après {} échecs", MAX_FAILURES);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Erreur POST timers", e);
            writeFailures.incrementAndGet();
            
            if (writeFailures.get() >= MAX_FAILURES) {
                openCircuitBreaker(writeCircuitBreakerUntil);
                LOGGER.warn("🔴 Circuit breaker écriture ouvert après {} échecs", MAX_FAILURES);
            }
        }
        
        return new PostResult(false, "Erreur inconnue");
    }
    
    /**
     * Lire la réponse HTTP
     */
    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    /**
     * Gérer les erreurs HTTP
     */
    private static void handleHttpError(String method, int responseCode, HttpURLConnection conn) {
        String errorBody = "";
        try {
            if (conn.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    errorBody = error.toString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        LOGGER.error("❌ {} échoué - Code: {}, Body: {}", method, responseCode, errorBody);
    }
    
    /**
     * Vérifier si le circuit breaker est ouvert
     */
    private static boolean isCircuitBreakerOpen(long until) {
        return until > System.currentTimeMillis();
    }
    
    /**
     * Ouvrir le circuit breaker
     */
    private static void openCircuitBreaker(AtomicLong breaker) {
        breaker.set(System.currentTimeMillis() + CIRCUIT_BREAKER_DURATION.toMillis());
    }
    
    /**
     * Fermer le circuit breaker (pour tests)
     */
    public static void resetCircuitBreakers() {
        readCircuitBreakerUntil.set(0);
        writeCircuitBreakerUntil.set(0);
        readFailures.set(0);
        writeFailures.set(0);
        LOGGER.info("🔄 Circuit breakers réinitialisés");
    }
    
    /**
     * Obtient les métriques de debug
     */
    public String getDebugMetrics() {
        return String.format("CB[r=%d,w=%d]", 
            readFailures.get(), writeFailures.get());
    }
    
    /**
     * Ferme le client (pour compatibilité)
     */
    public void close() {
        // Rien à fermer pour HttpURLConnection
    }
}
