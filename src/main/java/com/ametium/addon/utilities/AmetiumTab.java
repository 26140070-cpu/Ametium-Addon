package com.ametium.addon.utilities;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screen.Screen;

public class AmetiumTab extends Tab {
    public AmetiumTab() {
        super("Ametium");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new AmetiumTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof AmetiumTabScreen;
    }
}