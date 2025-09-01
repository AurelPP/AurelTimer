package com.aureltimer.utils;

import java.security.SecureRandom;

/**
 * ✅ GÉNÉRATEUR D'ID COURTS pour corrélation logs
 */
public class ShortId {
    
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LENGTH = 6; // IDs de 6 caractères
    
    /**
     * Génère un ID court unique (ex: "A7B2F9")
     */
    public static String newId() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}


