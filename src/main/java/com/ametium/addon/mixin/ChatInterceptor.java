package com.ametium.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatInterceptor {
    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        Module module = Modules.get().get("chat-cleaner");
        if (module == null || !module.isActive()) return;
        Text message = packet.content();
        try {
            boolean hide = (boolean) module.getClass().getMethod("shouldHideMessage", String.class).invoke(module, message.getString());
            if (hide) ci.cancel();
        } catch (ReflectiveOperationException ignored) { }
    }
}
