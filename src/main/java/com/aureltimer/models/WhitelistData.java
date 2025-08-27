package com.aureltimer.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modèle de données pour la whitelist dynamique
 */
public class WhitelistData {
    
    public static class WhitelistEntry {
        public List<String> usernames;
        public List<String> uuids;
        
        public WhitelistEntry() {}
    }
    
    public static class Settings {
        public boolean enabled;
        public String message_unauthorized;
        
        public Settings() {}
    }
    
    public String version;
    public String last_updated;
    public int ttl_minutes;
    public WhitelistEntry whitelist;
    public Settings settings;
    
    public WhitelistData() {}
    
    /**
     * Vérifie si un joueur est autorisé par username
     */
    public boolean isPlayerAuthorized(String username, String uuid) {
        if (settings == null || !settings.enabled) {
            return true; // Si la whitelist est désactivée, tout le monde passe
        }
        
        // Vérification par username
        if (username != null && whitelist != null && whitelist.usernames != null) {
            for (String allowedUsername : whitelist.usernames) {
                if (allowedUsername.equalsIgnoreCase(username)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Récupère le message d'erreur personnalisé
     */
    public String getUnauthorizedMessage() {
        if (settings != null && settings.message_unauthorized != null) {
            return settings.message_unauthorized;
        }
        return "⛔ Accès refusé";
    }
    
    /**
     * Récupère le TTL en millisecondes
     */
    public long getTtlMilliseconds() {
        return ttl_minutes * 60L * 1000L;
    }
    
    /**
     * Vérifie si la whitelist est activée
     */
    public boolean isEnabled() {
        return settings != null && settings.enabled;
    }
    
    @Override
    public String toString() {
        return String.format("WhitelistData{version='%s', enabled=%s, users=%d, uuids=%d}", 
            version, 
            isEnabled(), 
            whitelist != null && whitelist.usernames != null ? whitelist.usernames.size() : 0,
            whitelist != null && whitelist.uuids != null ? whitelist.uuids.size() : 0);
    }
}
