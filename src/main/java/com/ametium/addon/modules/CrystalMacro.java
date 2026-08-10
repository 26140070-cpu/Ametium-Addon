package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;

public class CrystalMacro extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Integer> breakDelay = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("Break Delay")).defaultValue(0)).min(0).sliderMax(10).build());
   private final Setting<Boolean> renderPlace = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("render-place"))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> placeRenderMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("place-mode"))
                  .defaultValue(ShapeMode.Sides))
               .visible(this.renderPlace::get))
            .build()
      );
   private final Setting<SettingColor> placeFillColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("place-fill"))
               .defaultValue(new SettingColor(90, 0, 150, 50))
               .visible(this.renderPlace::get))
            .build()
      );
   private final Setting<SettingColor> placeOutlineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("place-outline"))
               .defaultValue(new SettingColor(90, 0, 150, 255))
               .visible(this.renderPlace::get))
            .build()
      );
   private int placeTimer;
   private int breakTimer;
   private BlockPos bestBase;
   private Entity target;

   public CrystalMacro() {
      super(AmetiumAddon.Ametium_PvP, "CrystalMacro", "Legit AutoCrystal.");
   }

   public void onActivate() {
      this.placeTimer = 0;
      this.breakTimer = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.crosshairTarget != null) {
         if (this.placeTimer > 0) {
            this.placeTimer--;
         }

         if (this.breakTimer > 0) {
            this.breakTimer--;
         }

         this.bestBase = null;
         this.target = null;
         if (this.mc.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EndCrystalEntity crystal) {
            this.target = crystal;
            if (this.breakTimer <= 0) {
               this.attackCrystal(crystal);
               this.breakTimer = (Integer)this.breakDelay.get();
            }
         } else if (this.mc.crosshairTarget instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            if ((this.mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || this.mc.world.getBlockState(pos).isOf(Blocks.BEDROCK))
               && this.mc.options.useKey.isPressed()
               && this.mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)
               && this.placeTimer <= 0) {
               this.bestBase = pos;
               this.placeCrystal(blockHit);
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent e) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.renderPlace.get() && this.bestBase != null) {
            e.renderer.box(this.bestBase, (Color)this.placeFillColor.get(), (Color)this.placeOutlineColor.get(), (ShapeMode)this.placeRenderMode.get(), 0);
         }
      }
   }

   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private void placeCrystal(BlockHitResult hit) {
      this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hit);
      this.mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void attackCrystal(Entity entity) {
      this.mc.interactionManager.attackEntity(this.mc.player, entity);
      this.mc.player.swingHand(Hand.MAIN_HAND);
   }
}
