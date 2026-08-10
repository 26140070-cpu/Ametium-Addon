package com.ametium.addon.hud;

import com.ametium.addon.AmetiumAddon;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Identifier;

public class  Promotion extends HudElement {
   private static final Identifier LOGO_ID = Identifier.of("ametium-addon", "icon.png");
   public static final HudElementInfo<Promotion> INFO = new HudElementInfo(
      AmetiumAddon.HUD_GROUP, "Promotion", "Displays Ametium logo and text.", Promotion::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> showLogo = this.sgGeneral.add(((Builder)((Builder)new Builder().name("show-logo")).defaultValue(true)).build());
   private final Setting<Double> logoScale = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("logo-scale"))
               .defaultValue(2.5)
               .min(0.1)
               .sliderMax(5.0)
               .visible(() -> (Boolean)this.showLogo.get()))
            .build()
      );
   private final Setting<Boolean> showText = this.sgGeneral.add(((Builder)((Builder)new Builder().name("show-text")).defaultValue(true)).build());
   private final Setting<String> text = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("text"))
                  .defaultValue("Ametium.dev"))
               .visible(() -> (Boolean)this.showText.get()))
            .build()
      );
   private final Setting<Double> textScale = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("text-scale"))
               .defaultValue(1.0)
               .min(0.5)
               .sliderMax(3.0)
               .visible(() -> (Boolean)this.showText.get()))
            .build()
      );
   private final Setting<SettingColor> textColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("text-color"))
               .defaultValue(new SettingColor(90, 0, 150))
               .visible(() -> (Boolean)this.showText.get()))
            .build()
      );
   private final Setting<Double> spacing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("spacing"))
               .defaultValue(-20.0)
               .min(-20.0)
               .max(20.0)
               .sliderRange(-20.0, 20.0)
               .visible(() -> (Boolean)this.showLogo.get() && (Boolean)this.showText.get()))
            .build()
      );

   public Promotion() {
      super(INFO);
   }

   public void render(HudRenderer renderer) {
      boolean logo = (Boolean)this.showLogo.get();
      boolean txt = (Boolean)this.showText.get();
      double logoSize = 64.0 * (Double)this.logoScale.get();
      String content = (String)this.text.get();
      double ts = (Double)this.textScale.get();
      double textW = txt ? renderer.textWidth(content, true) * ts : 0.0;
      double textH = txt ? renderer.textHeight(true) * ts : 0.0;
      double gap = logo && txt ? (Double)this.spacing.get() : 0.0;
      double totalWidth = Math.max(logo ? logoSize : 0.0, textW);
      double totalHeight = (logo ? logoSize : 0.0) + gap + textH;
      this.box.setSize(totalWidth, totalHeight);
      double cursorY = this.y;
      if (logo) {
         double logoX = this.x + (totalWidth - logoSize) / 2.0;
         renderer.texture(LOGO_ID, logoX, cursorY, logoSize, logoSize, Color.WHITE);
         cursorY += logoSize + gap;
      }

      if (txt) {
         double textX = this.x + (totalWidth - textW) / 2.0;
         renderer.text(content, textX, cursorY, (Color)this.textColor.get(), true, ts);
      }
   }
}
