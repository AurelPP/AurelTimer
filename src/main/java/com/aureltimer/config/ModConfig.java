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
    
    // Valeurs par défaut
    private AlertDisplay alertDisplay = AlertDisplay.CHAT;
    private SoundEnabled soundEnabled = SoundEnabled.YES;
    
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
}
