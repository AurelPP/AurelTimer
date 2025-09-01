package com.aureltimer.sync;

/**
 * ✅ CONFIGURATION CLOUDFLARE (Worker + Write Token)
 * 
 * Simple et propre - plus de secrets R2 embarqués !
 * Le Worker gère l'écriture avec son propre token.
 */
public class CloudflareConfig {
    
    // Worker URLs
    public static final String WORKER_BASE_URL = "https://aureltimer-sync.aure-perreyprillo.workers.dev";
    public static final String TIMERS_PATH = "/timer_sync.json";
    public static final String WHITELIST_PATH = "/whitelist.json";
    
    public static final String WORKER_TIMERS_URL = WORKER_BASE_URL + TIMERS_PATH;
    public static final String WORKER_WHITELIST_URL = WORKER_BASE_URL + WHITELIST_PATH;
    
    // Write token (obfusqué)
    private static final int[] ENCODED_WRITE_TOKEN = {
        42, 7, 38, 10, 7, 104, 6, 43, 84, 20,
        15, 86, 10, 109, 124, 56, 13, 71, 7, 12,
        41, 34, 27, 32, 120, 17, 81, 87, 8, 93,
        59, 76, 47, 55, 50, 52, 50, 38, 109, 80,
        96, 89, 33, 42, 15, 57, 23, 77, 46, 41,
        14, 6, 94, 70, 126, 81, 17, 26, 63, 47,
        5, 59, 20, 4
    };
    
    /**
     * Obtient le token d'écriture déobfusqué
     */
    public static String getWriteToken() {
        return decodeSecret(ENCODED_WRITE_TOKEN, "AurelTimer2024");
    }
    
    /**
     * Décode un secret obfusqué
     */
    private static String decodeSecret(int[] encoded, String key) {
        StringBuilder result = new StringBuilder();
        try {
            for (int i = 0; i < encoded.length; i++) {
                int keyChar = key.charAt(i % key.length());
                int decoded = encoded[i] ^ keyChar ^ (i % 256);
                result.append((char) decoded);
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur décodage secret", e);
        }
    }
}
