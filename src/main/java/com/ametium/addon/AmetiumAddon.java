package com.ametium.addon;

import com.ametium.addon.modules.*;
import com.ametium.addon.hud.CombatInformation;
import com.ametium.addon.hud.Promotion;
import com.ametium.addon.utilities.AmetiumPrefix;
import com.ametium.addon.utilities.AmetiumTheme;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmetiumAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(AmetiumAddon.class);
    public static final Category Ametium = new Category("Ametium", new ItemStack(Items.AMETHYST_SHARD));
    public static final Category Ametium_PvP = new Category("Ametium/PvP");
    public static final Category Ametium_Utils = new Category("Ametium/Utils");
    public static final HudGroup HUD_GROUP = new HudGroup("Ametium");

    public void onInitialize() {
        AmetiumPrefix.register();
        GuiThemes.add(new AmetiumTheme());
        LOG.info("Initializing Ametium Addon");
        Hud.get().register(CombatInformation.INFO);
        Hud.get().register(Promotion.INFO);
        Modules.get().add(new AimAssist());
        Modules.get().add(new AnchorTP());
        Modules.get().add(new AutoCraft());
        Modules.get().add(new AutoEnchant());
        Modules.get().add(new AutoGrind());
        Modules.get().add(new DiscordRPC());
        Modules.get().add(new BoatNoclip());
        Modules.get().add(new CrystalMacro());
        Modules.get().add(new AutoMine());
        Modules.get().add(new FrameDupe());
    }

    public void onRegisterCategories() {
        Modules.registerCategory(Ametium);
        Modules.registerCategory(Ametium_PvP);
        Modules.registerCategory(Ametium_Utils);
    }

    public String getPackage() {
        return "com.ametium.addon";
    }
}
