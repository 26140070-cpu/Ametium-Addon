package com.ametium.addon.mixin;

import com.ametium.addon.utilities.AmetiumTab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Tabs.class, remap = false)
public class TabsMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private static void onInit(CallbackInfo ci) { Tabs.add(new AmetiumTab()); }
}
