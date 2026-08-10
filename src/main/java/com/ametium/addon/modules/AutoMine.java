package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoMine extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSurround = this.settings.createGroup("Break Surrounds");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> speedMultiplier = this.sgGeneral
      .add(((Builder)new Builder().name("Speed")).defaultValue(1.0).min(0.001).sliderMax(1.5).build());
   private final Setting<AutoMine.AutoSwitch> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Auto Switch"))
               .defaultValue(AutoMine.AutoSwitch.Silent))
            .build()
      );
   private final Setting<Boolean> instaRebreak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Instant Rebreak"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> multiMine = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Multiple Mine"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> particles = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Particles"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> maxRange = this.sgGeneral.add(((Builder)new Builder().name("Max Range")).defaultValue(6.0).min(1.0).sliderMax(6.0).build());
   private final Setting<Integer> maxBlocks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Max Blocks"))
                  .defaultValue(5))
               .min(2)
               .sliderMax(20)
               .visible(this.multiMine::get))
            .build()
      );
   private final Setting<Boolean> breakSurround = this.sgSurround
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Break Surrounds"))
               .defaultValue(false))
            .build()
      );
   private final Setting<AutoMine.SurroundMode> surroundMode = this.sgSurround
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Surround Mode"))
                  .defaultValue(AutoMine.SurroundMode.Normal))
               .visible(this.breakSurround::get))
            .build()
      );
   private final Setting<Double> enemyRange = this.sgSurround
      .add(((Builder)((Builder)new Builder().name("Enemy Range")).defaultValue(6.0).min(1.0).sliderMax(6.0).visible(this.breakSurround::get)).build());
   private final Setting<Boolean> antiSelfSurround = this.sgSurround
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Anti Break Self"))
                  .defaultValue(true))
               .visible(this.breakSurround::get))
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgSurround
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Ignore Friends"))
                  .defaultValue(true))
               .visible(this.breakSurround::get))
            .build()
      );
   private final Setting<Boolean> ignoreBots = this.sgSurround
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Ignore-Bots"))
                  .defaultValue(true))
               .visible(this.breakSurround::get))
            .build()
      );
   private final Setting<Boolean> antiUnbreakable = this.sgSurround
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Anti-Bedrock"))
                  .defaultValue(true))
               .visible(this.breakSurround::get))
            .build()
      );
   private final Setting<Double> rebreakDelay = this.sgSurround
      .add(
         ((Builder)((Builder)new Builder().name("Rebreak Delay"))
               .defaultValue(1.0)
               .min(0.1)
               .sliderMax(5.0)
               .visible(() -> (Boolean)this.breakSurround.get() && (Boolean)this.instaRebreak.get()))
            .build()
      );
   private final Setting<SettingColor> lineStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("line-start-color"))
            .defaultValue(new SettingColor(255, 0, 0, 50))
            .build()
      );
   private final Setting<SettingColor> fillStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("fill-start-color"))
            .defaultValue(new SettingColor(255, 0, 0, 25))
            .build()
      );
   private final Setting<SettingColor> lineEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("line-end-color"))
            .defaultValue(new SettingColor(0, 255, 0, 50))
            .build()
      );
   private final Setting<SettingColor> fillEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("fill-end-color"))
            .defaultValue(new SettingColor(0, 255, 0, 25))
            .build()
      );
   private final Setting<SettingColor> queuedLineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("queued-line-color"))
               .defaultValue(new SettingColor(255, 0, 0, 50))
               .visible(this.multiMine::get))
            .build()
      );
   private final Setting<SettingColor> queuedFillColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("queued-fill-color"))
               .defaultValue(new SettingColor(255, 0, 0, 25))
               .visible(this.multiMine::get))
            .build()
      );
   private final Setting<AutoMine.SwingMode> swingMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("swing-mode"))
               .defaultValue(AutoMine.SwingMode.Client))
            .build()
      );
   private final List<AutoMine.MineEntry> queue = new ArrayList<>();
   private AutoMine.MineEntry current = null;
   private boolean waitingRebreak = false;
   private int rebreakTimer = 0;

   public AutoMine() {
      super(AmetiumAddon.Ametium_PvP, "AutoMine", "High-Speed PacketMine.");
   }

   public void onDeactivate() {
      this.current = null;
      this.queue.clear();
      this.waitingRebreak = false;
      this.rebreakTimer = 0;
   }

   private void doSwing(Hand hand) {
      switch ((AutoMine.SwingMode)this.swingMode.get()) {
         case Packet:
            this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            break;
         case Client:
            this.mc.player.swingHand(hand);
         case None:
      }
   }

   @EventHandler
   private void onStartBreaking(StartBreakingBlockEvent event) {
      event.cancel();
      BlockPos pos = event.blockPos;
      Direction dir = event.direction;
      if (!this.isUnbreakable(this.mc.world.getBlockState(pos), pos)) {
         if (this.current == null || !this.current.pos.equals(pos)) {
            for (AutoMine.MineEntry e : this.queue) {
               if (e.pos.equals(pos)) {
                  return;
               }
            }

            if (!(Boolean)this.multiMine.get()) {
               this.current = new AutoMine.MineEntry(pos, dir);
               this.queue.clear();
               this.waitingRebreak = false;
               this.rebreakTimer = 0;
               this.sendStart(this.current);
            } else {
               if (this.current == null) {
                  this.current = new AutoMine.MineEntry(pos, dir);
                  this.waitingRebreak = false;
                  this.rebreakTimer = 0;
                  this.sendStart(this.current);
               } else if (this.queue.size() < (Integer)this.maxBlocks.get() - 1) {
                  this.queue.add(new AutoMine.MineEntry(pos, dir));
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.world != null && this.mc.player != null) {
         if (this.current == null) {
            if ((Boolean)this.breakSurround.get()) {
               PlayerEntity target = this.getClosestEnemy();
               if (target != null) {
                  BlockPos targetSurround = this.getTargetSurroundBlock(target);
                  if (targetSurround != null) {
                     BlockState surroundState = this.mc.world.getBlockState(targetSurround);
                     if (!surroundState.isAir() && !this.isUnbreakable(surroundState, targetSurround)) {
                        this.current = new AutoMine.MineEntry(targetSurround, Direction.UP, true);
                        this.waitingRebreak = false;
                        this.rebreakTimer = 0;
                        this.sendStart(this.current);
                     }
                  }
               }
            }

            if (this.current == null) {
               return;
            }
         }

         if (!this.current.pos.isWithinDistance(this.mc.player.getBlockPos(), (Double)this.maxRange.get())) {
            this.cancelAll();
         } else {
            BlockState state = this.mc.world.getBlockState(this.current.pos);
            if (this.isUnbreakable(state, this.current.pos)) {
               this.promoteNext();
            } else if (this.isAirOrFluid(state)) {
               if (!(Boolean)this.instaRebreak.get() || !this.queue.isEmpty()) {
                  this.promoteNext();
               } else if (!this.waitingRebreak) {
                  this.waitingRebreak = true;
                  this.rebreakTimer = 0;
               } else if (this.current.isSurround && (Boolean)this.breakSurround.get()) {
                  this.rebreakTimer++;
                  if (this.rebreakTimer >= (int)((Double)this.rebreakDelay.get() * 20.0)) {
                     this.current = null;
                     this.waitingRebreak = false;
                     this.rebreakTimer = 0;
                  }
               }
            } else if (this.waitingRebreak) {
               this.waitingRebreak = false;
               this.rebreakTimer = 0;
               this.sendStop(this.current, state);
            } else {
               this.current.progress = this.current.progress + this.getMiningSpeed(state, this.current.pos);
               if (this.current.progress >= 1.0) {
                  this.sendStop(this.current, state);
                  if (!(Boolean)this.instaRebreak.get() || !this.queue.isEmpty()) {
                     this.promoteNext();
                  }
               }
            }
         }
      }
   }

   private PlayerEntity getClosestEnemy() {
      PlayerEntity closest = null;
      double closestDist = (Double)this.enemyRange.get();

      for (PlayerEntity player : this.mc.world.getPlayers()) {
         if (player != this.mc.player
            && player.isAlive()
            && (!(Boolean)this.ignoreFriends.get() || !Friends.get().isFriend(player))
            && (!(Boolean)this.ignoreBots.get() || this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null)) {
            double dist = this.mc.player.distanceTo(player);
            if (dist < closestDist) {
               closestDist = dist;
               closest = player;
            }
         }
      }

      return closest;
   }

   private BlockPos getTargetSurroundBlock(PlayerEntity target) {
      BlockPos targetFeet = target.getBlockPos();
      BlockPos targetHead = targetFeet.up();
      BlockPos myFeet = this.mc.player.getBlockPos();
      BlockPos myHead = myFeet.up();
      List<BlockPos> positions = new ArrayList<>();
      positions.add(targetFeet.north());
      positions.add(targetFeet.south());
      positions.add(targetFeet.east());
      positions.add(targetFeet.west());
      if (this.surroundMode.get() == AutoMine.SurroundMode.Double) {
         positions.add(targetHead.north());
         positions.add(targetHead.south());
         positions.add(targetHead.east());
         positions.add(targetHead.west());
      }

      for (BlockPos pos : positions) {
         BlockState state = this.mc.world.getBlockState(pos);
         if (!this.isAirOrFluid(state)
            && (
               !(Boolean)this.antiUnbreakable.get()
                  || !state.isOf(Blocks.BEDROCK) && !state.isOf(Blocks.END_PORTAL_FRAME) && !(state.getHardness(this.mc.world, pos) <= 0.0F)
            )
            && (
               !(Boolean)this.antiSelfSurround.get()
                  || !pos.equals(myFeet.north())
                     && !pos.equals(myFeet.south())
                     && !pos.equals(myFeet.east())
                     && !pos.equals(myFeet.west())
                     && !pos.equals(myHead.north())
                     && !pos.equals(myHead.south())
                     && !pos.equals(myHead.east())
                     && !pos.equals(myHead.west())
            )) {
            return pos;
         }
      }

      return null;
   }

   private boolean isFishtrapped(BlockPos ceiling) {
      if (this.mc.world.getBlockState(ceiling).isAir()) {
         return false;
      }

      BlockPos feet = this.mc.player.getBlockPos();
      BlockPos[] around = new BlockPos[]{feet.north(), feet.south(), feet.east(), feet.west()};

      for (BlockPos pos : around) {
         if (this.mc.world.getBlockState(pos).isAir()) {
            return false;
         }
      }

      return true;
   }

   private void promoteNext() {
      this.waitingRebreak = false;
      this.rebreakTimer = 0;

      while (!this.queue.isEmpty()) {
         AutoMine.MineEntry next = this.queue.remove(0);
         if (!this.isUnbreakable(this.mc.world.getBlockState(next.pos), next.pos)) {
            this.current = next;
            this.current.progress = 0.0;
            this.sendStart(this.current);
            return;
         }
      }

      this.current = null;
   }

   private boolean isUnbreakable(BlockState state, BlockPos pos) {
      return state.getHardness(this.mc.world, pos) < 0.0F;
   }

   private boolean isAirOrFluid(BlockState state) {
      return state.isAir() || !state.getFluidState().isEmpty();
   }

   private void cancelAll() {
      if (this.current != null) {
         this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.current.pos, this.current.dir));
      }

      this.current = null;
      this.queue.clear();
      this.waitingRebreak = false;
      this.rebreakTimer = 0;
   }

   private void sendStart(AutoMine.MineEntry entry) {
      this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, entry.pos, entry.dir));
      this.doSwing(Hand.MAIN_HAND);
   }

   private void sendStop(AutoMine.MineEntry entry, BlockState state) {
      if ((Boolean)this.particles.get()) {
         this.mc.world.addBlockBreakParticles(entry.pos, state);
      }

      int bestSlot = this.findBestToolSlot(state);
      int prevSlot = this.mc.player.getInventory().selectedSlot;
      if (bestSlot != -1 && bestSlot != prevSlot) {
         if (this.autoSwitch.get() == AutoMine.AutoSwitch.Silent) {
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
            this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, entry.pos, entry.dir));
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
         } else {
            this.mc.player.getInventory().selectedSlot = bestSlot;
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
            this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, entry.pos, entry.dir));
         }
      } else {
         this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, entry.pos, entry.dir));
      }

      this.doSwing(Hand.MAIN_HAND);
   }

   private int getEfficiencyLevel(ItemStack stack) {
      ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(stack);

      for (RegistryEntry<Enchantment> e : enchants.getEnchantments()) {
         if (e.matchesKey(Enchantments.EFFICIENCY)) {
            return enchants.getLevel(e);
         }
      }

      return 0;
   }

   private int findBestToolSlot(BlockState state) {
      int bestSlot = -1;
      float bestSpeed = 1.0F;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = this.mc.player.getInventory().getStack(slot);
         float speed = stack.getMiningSpeedMultiplier(state);
         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = slot;
         }
      }

      return bestSlot;
   }

   private double getMiningSpeed(BlockState state, BlockPos pos) {
      float hardness = state.getHardness(this.mc.world, pos);
      if (hardness < 0.0F) {
         return 0.0;
      }

      if (hardness == 0.0F) {
         return 1.0;
      }

      int toolSlot = this.findBestToolSlot(state);
      boolean canHarvest = !state.isToolRequired();
      float miningSpeedMultiplier = 1.0F;
      if (toolSlot != -1) {
         ItemStack toolStack = this.mc.player.getInventory().getStack(toolSlot);
         miningSpeedMultiplier = toolStack.getMiningSpeedMultiplier(state);
         canHarvest = true;
         int efficiencyLevel = this.getEfficiencyLevel(toolStack);
         if (efficiencyLevel > 0) {
            miningSpeedMultiplier += efficiencyLevel * efficiencyLevel + 1;
         }
      }

      float damage = miningSpeedMultiplier / hardness / (canHarvest ? 30.0F : 100.0F);
      if (this.mc.player.isSubmergedIn(FluidTags.WATER)) {
         damage /= 5.0F;
      }

      if (!this.mc.player.isOnGround()) {
         damage /= 5.0F;
      }

      return damage * (Double)this.speedMultiplier.get();
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.current != null && !this.waitingRebreak) {
         this.renderEntry(event, this.current, false);
      }

      if ((Boolean)this.multiMine.get()) {
         for (AutoMine.MineEntry entry : this.queue) {
            this.renderEntry(event, entry, true);
         }
      }
   }

   private void renderEntry(Render3DEvent event, AutoMine.MineEntry entry, boolean isQueued) {
      if (isQueued) {
         SettingColor lc = (SettingColor)this.queuedLineColor.get();
         SettingColor fc = (SettingColor)this.queuedFillColor.get();
         event.renderer
            .box(
               entry.pos.getX(),
               entry.pos.getY(),
               entry.pos.getZ(),
               entry.pos.getX() + 1.0,
               entry.pos.getY() + 1.0,
               entry.pos.getZ() + 1.0,
               new Color(fc.r, fc.g, fc.b, fc.a),
               new Color(lc.r, lc.g, lc.b, lc.a),
               ShapeMode.Both,
               0
            );
      } else {
         double size = Math.min(entry.progress, 1.0);
         double shrink = (1.0 - size) / 2.0;
         SettingColor ls = (SettingColor)this.lineStartColor.get();
         SettingColor le = (SettingColor)this.lineEndColor.get();
         SettingColor fs = (SettingColor)this.fillStartColor.get();
         SettingColor fe = (SettingColor)this.fillEndColor.get();
         Color lineC = new Color(
            (int)(ls.r + (le.r - ls.r) * size), (int)(ls.g + (le.g - ls.g) * size), (int)(ls.b + (le.b - ls.b) * size), (int)(ls.a + (le.a - ls.a) * size)
         );
         Color fillC = new Color(
            (int)(fs.r + (fe.r - fs.r) * size), (int)(fs.g + (fe.g - fs.g) * size), (int)(fs.b + (fe.b - fs.b) * size), (int)(fs.a + (fe.a - fs.a) * size)
         );
         event.renderer
            .box(
               entry.pos.getX() + shrink,
               entry.pos.getY() + shrink,
               entry.pos.getZ() + shrink,
               entry.pos.getX() + 1.0 - shrink,
               entry.pos.getY() + 1.0 - shrink,
               entry.pos.getZ() + 1.0 - shrink,
               fillC,
               lineC,
               ShapeMode.Both,
               0
            );
      }
   }

   public enum AutoSwitch {
      Normal,
      Silent;
   }

   private static class MineEntry {
      BlockPos pos;
      Direction dir;
      double progress;
      boolean isSurround;

      MineEntry(BlockPos pos, Direction dir) {
         this(pos, dir, false);
      }

      MineEntry(BlockPos pos, Direction dir, boolean isSurround) {
         this.pos = pos;
         this.dir = dir;
         this.progress = 0.0;
         this.isSurround = isSurround;
      }
   }

   public enum SurroundMode {
      Normal,
      Double;
   }

   public enum SwingMode {
      Packet,
      Client,
      None;
   }
}
