package com.aureltimer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration du mod Aurel Timer
 */
public class ModConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("ModConfig");
    private static final String CONFIG_FILE = "aurel-timer-config.json";
    
    // Options de configuration
    public enum AlertDisplay {
        CHAT("Chat"),
        NONE("Non");
        
        private final String displayName;
        
        AlertDisplay(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public enum SoundEnabled {
        YES("Oui"),
        NO("Non");
        
        private final String displayName;
        
        SoundEnabled(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public enum SyncEnabled {
        YES("Oui"),
        NO("Non");
        
        private final String displayName;
        
        SyncEnabled(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Valeurs par défaut
    private AlertDisplay alertDisplay = AlertDisplay.CHAT;
    private SoundEnabled soundEnabled = SoundEnabled.YES;
    private SyncEnabled syncEnabled = SyncEnabled.YES;
    
    // Position de l'interface des timers (-1 = centrée par défaut)
    private int overlayX = -1;
    private int overlayY = -1;
    
    // Nouveaux paramètres v1.4.4
    private int soundVolume = 100; // Volume du son d'enclume (0-100%)
    private int maxDisplayedTimers = 3; // Nombre de timers affichés (1-6)
    
    // Instance singleton
    private static ModConfig instance;
    
    private ModConfig() {}
    
    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
            instance.loadConfig();
        }
        return instance;
    }
    
    /**
     * Charge la configuration depuis le fichier
     */
    public void loadConfig() {
        try {
            File configFile = getConfigFile();
            if (configFile.exists()) {
                Gson gson = new Gson();
                try (FileReader reader = new FileReader(configFile)) {
                    ModConfig loadedConfig = gson.fromJson(reader, ModConfig.class);
                    if (loadedConfig != null) {
                        this.alertDisplay = loadedConfig.alertDisplay;
                        this.soundEnabled = loadedConfig.soundEnabled;
                        // Gérer la nouvelle option de sync (peut être null dans les anciens configs)
                        this.syncEnabled = loadedConfig.syncEnabled != null ? loadedConfig.syncEnabled : SyncEnabled.YES;
                        // Charger la position de l'interface (valeurs par défaut si non présentes)
                        this.overlayX = loadedConfig.overlayX;
                        this.overlayY = loadedConfig.overlayY;
                        // Charger les nouveaux paramètres v1.4.4 (valeurs par défaut si non présentes)
                        this.soundVolume = loadedConfig.soundVolume > 0 ? loadedConfig.soundVolume : 100;
                        this.maxDisplayedTimers = loadedConfig.maxDisplayedTimers > 0 ? Math.min(6, Math.max(1, loadedConfig.maxDisplayedTimers)) : 3;
                        LOGGER.info("Configuration chargée depuis {}", configFile.getAbsolutePath());
                    }
                }
            } else {
                saveConfig(); // Créer le fichier avec les valeurs par défaut
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement de la configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Sauvegarde la configuration dans le fichier
     */
    public void saveConfig() {
        try {
            File configFile = getConfigFile();
            configFile.getParentFile().mkdirs();
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(this, writer);
                LOGGER.info("Configuration sauvegardée dans {}", configFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la sauvegarde de la configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Obtient le fichier de configuration
     */
    private File getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE).toFile();
    }
    
    // Getters et Setters
    public AlertDisplay getAlertDisplay() {
        return alertDisplay;
    }
    
    public void setAlertDisplay(AlertDisplay alertDisplay) {
        this.alertDisplay = alertDisplay;
        saveConfig();
    }
    
    public SoundEnabled getSoundEnabled() {
        return soundEnabled;
    }
    
    public void setSoundEnabled(SoundEnabled soundEnabled) {
        this.soundEnabled = soundEnabled;
        saveConfig();
    }
    
    /**
     * Vérifie si l'alerte doit être affichée
     */
    public boolean shouldShowAlert() {
        return alertDisplay != AlertDisplay.NONE;
    }
    
    /**
     * Vérifie si le son doit être joué
     */
    public boolean shouldPlaySound() {
        return soundEnabled == SoundEnabled.YES;
    }
    
    /**
     * Vérifie si l'alerte doit être affichée dans le chat
     */
    public boolean shouldShowInChat() {
        return alertDisplay == AlertDisplay.CHAT;
    }
    
    public SyncEnabled getSyncEnabled() {
        return syncEnabled;
    }
    
    public void setSyncEnabled(SyncEnabled syncEnabled) {
        this.syncEnabled = syncEnabled;
        saveConfig();
    }
    
    /**
     * Vérifie si la synchronisation est activée
     */
    public boolean shouldSyncTimers() {
        return syncEnabled == SyncEnabled.YES;
    }
    
    // Getters et Setters pour la position de l'interface
    public int getOverlayX() {
        return overlayX;
    }
    
    public void setOverlayX(int overlayX) {
        this.overlayX = overlayX;
        saveConfig();
    }
    
    public int getOverlayY() {
        return overlayY;
    }
    
    public void setOverlayY(int overlayY) {
        this.overlayY = overlayY;
        saveConfig();
    }
    
    public void setOverlayPosition(int x, int y) {
        this.overlayX = x;
        this.overlayY = y;
        saveConfig();
    }
    
    // Setters temporaires pour le drag (sans sauvegarde immédiate)
    public void setOverlayXTemporary(int x) {
        this.overlayX = x;
    }
    
    public void setOverlayYTemporary(int y) {
        this.overlayY = y;
    }
    
    // Getters et Setters pour les nouveaux paramètres v1.4.4
    
    /**
     * Obtient le volume du son d'enclume (0-100%)
     */
    public int getSoundVolume() {
        return soundVolume;
    }
    
    /**
     * Définit le volume du son d'enclume (0-100%)
     */
    public void setSoundVolume(int soundVolume) {
        this.soundVolume = Math.max(0, Math.min(100, soundVolume));
        saveConfig();
    }
    
    /**
     * Obtient le nombre maximum de timers affichés (1-6)
     */
    public int getMaxDisplayedTimers() {
        return maxDisplayedTimers;
    }
    
    /**
     * Définit le nombre maximum de timers affichés (1-6)
     */
    public void setMaxDisplayedTimers(int maxDisplayedTimers) {
        this.maxDisplayedTimers = Math.max(1, Math.min(6, maxDisplayedTimers));
        saveConfig();
    }
    
    /**
     * Obtient le volume normalisé pour Minecraft (0.0-1.0)
     */
    public float getNormalizedSoundVolume() {
        return soundVolume / 100.0f;
    }
}
