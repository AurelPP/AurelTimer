package com.aureltimer.gui;

import com.aureltimer.config.ModConfig;
import com.aureltimer.AurelTimerMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
        int centerY = this.height / 2;
        
        // Bouton pour l'affichage de l'alerte
        this.alertDisplayButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Affichage alerte: " + config.getAlertDisplay().toString()), 
            (button) -> {
                cycleAlertDisplay();
            }).dimensions(centerX - 100, centerY - 30, 200, 20).build());
        
        // Bouton pour le son
        this.soundEnabledButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Son alerte: " + config.getSoundEnabled().toString()), 
            (button) -> {
                cycleSoundEnabled();
            }).dimensions(centerX - 100, centerY + 10, 200, 20).build());
        
        // Bouton pour la synchronisation
        this.syncEnabledButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Synchronisation: " + config.getSyncEnabled().toString()), 
            (button) -> {
                cycleSyncEnabled();
            }).dimensions(centerX - 100, centerY + 50, 200, 20).build());
        
        // Bouton Terminé
        this.doneButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Terminé"), (button) -> {
            this.client.setScreen(this.parent);
        }).dimensions(centerX - 50, centerY + 90, 100, 20).build());
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
            
            // Labels des options
            context.drawTextWithShadow(this.textRenderer, Text.literal("Affichage alerte:"), this.width / 2 - 100, this.height / 2 - 50, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Son alerte:"), this.width / 2 - 100, this.height / 2 - 10, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Synchronisation:"), this.width / 2 - 100, this.height / 2 + 30, 0xFFFFFF);
            
            // Description des options
            context.drawTextWithShadow(this.textRenderer, Text.literal("Chat: Affiche l'alerte dans le chat"), this.width / 2 - 100, this.height / 2 + 140, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Non: Désactive l'affichage de l'alerte"), this.width / 2 - 100, this.height / 2 + 155, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Sync: Partage les timers avec d'autres joueurs"), this.width / 2 - 100, this.height / 2 + 170, 0xAAAAAA);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
