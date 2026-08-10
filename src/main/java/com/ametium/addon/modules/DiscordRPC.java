package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;

public class DiscordRPC extends Module {
   private final SettingGroup sgLine1 = this.settings.createGroup("Line 1");
   private final SettingGroup sgLine2 = this.settings.createGroup("Line 2");
   private final Setting<List<String>> line1Strings = this.sgLine1
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("messages")).defaultValue(List.of("discord.gg/ametium"))).onChanged(strings -> this.recompileLine1()))
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   private final Setting<Integer> line1UpdateDelay = this.sgLine1
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("update-delay"))
               .defaultValue(20))
            .min(0)
            .sliderRange(0, 200)
            .build()
      );
   private final Setting<List<String>> line2Strings = this.sgLine2
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("messages")).defaultValue(List.of("ametium.dev"))).onChanged(strings -> this.recompileLine2()))
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   private final Setting<Integer> line2UpdateDelay = this.sgLine2
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("update-delay"))
               .defaultValue(20))
            .min(0)
            .sliderRange(0, 200)
            .build()
      );
   private static final RichPresence rpc = new RichPresence();
   private int line1Ticks;
   private int line2Ticks;
   private int line1I;
   private int line2I;
   private boolean forceUpdate;
   private final List<Script> line1Scripts = new ArrayList<>();
   private final List<Script> line2Scripts = new ArrayList<>();

   public DiscordRPC() {
      super(AmetiumAddon.Ametium, "DiscordRPC", "Discord Presence for Ametium.");
      this.runInMainMenu = true;
   }

   public void onActivate() {
      DiscordIPC.start(1517827233052360834L, null);
      rpc.setStart(System.currentTimeMillis() / 1000L);
      rpc.setLargeImage("logo", "Ametium.dev");
      this.recompileLine1();
      this.recompileLine2();
      this.line1Ticks = 0;
      this.line2Ticks = 0;
      this.line1I = 0;
      this.line2I = 0;
   }

   public void onDeactivate() {
      DiscordIPC.stop();
   }

   private void recompile(List<String> messages, List<Script> scripts) {
      scripts.clear();

      for (String message : messages) {
         Script script = MeteorStarscript.compile(message);
         if (script != null) {
            scripts.add(script);
         }
      }

      this.forceUpdate = true;
   }

   private void recompileLine1() {
      this.recompile((List<String>)this.line1Strings.get(), this.line1Scripts);
   }

   private void recompileLine2() {
      this.recompile((List<String>)this.line2Strings.get(), this.line2Scripts);
   }

   @EventHandler
   private void onTick(Post event) {
      boolean update = false;
      if (this.line1Ticks < (Integer)this.line1UpdateDelay.get() && !this.forceUpdate) {
         this.line1Ticks++;
      } else {
         if (!this.line1Scripts.isEmpty()) {
            if (this.line1I >= this.line1Scripts.size()) {
               this.line1I = 0;
            }

            String message = MeteorStarscript.run(this.line1Scripts.get(this.line1I++));
            if (message != null) {
               rpc.setDetails(message);
            }
         }

         update = true;
         this.line1Ticks = 0;
      }

      if (this.line2Ticks < (Integer)this.line2UpdateDelay.get() && !this.forceUpdate) {
         this.line2Ticks++;
      } else {
         if (!this.line2Scripts.isEmpty()) {
            if (this.line2I >= this.line2Scripts.size()) {
               this.line2I = 0;
            }

            String message = MeteorStarscript.run(this.line2Scripts.get(this.line2I++));
            if (message != null) {
               rpc.setState(message);
            }
         }

         update = true;
         this.line2Ticks = 0;
      }

      if (update) {
         DiscordIPC.setActivity(rpc);
         this.forceUpdate = false;
      }
   }
}
