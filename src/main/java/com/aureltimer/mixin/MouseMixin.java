package com.aureltimer.mixin;

import com.aureltimer.AurelTimerMod;
import com.aureltimer.gui.TimerOverlay;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        try {
            TimerOverlay timerOverlay = AurelTimerMod.getTimerOverlay();
            if (timerOverlay != null && timerOverlay.isVisible()) {
                Mouse mouse = (Mouse) (Object) this;
                double mouseX = mouse.getX() * 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth() / 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getWidth();
                double mouseY = mouse.getY() * 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight() / 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getHeight();
                
                if (action == 1) { // Clic pressé
                    if (timerOverlay.handleMouseClick(mouseX, mouseY, button)) {
                        ci.cancel(); // Annuler l'événement pour éviter les interactions avec le monde
                    }
                } else if (action == 0) { // Clic relâché
                    if (timerOverlay.handleMouseRelease(mouseX, mouseY, button)) {
                        ci.cancel();
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs pour éviter de casser les interactions
        }
    }
    
    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double xpos, double ypos, CallbackInfo ci) {
        try {
            TimerOverlay timerOverlay = AurelTimerMod.getTimerOverlay();
            if (timerOverlay != null && timerOverlay.isVisible()) {
                // Convertir les coordonnées de la fenêtre vers les coordonnées de l'écran mis à l'échelle
                double mouseX = xpos * 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth() / 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getWidth();
                double mouseY = ypos * 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight() / 
                    net.minecraft.client.MinecraftClient.getInstance().getWindow().getHeight();
                
                // Gérer le drag si en cours
                if (timerOverlay.handleMouseDrag(mouseX, mouseY, 0, 0, 0)) {
                    ci.cancel(); // Annuler l'événement pour éviter les interactions avec le monde
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs pour éviter de casser les interactions
        }
    }
}
