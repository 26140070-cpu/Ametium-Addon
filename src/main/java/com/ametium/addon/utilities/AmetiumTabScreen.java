package com.ametium.addon.utilities;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.util.Util;

public class AmetiumTabScreen extends TabScreen {
    public AmetiumTabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);
    }

    @Override
    public void initWidgets() {
        WWindow window = add(theme.window("Ametium")).centerX().widget();
        WVerticalList root = window.add(theme.verticalList()).widget();
        root.minWidth = 300.0;

        WTable table = root.add(theme.table()).expandX().widget();
        addButton(table, "Website", "https://ametium.dev");
        addButton(table, "Discord", "https://discord.gg/ametium");
    }

    private void addButton(WTable table, String name, String url) {
        WButton button = table.add(theme.button(name)).expandX().widget();
        button.action = () -> Util.getOperatingSystem().open(url);
    }
}