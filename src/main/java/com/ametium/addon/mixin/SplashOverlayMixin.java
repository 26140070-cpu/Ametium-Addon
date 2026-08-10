package com.ametium.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    private static final Identifier AMETIUM_LOGO =
            Identifier.of("ametium-addon", "textures/gui/logo.png");

    private static final int PURPLE_BG = -10878826;
    private static final int PURPLE_ACCENT = 0xFFB266FF;

    @Shadow
    private float progress;

    @Inject(method = "render", at = @At("TAIL"))
    private void ametium$overlaySplash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        context.fill(0, 0, screenWidth, screenHeight, PURPLE_BG);

        int logoWidth = 128;
        int logoHeight = 128;
        int logoX = (screenWidth - logoWidth) / 2;
        int logoY = (screenHeight - logoHeight) / 2 - 20;

        context.drawTexture(AMETIUM_LOGO, logoX, logoY, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);

        int barWidth = 200;
        int barHeight = 4;
        int barX = (screenWidth - barWidth) / 2;
        int barY = logoY + logoHeight + 20;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF3A0060);

        int fillWidth = (int) (barWidth * MathHelper.clamp(progress, 0f, 1f));
        context.fill(barX, barY, barX + fillWidth, barY + barHeight, PURPLE_ACCENT);
    }
}