package com.ametium.addon.mixin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class ContainerButtonsMixin {
    @Unique private final List<ButtonWidget> ametiumButtons = new ArrayList<>();

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Module module = Modules.get().get("inventory-utils");
        if (module == null || !module.isActive()) return;
        Object self = this;
        boolean container = self instanceof GenericContainerScreen || self instanceof ShulkerBoxScreen;
        boolean inventory = self instanceof InventoryScreen;
        if (!container && !inventory) return;
        addButton("Save", 5, 5, () -> call(module, "executeSave", "default"));
        addButton("Rekit", 5, 25, () -> call(module, "executeLoad", "default"));
        if (container) {
            addButton("Steal", 5, 45, () -> call(module, "executeSteal"));
            addButton("Dump", 5, 65, () -> call(module, "executeDump"));
            addButton("Drop Box", 5, 85, () -> call(module, "executeDropChest"));
            addButton("Drop Inv", 5, 105, () -> call(module, "executeDropInventory"));
        }
        addButton("Drop All", 5, container ? 125 : 45, () -> call(module, "executeDropAll"));
    }

    @Unique private void addButton(String label, int x, int y, Runnable action) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> action.run()).dimensions(x, y, 70, 18).build();
        ametiumButtons.add(button);
        ((ScreenAccessor) this).invokeAddDrawableChild(button);
    }

    @Unique private static void call(Module module, String method, Object... argument) {
        try {
            Method target = argument.length == 0 ? module.getClass().getMethod(method) : module.getClass().getMethod(method, String.class);
            target.invoke(module, argument);
        } catch (ReflectiveOperationException ignored) { }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Module module = Modules.get().get("inventory-utils");
        boolean active = module != null && module.isActive();
        for (ButtonWidget button : ametiumButtons) button.active = active;
    }
}
