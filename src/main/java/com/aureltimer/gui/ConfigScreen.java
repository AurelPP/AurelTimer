package com.aureltimer.gui;

import com.aureltimer.config.ModConfig;
import com.aureltimer.AurelTimerMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Écran de configuration du mod Aurel Timer
 */
public class ConfigScreen extends Screen {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigScreen");
    private final Screen parent;
    private final ModConfig config;
    
    private ButtonWidget alertDisplayButton;
    private ButtonWidget soundEnabledButton;
    private ButtonWidget syncEnabledButton;
    private SliderWidget soundVolumeSlider;
    private ButtonWidget playSoundButton;
    private SliderWidget maxDisplayedTimersSlider;
    private ButtonWidget phaseColorsButton;
    private SliderWidget transparencySlider;
    private ButtonWidget doneButton;
    
    public ConfigScreen(Screen parent) {
        super(Text.literal("Configuration Aurel Timer"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Vérifier l'autorisation avant d'afficher l'interface
        if (AurelTimerMod.getWhitelistManager() != null && !AurelTimerMod.getWhitelistManager().isVerified()) {
            if (!AurelTimerMod.getWhitelistManager().hasChecked()) {
                // En cours de vérification
                this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Vérification en cours..."), 
                    (button) -> {}).dimensions(this.width / 2 - 100, this.height / 2, 200, 20).build());
                return;
            } else {
                // Vérification échouée
                String message = AurelTimerMod.getWhitelistManager().getUnauthorizedMessage();
                this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(message), 
                    (button) -> {}).dimensions(this.width / 2 - 150, this.height / 2, 300, 20).build());
                
                this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Retour"), 
                    (button) -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 50, this.height / 2 + 30, 100, 20).build());
                return;
            }
        }
        
        int centerX = this.width / 2;
        int startY = 80; // Déplacé vers le haut
        
        // Bouton pour l'affichage de l'alerte
        this.alertDisplayButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Affichage alerte: " + config.getAlertDisplay().toString()), 
            (button) -> {
                cycleAlertDisplay();
            }).dimensions(centerX - 100, startY, 200, 20).build());
        
        // Bouton pour le son
        this.soundEnabledButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Son alerte: " + config.getSoundEnabled().toString()), 
            (button) -> {
                cycleSoundEnabled();
            }).dimensions(centerX - 100, startY + 30, 200, 20).build());
        
        // Slider pour le volume du son
        this.soundVolumeSlider = this.addDrawableChild(new SliderWidget(
            centerX - 100, startY + 60, 150, 20,
            Text.literal("Volume son: " + config.getSoundVolume() + "%"),
            config.getSoundVolume() / 100.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Volume son: " + (int)(this.value * 100) + "%"));
            }
            
            @Override
            protected void applyValue() {
                int newVolume = (int)(this.value * 100);
                config.setSoundVolume(newVolume);
            }
        });
        
        // Bouton play pour tester le son
        this.playSoundButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("▶"), 
            (button) -> {
                if (client != null && client.player != null) {
                    float volume = config.getNormalizedSoundVolume();
                    client.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, volume, 1.0f);
                    LOGGER.info("🔊 Test son joué avec volume: {}%", config.getSoundVolume());
                } else {
                    LOGGER.warn("❌ Impossible de jouer le son: client ou player null");
                }
            }).dimensions(centerX + 60, startY + 60, 40, 20).build());
        
        // Bouton pour la synchronisation
        this.syncEnabledButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Synchronisation: " + config.getSyncEnabled().toString()), 
            (button) -> {
                cycleSyncEnabled();
            }).dimensions(centerX - 100, startY + 90, 200, 20).build());
        
        // Slider pour le nombre de timers affichés
        this.maxDisplayedTimersSlider = this.addDrawableChild(new SliderWidget(
            centerX - 100, startY + 120, 200, 20,
            Text.literal("Timers affichés: " + config.getMaxDisplayedTimers()),
            (config.getMaxDisplayedTimers() - 1) / 5.0) {
            @Override
            protected void updateMessage() {
                int value = (int)(this.value * 5) + 1;
                setMessage(Text.literal("Timers affichés: " + value));
            }
            
            @Override
            protected void applyValue() {
                int newValue = (int)(this.value * 5) + 1;
                config.setMaxDisplayedTimers(newValue);
            }
        });
        
        // Bouton pour les couleurs de phase
        this.phaseColorsButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Couleurs phase: " + (config.isPhaseColorsEnabled() ? "Oui" : "Non")), 
            (button) -> {
                config.setPhaseColorsEnabled(!config.isPhaseColorsEnabled());
                phaseColorsButton.setMessage(Text.literal("Couleurs phase: " + (config.isPhaseColorsEnabled() ? "Oui" : "Non")));
            }).dimensions(centerX - 100, startY + 150, 200, 20).build());
        
        // Slider pour l'opacité de l'interface
        this.transparencySlider = this.addDrawableChild(new SliderWidget(
            centerX - 100, startY + 180, 200, 20,
            Text.literal("Opacité: " + config.getInterfaceTransparency() + "%"),
            config.getInterfaceTransparency() / 100.0) {
            @Override
            protected void updateMessage() {
                int value = (int)(this.value * 100);
                setMessage(Text.literal("Opacité: " + value + "%"));
            }
            
            @Override
            protected void applyValue() {
                int newValue = (int)(this.value * 100);
                config.setInterfaceTransparency(newValue);
                
                // Afficher l'interface des timers pour voir l'opacité en temps réel
                if (AurelTimerMod.getTimerOverlay() != null) {
                    AurelTimerMod.getTimerOverlay().setVisible(true);
                }
            }
        });
        
        // Bouton Terminé
        this.doneButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Terminé"), (button) -> {
            this.client.setScreen(this.parent);
        }).dimensions(centerX - 50, startY + 220, 100, 20).build());
    }
    
    private void cycleAlertDisplay() {
        ModConfig.AlertDisplay[] values = ModConfig.AlertDisplay.values();
        ModConfig.AlertDisplay current = config.getAlertDisplay();
        int currentIndex = -1;
        
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        
        int nextIndex = (currentIndex + 1) % values.length;
        config.setAlertDisplay(values[nextIndex]);
        
        // Mettre à jour le texte du bouton
        alertDisplayButton.setMessage(Text.literal("Affichage alerte: " + config.getAlertDisplay().toString()));
    }
    
    private void cycleSoundEnabled() {
        ModConfig.SoundEnabled[] values = ModConfig.SoundEnabled.values();
        ModConfig.SoundEnabled current = config.getSoundEnabled();
        int currentIndex = -1;
        
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        
        int nextIndex = (currentIndex + 1) % values.length;
        config.setSoundEnabled(values[nextIndex]);
        
        // Mettre à jour le texte du bouton
        soundEnabledButton.setMessage(Text.literal("Son alerte: " + config.getSoundEnabled().toString()));
    }
    
    private void cycleSyncEnabled() {
        ModConfig.SyncEnabled[] values = ModConfig.SyncEnabled.values();
        ModConfig.SyncEnabled current = config.getSyncEnabled();
        int currentIndex = -1;
        
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        
        int nextIndex = (currentIndex + 1) % values.length;
        config.setSyncEnabled(values[nextIndex]);
        
        // Appliquer immédiatement le changement au TimerManager
        if (AurelTimerMod.getTimerManager() != null) {
            AurelTimerMod.getTimerManager().setSyncEnabled(config.shouldSyncTimers());
        }
        
        // Mettre à jour le texte du bouton
        syncEnabledButton.setMessage(Text.literal("Synchronisation: " + config.getSyncEnabled().toString()));
    }
    

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Vérifier l'autorisation avant d'afficher le contenu
        if (AurelTimerMod.getWhitelistManager() != null && !AurelTimerMod.getWhitelistManager().isVerified()) {
            if (!AurelTimerMod.getWhitelistManager().hasChecked()) {
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal("Vérification en cours..."), 
                    this.width / 2, this.height / 2 - 20, 0xFFFFAA);
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal("Veuillez patienter..."), 
                    this.width / 2, this.height / 2 + 10, 0xAAAAAA);
            } else {
                String message = AurelTimerMod.getWhitelistManager().getUnauthorizedMessage();
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal("Accès refusé"), 
                    this.width / 2, this.height / 2 - 20, 0xFF5555);
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal(message), 
                    this.width / 2, this.height / 2 + 10, 0xAAAAAA);
            }
        }
        
        // Rendre les widgets AVANT le texte de description
        super.render(context, mouseX, mouseY, delta);
        
        // Tout le texte rendu APRÈS les boutons (si autorisé)
        if (AurelTimerMod.getWhitelistManager() == null || AurelTimerMod.getWhitelistManager().isVerified()) {
            // Titre rendu en dernier pour être visible
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
            
            // Description des options (position ajustée)
            int startY = 80;
            context.drawTextWithShadow(this.textRenderer, Text.literal("Chat: Affiche l'alerte dans le chat"), this.width / 2 - 100, startY + 250, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Volume: Contrôle le volume du son d'enclume (bouton ▶ pour tester)"), this.width / 2 - 100, startY + 265, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Timers: Nombre de timers visibles (1-6)"), this.width / 2 - 100, startY + 280, 0xAAAAAA);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
