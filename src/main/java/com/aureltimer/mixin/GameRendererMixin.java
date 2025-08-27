package com.aureltimer.mixin;

import com.aureltimer.AurelTimerMod;
import com.aureltimer.gui.TimerOverlay;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class GameRendererMixin {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        try {
            TimerOverlay timerOverlay = AurelTimerMod.getTimerOverlay();
            if (timerOverlay != null && timerOverlay.isVisible()) {
                // Créer un DrawContext simple
                net.minecraft.client.gui.DrawContext context = new net.minecraft.client.gui.DrawContext(
                    net.minecraft.client.MinecraftClient.getInstance(),
                    net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                );
                timerOverlay.render(context);
            }
        } catch (Exception e) {
            // Ignorer les erreurs pour éviter de casser le rendu
        }
    }
}
