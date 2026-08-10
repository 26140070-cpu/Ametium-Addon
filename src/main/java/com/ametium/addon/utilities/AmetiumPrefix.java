package com.ametium.addon.utilities;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class AmetiumPrefix {
    private static final int AMETIUM_COLOR = 5898390;

    public static void register() {
        ChatUtils.registerCustomPrefix("com.ametium.addon", AmetiumPrefix::buildPrefix);
    }

    private static Text buildPrefix() {
        return Text.empty()
                .append(Text.literal("AMETIUM").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(5898390)).withBold(true)))
                .append(Text.literal(" » ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(8421504))));
    }
}