package com.ametium.addon.hud;

import com.ametium.addon.AmetiumAddon;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundEvents;

public class CombatInformation extends HudElement {
   public static final HudElementInfo<CombatInformation> INFO = new HudElementInfo(
      AmetiumAddon.HUD_GROUP, "Combat Information", "Displays combat logs.", CombatInformation::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> scale = this.sgGeneral.add(((Builder)new Builder().name("scale")).defaultValue(1.0).min(0.5).sliderMax(3.0).build());
   private final Setting<Double> lineSpacing = this.sgGeneral
      .add(((Builder)new Builder().name("line-spacing")).defaultValue(2.0).min(0.0).sliderMax(10.0).build());
   private final Setting<Boolean> shadow = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("text-shadow"))
               .defaultValue(true))
            .build()
      );
   private final SettingGroup sgCombat = this.settings.createGroup("Combat Log");
   private final Setting<Integer> maxLogs = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("max-logs"))
               .defaultValue(8))
            .min(1)
            .sliderMax(30)
            .build()
      );
   private final Setting<Integer> logLifetime = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("log-lifetime"))
               .defaultValue(10))
            .min(0)
            .sliderMax(60)
            .build()
      );
   private final Setting<Boolean> onlyInvolvingMe = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("only-involving-me"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> maxLogRange = this.sgCombat
      .add(((Builder)new Builder().name("max-range")).defaultValue(0.0).min(0.0).sliderMax(200.0).build());
   private final Setting<Boolean> logHits = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("log-hits"))
               .defaultValue(true))
            .build()
      );
   private final SettingGroup sgSound = this.settings.createGroup("Sound");
   private final Setting<Boolean> totemSound = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("totem-pop-sound"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> soundRange = this.sgSound
      .add(((Builder)((Builder)new Builder().name("sound-range")).defaultValue(30.0).min(0.0).sliderMax(200.0).visible(this.totemSound::get)).build());
   private final SettingGroup sgColors = this.settings.createGroup("Colors");
   private final Setting<SettingColor> titleColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("title-color"))
            .defaultValue(new SettingColor(90, 0, 150))
            .build()
      );
   private final Setting<SettingColor> totemColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("totem-pop-color"))
            .defaultValue(new SettingColor(255, 200, 0))
            .build()
      );
   private final Setting<SettingColor> killColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("kill-color"))
            .defaultValue(new SettingColor(255, 70, 70))
            .build()
      );
   private final Setting<SettingColor> hitColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("hit-color"))
            .defaultValue(new SettingColor(255, 165, 60))
            .build()
      );
   private final Setting<SettingColor> logNameColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("log-name-color"))
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> selfColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("self-highlight-color"))
            .defaultValue(new SettingColor(90, 180, 255))
            .build()
      );
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Boolean> background = this.sgBackground
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("background"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("background-color"))
               .defaultValue(new SettingColor(0, 0, 0, 100))
               .visible(this.background::get))
            .build()
      );
   private final Setting<Double> padding = this.sgBackground
      .add(((Builder)((Builder)new Builder().name("padding")).defaultValue(4.0).min(0.0).sliderMax(20.0).visible(this.background::get)).build());
   private final List<CombatInformation.LogEntry> logs = new CopyOnWriteArrayList<>();

   public CombatInformation() {
      super(INFO);
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (MeteorClient.mc.world != null && MeteorClient.mc.player != null) {
         if (event.packet instanceof EntityStatusS2CPacket p) {
            if (p.getEntity(MeteorClient.mc.world) instanceof PlayerEntity player) {
               if (this.passesLogFilter(player)) {
                  if (p.getStatus() == 35) {
                     this.addLog(player.getName().getString(), "popped a totem", "totem", (Color)this.totemColor.get(), player == MeteorClient.mc.player);
                     if ((Boolean)this.totemSound.get() && MeteorClient.mc.player.distanceTo(player) <= (Double)this.soundRange.get()) {
                        MeteorClient.mc.execute(() -> MeteorClient.mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_TOTEM_USE, 1.0F)));
                     }
                  } else if (p.getStatus() == 3) {
                     this.addLog(player.getName().getString(), "was killed", "kill", (Color)this.killColor.get(), player == MeteorClient.mc.player);
                  }
               }
            }
         } else {
            if (event.packet instanceof EntityDamageS2CPacket p) {
               if (!(Boolean)this.logHits.get()) {
                  return;
               }

               Entity attacker = MeteorClient.mc.world.getEntityById(p.sourceCauseId());
               Entity target = MeteorClient.mc.world.getEntityById(p.entityId());
               if (!(attacker instanceof PlayerEntity attackerPlayer)) {
                  return;
               }

               if (!(target instanceof PlayerEntity targetPlayer)) {
                  return;
               }

               if (attackerPlayer == targetPlayer) {
                  return;
               }

               if (!this.passesHitFilter(attackerPlayer, targetPlayer)) {
                  return;
               }

               String attackerName = attackerPlayer.getName().getString();
               String targetName = targetPlayer.getName().getString();
               int targetHp = (int)targetPlayer.getHealth();
               String action = "hitted " + targetName + " [" + targetHp + "hp]";
               String mergeKey = "hit:" + targetName;
               boolean involvesMe = attackerPlayer == MeteorClient.mc.player || targetPlayer == MeteorClient.mc.player;
               this.addLog(attackerName, action, mergeKey, (Color)this.hitColor.get(), involvesMe);
            }
         }
      }
   }

   private boolean passesLogFilter(PlayerEntity player) {
      if (MeteorClient.mc.player == null) {
         return false;
      } else {
         return this.onlyInvolvingMe.get() && player != MeteorClient.mc.player
            ? false
            : !((Double)this.maxLogRange.get() > 0.0) || !(MeteorClient.mc.player.distanceTo(player) > (Double)this.maxLogRange.get());
      }
   }

   private boolean passesHitFilter(PlayerEntity attacker, PlayerEntity target) {
      if (MeteorClient.mc.player == null) {
         return false;
      }

      if ((Boolean)this.onlyInvolvingMe.get()) {
         boolean involvesMe = attacker == MeteorClient.mc.player || target == MeteorClient.mc.player;
         if (!involvesMe) {
            return false;
         }
      }

      return !((Double)this.maxLogRange.get() > 0.0) || !(MeteorClient.mc.player.distanceTo(target) > (Double)this.maxLogRange.get());
   }

   private void addLog(String victim, String action, String mergeKey, Color color, boolean involvesMe) {
      int count = 1;

      for (CombatInformation.LogEntry entry : this.logs) {
         if (entry.victim.equals(victim) && entry.mergeKey.equals(mergeKey)) {
            count = entry.count + 1;
            this.logs.remove(entry);
            break;
         }
      }

      this.logs.add(0, new CombatInformation.LogEntry(victim, action, mergeKey, color, involvesMe, count, System.currentTimeMillis()));
      if (this.logs.size() > (Integer)this.maxLogs.get()) {
         this.logs.remove(this.logs.size() - 1);
      }
   }

   public void render(HudRenderer renderer) {
      if ((Integer)this.logLifetime.get() > 0) {
         long now = System.currentTimeMillis();
         this.logs.removeIf(entryx -> now - entryx.timestamp > ((Integer)this.logLifetime.get()).intValue() * 1000L);
      }

      double s = (Double)this.scale.get();
      double lineHeight = (renderer.textHeight(true) + (Double)this.lineSpacing.get()) * s;
      double pad = this.background.get() ? (Double)this.padding.get() : 0.0;
      double baseX = this.x + pad;
      double maxWidth = renderer.textWidth("Ametium Combat Information", true) * s;
      List<CombatInformation.LogEntry> currentLogs = new ArrayList<>(this.logs);
      int logCount = Math.min(currentLogs.size(), (Integer)this.maxLogs.get());
      int logSectionLines = 1 + logCount;

      for (int i = 0; i < logCount; i++) {
         double w = this.measureLogLineWidth(renderer, currentLogs.get(i), s);
         if (w > maxWidth) {
            maxWidth = w;
         }
      }

      double totalHeight = lineHeight * logSectionLines + pad * 2.0;
      if ((Boolean)this.background.get()) {
         renderer.quad(this.x, this.y, maxWidth + pad * 2.0, totalHeight, (Color)this.backgroundColor.get());
      }

      double curY = this.y + pad;
      renderer.text("Ametium Combat Information", baseX, curY, (Color)this.titleColor.get(), (Boolean)this.shadow.get(), s);
      curY += lineHeight;

      for (int i = 0; i < logCount; i++) {
         CombatInformation.LogEntry entry = currentLogs.get(i);
         Color nameCol = entry.involvesMe ? (Color)this.selfColor.get() : (Color)this.logNameColor.get();
         double curX = baseX;
         curX = this.drawText(renderer, entry.victim + " ", curX, curY, nameCol, s);
         String actionText = entry.action + (entry.count > 1 ? " x" + entry.count : "");
         this.drawText(renderer, actionText, curX, curY, entry.color, s);
         curY += lineHeight;
      }

      this.box.setSize(maxWidth + pad * 2.0, totalHeight);
   }

   private double drawText(HudRenderer renderer, String text, double curX, double curY, Color color, double s) {
      renderer.text(text, curX, curY, color, (Boolean)this.shadow.get(), s);
      return curX + renderer.textWidth(text, true) * s;
   }

   private double measureLogLineWidth(HudRenderer renderer, CombatInformation.LogEntry entry, double s) {
      String actionText = entry.action + (entry.count > 1 ? " x" + entry.count : "");
      return renderer.textWidth(entry.victim + " " + actionText, true) * s;
   }

   private record LogEntry(String victim, String action, String mergeKey, Color color, boolean involvesMe, int count, long timestamp) {
   }
}
