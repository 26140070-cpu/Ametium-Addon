package com.ametium.addon.mixin.accessor;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WidgetScreen.class)
public interface WidgetScreenAccessor {
    @Accessor("theme")
    GuiTheme getTheme();
}
