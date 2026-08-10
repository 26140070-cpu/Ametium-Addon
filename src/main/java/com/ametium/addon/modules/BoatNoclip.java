package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;

public class BoatNoclip extends Module {
   private final SettingGroup sgSpeed = this.settings.createGroup("Speed");
   private final SettingGroup sgControls = this.settings.createGroup("Controls");
   private final Setting<Boolean> speed = this.sgSpeed.add(((Builder)((Builder)new Builder().name("speed")).defaultValue(true)).build());
   private final Setting<Double> horizontalSpeed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("horizontal-speed"))
               .defaultValue(10.0)
               .min(0.0)
               .sliderMax(50.0)
               .visible(this.speed::get))
            .build()
      );
   private final Setting<Double> horizontalSpeedInsideBlocks = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("horizontal-speed-inside-blocks"))
               .defaultValue(5.0)
               .min(0.0)
               .sliderMax(50.0)
               .visible(this.speed::get))
            .build()
      );
   private final Setting<Double> verticalSpeed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("vertical-speed"))
            .defaultValue(6.0)
            .min(0.0)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Keybind> keyUp = this.sgControls
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("up"))
               .defaultValue(Keybind.fromKey(32)))
            .build()
      );
   private final Setting<Keybind> keyDown = this.sgControls
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("down"))
               .defaultValue(Keybind.fromKey(342)))
            .build()
      );
   private boolean insideBlock;

   public BoatNoclip() {
      super(AmetiumAddon.Ametium_Utils, "BoatNoclip", "Fly through anything using boats.\nForked from TrouserStreak and improved.");
   }

   public void onActivate() {
   }

   public void onDeactivate() {
      if (this.mc.player != null && this.mc.player.getVehicle() instanceof BoatEntity boat) {
         boat.noClip = false;
         boat.setNoGravity(false);
      }
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.mc.player != null && this.mc.player.getVehicle() instanceof BoatEntity boat) {
         this.insideBlock = this.isInsideBlock(boat);
         boat.noClip = true;
         boat.setNoGravity(true);
         double velX = 0.0;
         double velY = 0.0;
         double velZ = 0.0;
         if ((Boolean)this.speed.get()) {
            double appliedSpeed = this.insideBlock ? (Double)this.horizontalSpeedInsideBlocks.get() : (Double)this.horizontalSpeed.get();
            double moveSpeed = appliedSpeed / 20.0;
            float yaw = this.mc.player.getYaw();
            double forward = 0.0;
            double strafe = 0.0;
            if (this.mc.options.forwardKey.isPressed()) {
               forward++;
            }

            if (this.mc.options.backKey.isPressed()) {
               forward--;
            }

            if (this.mc.options.leftKey.isPressed()) {
               strafe++;
            }

            if (this.mc.options.rightKey.isPressed()) {
               strafe--;
            }

            if (forward != 0.0 || strafe != 0.0) {
               if (forward != 0.0) {
                  if (strafe > 0.0) {
                     yaw += forward > 0.0 ? -45.0F : 45.0F;
                  } else if (strafe < 0.0) {
                     yaw += forward > 0.0 ? 45.0F : -45.0F;
                  }

                  strafe = 0.0;
                  forward = forward > 0.0 ? 1.0 : -1.0;
               }

               double rad = Math.toRadians(yaw + 90.0);
               velX = forward * moveSpeed * Math.cos(rad) + strafe * moveSpeed * Math.sin(rad);
               velZ = forward * moveSpeed * Math.sin(rad) - strafe * moveSpeed * Math.cos(rad);
            }
         }

         if (((Keybind)this.keyUp.get()).isPressed() || this.mc.currentScreen == null && Input.isPressed(this.mc.options.jumpKey)) {
            velY += this.verticalSpeed.get() / 20.0;
         }

         if (((Keybind)this.keyDown.get()).isPressed()) {
            velY -= this.verticalSpeed.get() / 20.0;
         }

         float yaw = this.mc.player.getYaw();
         boat.setYaw(yaw);
         boat.setBodyYaw(yaw);
         boat.setVelocity(velX, velY, velZ);
      }
   }

   private boolean isInsideBlock(Entity entity) {
      if (entity != null && this.mc.world != null) {
         Box box = entity.getBoundingBox().contract(0.0, 0.05, 0.0).expand(0.5, 0.0, 0.5);
         int minX = MathHelper.floor(box.minX);
         int minY = MathHelper.floor(box.minY);
         int minZ = MathHelper.floor(box.minZ);
         int maxX = MathHelper.floor(box.maxX);
         int maxY = MathHelper.floor(box.maxY);
         int maxZ = MathHelper.floor(box.maxZ);
         Mutable pos = new Mutable();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  pos.set(x, y, z);
                  BlockState state = this.mc.world.getBlockState(pos);
                  if (!state.isAir()) {
                     VoxelShape shape = state.getCollisionShape(this.mc.world, pos);
                     if (!shape.isEmpty() && entity.getBoundingBox().intersects(shape.getBoundingBox().offset(pos))) {
                        return true;
                     }
                  }
               }
            }
         }

         for (Entity other : this.mc.world.getOtherEntities(entity, box)) {
            if (other != this.mc.player
               && other != entity.getVehicle()
               && !other.hasPassenger(entity)
               && !entity.hasPassenger(other)
               && other.isAlive()
               && !other.isRemoved()
               && other.getBoundingBox().intersects(box)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
