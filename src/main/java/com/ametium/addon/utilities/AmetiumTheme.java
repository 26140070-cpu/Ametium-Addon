package com.ametium.addon.utilities;

import java.lang.reflect.Field;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class AmetiumTheme extends MeteorGuiTheme {
    private static final SettingColor AMETIUM_PURPLE = new SettingColor(90, 0, 150);

    public AmetiumTheme() {
        try {
            Field nameField = GuiTheme.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(this, "Ametium");
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.accentColor.set(AMETIUM_PURPLE);
        this.checkboxColor.set(AMETIUM_PURPLE);
        this.moduleBackground.set(new SettingColor(60, 0, 100, 200));

        this.setDefaultForSetting(this.accentColor);
        this.setDefaultForSetting(this.checkboxColor);
        this.setSliderHandleColors();
    }

    @Override
    public TextRenderer textRenderer() {
        return TextRenderer.get();
    }

    private void setSliderHandleColors() {
        try {
            Field normalField = this.sliderHandle.getClass().getDeclaredField("normal");
            Field hoveredField = this.sliderHandle.getClass().getDeclaredField("hovered");
            Field pressedField = this.sliderHandle.getClass().getDeclaredField("pressed");

            normalField.setAccessible(true);
            hoveredField.setAccessible(true);
            pressedField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Setting<SettingColor> normal = (Setting<SettingColor>) normalField.get(this.sliderHandle);
            @SuppressWarnings("unchecked")
            Setting<SettingColor> hovered = (Setting<SettingColor>) hoveredField.get(this.sliderHandle);
            @SuppressWarnings("unchecked")
            Setting<SettingColor> pressed = (Setting<SettingColor>) pressedField.get(this.sliderHandle);

            normal.set(AMETIUM_PURPLE);
            hovered.set(AMETIUM_PURPLE);
            pressed.set(AMETIUM_PURPLE);

            this.setDefaultForSetting(normal);
            this.setDefaultForSetting(hovered);
            this.setDefaultForSetting(pressed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDefaultForSetting(Setting<SettingColor> setting) {
        try {
            Field defaultValueField = Setting.class.getDeclaredField("defaultValue");
            defaultValueField.setAccessible(true);
            defaultValueField.set(setting, AMETIUM_PURPLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}