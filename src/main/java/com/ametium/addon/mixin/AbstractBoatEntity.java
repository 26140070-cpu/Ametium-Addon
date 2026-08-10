package com.ametium.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatEntity.class)
public abstract class AbstractBoatEntity {
    @Inject(method = "updatePaddles", at = @At("HEAD"), cancellable = true)
    private void cancelPaddles(CallbackInfo ci) {
        Module module = Modules.get().get("boat-noclip");
        if (module != null && module.isActive()) ci.cancel();
    }
}
