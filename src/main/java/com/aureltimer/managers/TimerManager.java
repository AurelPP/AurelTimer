package com.aureltimer.managers;

import com.aureltimer.models.DimensionTimer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerManager.class);
    private final Map<String, DimensionTimer> dimensionTimers = new ConcurrentHashMap<>();
    
    public void updateTimer(String dimensionName, int minutes, int seconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime spawnTime = now.plusMinutes(minutes).plusSeconds(seconds);
        
        DimensionTimer timer = new DimensionTimer(dimensionName, minutes, seconds, spawnTime);
        dimensionTimers.put(dimensionName, timer);
        
        LOGGER.info("Timer mis à jour pour {}: {} minutes et {} secondes", dimensionName, minutes, seconds);
    }
    
    public void updateTimer(String dimensionName, String timeString) {
        // Parse du format "X minutes et Y secondes"
        try {
            String[] parts = timeString.split(" et ");
            int minutes = 0;
            int seconds = 0;
            
            if (parts.length >= 1) {
                String minutesPart = parts[0].trim();
                if (minutesPart.contains("minute")) {
                    minutes = Integer.parseInt(minutesPart.replaceAll("[^0-9]", ""));
                }
            }
            
            if (parts.length >= 2) {
                String secondsPart = parts[1].trim();
                if (secondsPart.contains("seconde")) {
                    seconds = Integer.parseInt(secondsPart.replaceAll("[^0-9]", ""));
                }
            }
            
            updateTimer(dimensionName, minutes, seconds);
        } catch (Exception e) {
            LOGGER.error("Erreur lors du parsing du timer: {}", timeString, e);
        }
    }
    
    public Map<String, DimensionTimer> getAllTimers() {
        return new HashMap<>(dimensionTimers);
    }
    
    public DimensionTimer getTimer(String dimensionName) {
        return dimensionTimers.get(dimensionName);
    }
    
    public void clearTimer(String dimensionName) {
        dimensionTimers.remove(dimensionName);
        LOGGER.info("Timer supprimé pour {}", dimensionName);
    }
    
    public void clearAllTimers() {
        dimensionTimers.clear();
        LOGGER.info("Tous les timers ont été supprimés");
    }
    
    public boolean hasTimer(String dimensionName) {
        return dimensionTimers.containsKey(dimensionName);
    }
    
    public int getActiveTimerCount() {
        return dimensionTimers.size();
    }
}
