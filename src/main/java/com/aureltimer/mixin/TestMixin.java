package com.aureltimer.mixin;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin de test pour vérifier que les mixins fonctionnent
 */
@Mixin(MinecraftClient.class)
public class TestMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("TestMixin");
    private static int tickCount = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        tickCount++;
        // Log toutes les 1000 ticks pour vérifier que le mixin fonctionne
        if (tickCount % 1000 == 0) {
            LOGGER.info("✅ TestMixin fonctionne ! Tick: {}", tickCount);
        }
    }
}
