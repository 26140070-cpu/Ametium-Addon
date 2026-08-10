package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class AnchorTP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTargets = this.settings.createGroup("Targets");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private static final long manualCooldown = 1L;
   private final Setting<Double> range = this.sgGeneral.add(((Builder)new Builder().name("range")).defaultValue(500.0).min(0.0).sliderMax(500.0).build());
   private final Setting<Double> maxStep = this.sgGeneral.add(((Builder)new Builder().name("max-step")).defaultValue(200.0).min(1.0).sliderMax(200.0).build());
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("attack-delay"))
               .defaultValue(0))
            .min(0)
            .sliderMax(20)
            .build()
      );
   private final Setting<Boolean> padding = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("padding-packets"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> pauseOnEat = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("pause-on-eat"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoRefill = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("auto-refill"))
               .defaultValue(true))
            .build()
      );
   private final Setting<AnchorTP.AttackMode> attackMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("attack-mode"))
               .defaultValue(AnchorTP.AttackMode.Automatic))
            .build()
      );
   private final Setting<Keybind> manualAttackKey = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("manual-key"))
                  .defaultValue(Keybind.none()))
               .visible(() -> this.attackMode.get() == AnchorTP.AttackMode.ManualKeybind))
            .build()
      );
   private long lastManualAttack = 0L;
   private final Setting<Set<EntityType<?>>> entities = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
               .name("entities"))
            .defaultValue(new EntityType[]{EntityType.PLAYER})
            .build()
      );
   private final Setting<AnchorTP.SortPriority> sortPriority = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("priority"))
               .defaultValue(AnchorTP.SortPriority.Closest))
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("ignore-friends"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreBots = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("ignore-bots"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreCreative = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("ignore-creative"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> minDamage = this.sgTargets.add(((Builder)new Builder().name("min-damage")).defaultValue(4.0).min(0.0).sliderMax(36.0).build());
   private final Setting<Double> maxSelfDamage = this.sgTargets
      .add(((Builder)new Builder().name("max-self-damage")).defaultValue(36.0).min(0.0).sliderMax(36.0).build());
   private final Setting<Boolean> renderTarget = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("render-target"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("target-color"))
               .defaultValue(new SettingColor(150, 0, 0, 255))
               .visible(this.renderTarget::get))
            .build()
      );
   private final Setting<Boolean> renderPath = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("render-path"))
               .defaultValue(false))
            .build()
      );
   private final Setting<SettingColor> pathColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("path-color"))
               .defaultValue(new SettingColor(0, 150, 0, 255))
               .visible(this.renderPath::get))
            .build()
      );
   private final Setting<SwingMode> swingMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("swing-mode"))
               .defaultValue(SwingMode.Client))
            .build()
      );
   private static final double HORIZONTAL_OFFSET = 0.0;
   private static final double Y_OFFSET = 0.0;
   private static final int PACKET_COOLDOWN = 0;
   private static final boolean IGNORE_NAMED = true;
   private static final boolean IGNORE_TAMED = true;
   private static final boolean RETURN_POS = true;
   private static final boolean S08_RETURN = true;
   private AnchorTP.SmartTPCore core;
   private Vec3d originalPos = null;
   private boolean pendingS08Return = false;
   private int tickCounter = 0;
   private final List<Vec3d> renderPathNodes = new ArrayList<>();
   private int delayTimer = 0;
   private Entity currentTarget = null;
   private int packetsThisTick = 0;
   private long lastPacketTime = 0L;
   private final Map<UUID, Vec3d> lastPosMap = new HashMap<>();
   private final Map<UUID, Vec3d> lastVelMap = new HashMap<>();

   public AnchorTP() {
      super(AmetiumAddon.Ametium_PvP, "AnchorTP", "High Speed and reach AnchorAura.");
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         this.core = new AnchorTP.SmartTPCore(this.mc.world, this.mc.player);
         this.originalPos = null;
         this.pendingS08Return = false;
         this.tickCounter = 0;
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof PlayerPositionLookS2CPacket && this.core != null) {
         this.core.desyncPos = null;
         if (this.originalPos != null && this.tickCounter < 40) {
            this.pendingS08Return = true;
         }
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.tickCounter++;
         this.packetsThisTick = 0;
         if (!this.pendingS08Return) {
            this.currentTarget = this.findTarget();
            if (this.currentTarget != null) {
               if (!(Boolean)this.pauseOnEat.get() || !this.mc.player.isUsingItem()) {
                  if (this.delayTimer > 0) {
                     this.delayTimer--;
                  } else {
                     if (this.attackMode.get() == AnchorTP.AttackMode.ManualKeybind) {
                        if (!((Keybind)this.manualAttackKey.get()).isPressed()) {
                           return;
                        }

                        long now = System.currentTimeMillis();
                        if (now - this.lastManualAttack < 1L) {
                           return;
                        }

                        this.lastManualAttack = now;
                     }

                     FindItemResult anchor = InvUtils.find(new Item[]{Items.RESPAWN_ANCHOR});
                     FindItemResult glowstone = InvUtils.find(new Item[]{Items.GLOWSTONE});
                     if (anchor.found() && glowstone.found()) {
                        int aSlot = this.getSlot(anchor, 6);
                        int gSlot = this.getSlot(glowstone, 7);
                        if (aSlot != -1 && gSlot != -1) {
                           AnchorTP.AttackPos info = this.findBestPos(this.currentTarget);
                           if (info != null) {
                              this.executeTPAuraAttack(info, aSlot, gSlot);
                              this.delayTimer = (Integer)this.attackDelay.get();
                           }
                        }
                     } else {
                        this.delayTimer = 40;
                     }
                  }
               }
            }
         } else {
            this.pendingS08Return = false;
            Vec3d current = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
            if (this.originalPos != null && current.distanceTo(this.originalPos) > 3.0) {
               this.core.updatePathfinding(current, this.originalPos, (Double)this.maxStep.get());
               List<Vec3d> emergency = this.core.getEfficientPath((Double)this.maxStep.get());
               if (emergency != null && !emergency.isEmpty()) {
                  for (Vec3d p : emergency) {
                     this.sendTPPacket(p);
                  }

                  this.sendTPPacket(this.originalPos);
                  this.mc.player.updatePosition(this.originalPos.x, this.originalPos.y, this.originalPos.z);
                  this.originalPos = null;
               }
            }
         }
      }
   }

   private void sendBlock(BlockHitResult hit) {
      this.mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
      this.doSwing(Hand.MAIN_HAND);
   }

   private void doSwing(Hand hand) {
      switch ((SwingMode)this.swingMode.get()) {
         case Packet:
            this.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            break;
         case Client:
            this.mc.player.swingHand(hand);
         case None:
      }
   }

   private Entity findTarget() {
      List<Entity> candidates = new ArrayList<>();

      for (Entity e : this.mc.world.getEntities()) {
         if (e != this.mc.player
            && e.isAlive()
            && e instanceof LivingEntity
            && ((Set)this.entities.get()).contains(e.getType())
            && !(e.distanceTo(this.mc.player) > (Double)this.range.get())
            && (!((Boolean)this.ignoreFriends.get() && e instanceof PlayerEntity p) || !Friends.get().isFriend(p))
            && !e.hasCustomName()
            && !(e instanceof TameableEntity t && t.isTamed())
            && !(
               e instanceof PlayerEntity p
                  && (
                     (Boolean)this.ignoreCreative.get() && this.getGameMode(p) == GameMode.CREATIVE
                        || (Boolean)this.ignoreBots.get() && this.isBot(p)
                        || !Friends.get().shouldAttack(p)
                  )
            )) {
            candidates.add(e);
         }
      }

      if (candidates.isEmpty()) {
         return null;
      }

      switch ((AnchorTP.SortPriority)this.sortPriority.get()) {
         case Closest:
            candidates.sort(Comparator.comparingDouble(ex -> ex.distanceTo(this.mc.player)));
            break;
         case Farthest:
            candidates.sort(Comparator.<Entity>comparingDouble(ex -> ex.distanceTo(this.mc.player)).reversed());
            break;
         case LowestHealth:
            candidates.sort(Comparator.comparingDouble(ex -> ((LivingEntity)ex).getHealth()));
            break;
         case HighestHealth:
            candidates.sort(Comparator.<Entity>comparingDouble(ex -> ((LivingEntity)ex).getHealth()).reversed());
      }

      return candidates.get(0);
   }

   private GameMode getGameMode(PlayerEntity p) {
      PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
      return entry == null ? GameMode.SURVIVAL : entry.getGameMode();
   }

   private boolean isBot(PlayerEntity player) {
      return this.mc.getNetworkHandler() == null ? false : this.mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null;
   }

   private AnchorTP.AttackPos findBestPos(Entity target) {
      Vec3d future = this.predictFuturePos(target, 4);
      Vec3d head = new Vec3d(future.x, future.y + target.getEyeHeight(target.getPose()), future.z);
      BlockPos headPos = BlockPos.ofFloored(head);
      BlockPos[] candidates = new BlockPos[]{headPos.north(), headPos.south(), headPos.east(), headPos.west(), headPos.up(), headPos.down()};
      List<AnchorTP.AttackPos> list = new ArrayList<>();

      for (BlockPos pos : candidates) {
         if (this.checkPlace(pos)) {
            Vec3d tpSpot = this.findSmartTpSpot(pos);
            if (tpSpot != null) {
               Vec3d explosionOrigin = Vec3d.ofCenter(pos);
               float targetDmg = DamageUtils.anchorDamage((LivingEntity)target, explosionOrigin);
               float selfDmg = DamageUtils.anchorDamage(this.mc.player, explosionOrigin);
               if (!(targetDmg < (Double)this.minDamage.get()) && !(selfDmg > (Double)this.maxSelfDamage.get())) {
                  double score = head.squaredDistanceTo(tpSpot);
                  list.add(new AnchorTP.AttackPos(pos, tpSpot, score));
               }
            }
         }
      }

      if (list.isEmpty()) {
         return null;
      }

      list.sort(Comparator.comparingDouble(a -> a.score));
      return list.get(0);
   }

   private boolean checkPlace(BlockPos pos) {
      if (!this.mc.world.isInBuildLimit(pos)) {
         return false;
      }

      BlockState state = this.mc.world.getBlockState(pos);
      return !state.isReplaceable() ? false : this.mc.world.canPlace(Blocks.RESPAWN_ANCHOR.getDefaultState(), pos, ShapeContext.absent());
   }

   private int getSlot(FindItemResult res, int pref) {
      if (res.isHotbar()) {
         return res.slot();
      } else if ((Boolean)this.autoRefill.get() && res.found()) {
         InvUtils.move().from(res.slot()).toHotbar(pref);
         return pref;
      } else {
         return -1;
      }
   }

   private void executeTPAuraAttack(AnchorTP.AttackPos info, int aSlot, int gSlot) {
      Entity baseEntity = (Entity)(this.mc.player.hasVehicle() ? this.mc.player.getVehicle() : this.mc.player);
      Vec3d startPos = new Vec3d(baseEntity.getX(), baseEntity.getY(), baseEntity.getZ());
      Vec3d targetStandPos = this.getTp5BlocksAway(info.pos, this.mc.player);
      if (this.invalid(targetStandPos)) {
         targetStandPos = this.findNearestPos(targetStandPos);
         if (targetStandPos == null) {
            return;
         }
      }

      this.originalPos = startPos;
      this.tickCounter = 0;
      this.core.updatePathfinding(startPos, targetStandPos, (Double)this.maxStep.get());
      List<Vec3d> path = this.core.getEfficientPath((Double)this.maxStep.get());
      if (path != null && !path.isEmpty()) {
         this.renderPathNodes.clear();
         this.renderPathNodes.addAll(path);
         if ((Boolean)this.padding.get()) {
            for (int i = 0; i < 2; i++) {
               this.mc.getNetworkHandler().sendPacket(new OnGroundOnly(this.mc.player.isOnGround()));
            }
         }

         for (Vec3d p : path) {
            this.sendTPPacket(p);
            this.core.desyncPos = p;
         }

         if (!this.handleExistingAnchor(info.pos, gSlot, aSlot)) {
            InvUtils.swap(aSlot, true);
            this.placePacket(info.pos);
            InvUtils.swap(gSlot, true);
            this.interactPacket(info.pos, Direction.UP);
            InvUtils.swap(aSlot, true);
            this.interactPacket(info.pos, Direction.UP);
            InvUtils.swapBack();
            List<Vec3d> reverse = new ArrayList<>(path);
            Collections.reverse(reverse);

            for (Vec3d p : reverse) {
               this.sendTPPacket(p);
            }

            this.sendTPPacket(startPos);
            this.mc.player.updatePosition(startPos.x, startPos.y, startPos.z);
            Vec3d offset = this.getOffset(startPos);
            this.sendTPPacket(offset);
            this.mc.player.updatePosition(offset.x, offset.y, offset.z);
         }
      }
   }

   private void sendTPPacket(Vec3d p) {
      if (p != null && this.mc.getNetworkHandler() != null && this.mc.player != null) {
         PlayerMoveC2SPacket packet = new PositionAndOnGround(p.x, p.y, p.z, false);
         this.sendPacketSafe(packet);
      }
   }

   private Vec3d getOffset(Vec3d base) {
      double dx = 0.0;
      double dy = 0.0;
      Vec3d[] offsets = new Vec3d[]{
         base.add(dx, dy, 0.0),
         base.add(-dx, dy, 0.0),
         base.add(0.0, dy, dx),
         base.add(0.0, dy, -dx),
         base.add(dx, dy, dx),
         base.add(-dx, dy, -dx),
         base.add(-dx, dy, dx),
         base.add(dx, dy, -dx)
      };
      List<Vec3d> list = Arrays.asList(offsets);
      Collections.shuffle(list);

      for (Vec3d p : list) {
         if (!this.invalid(p)) {
            return p;
         }
      }

      return base.add(0.0, dy, 0.0);
   }

   private boolean invalid(Vec3d pos) {
      if (this.mc.world == null) {
         return true;
      }

      BlockPos bp = BlockPos.ofFloored(pos);
      if (this.mc.world.getChunk(bp.getX() >> 4, bp.getZ() >> 4) == null) {
         return true;
      }

      Entity entity = (Entity)(this.mc.player.hasVehicle() ? this.mc.player.getVehicle() : this.mc.player);
      Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
      Box box = entity.getBoundingBox().offset(pos.subtract(entityPos));

      for (BlockPos b : BlockPos.iterate(BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
         BlockState state = this.mc.world.getBlockState(b);
         if (state.isOf(Blocks.LAVA)) {
            return true;
         }

         if (!state.getCollisionShape(this.mc.world, b).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private Vec3d predictFuturePos(Entity target, int ticksAhead) {
      UUID id = target.getUuid();
      Vec3d current = new Vec3d(target.getX(), target.getY(), target.getZ());
      Vec3d lastPos = this.lastPosMap.get(id);
      if (lastPos == null) {
         this.lastPosMap.put(id, current);
         this.lastVelMap.put(id, Vec3d.ZERO);
         return current;
      }

      Vec3d vel = current.subtract(lastPos);
      Vec3d smoothVel = vel.multiply(0.3).add(this.lastVelMap.getOrDefault(id, Vec3d.ZERO).multiply(0.7));
      double maxSpeed = 0.9;
      if (smoothVel.length() > maxSpeed) {
         smoothVel = smoothVel.normalize().multiply(maxSpeed);
      }

      this.lastPosMap.put(id, current);
      this.lastVelMap.put(id, smoothVel);
      return current.add(smoothVel.multiply(ticksAhead));
   }

   private Vec3d getTp5BlocksAway(BlockPos anchor, Entity player) {
      Vec3d anchorCenter = anchor.toCenterPos();
      Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
      Vec3d future = this.currentTarget != null ? this.predictFuturePos(this.currentTarget, 4) : playerPos;
      Vec3d dir = new Vec3d(future.x - anchorCenter.x, 0.0, future.z - anchorCenter.z).normalize();
      Vec3d desired = anchorCenter.add(dir.multiply(5.0));
      if (this.invalid(desired)) {
         Vec3d alt = this.findNearestPos(desired);
         if (alt != null) {
            return alt;
         }
      }

      return desired;
   }

   private Vec3d findNearestPos(Vec3d desired) {
      for (int x = -2; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
               Vec3d test = desired.add(x, y, z);
               if (!this.invalid(test)) {
                  return test;
               }
            }
         }
      }

      return null;
   }

   private boolean handleExistingAnchor(BlockPos pos, int gSlot, int aSlot) {
      BlockState state = this.mc.world.getBlockState(pos);
      if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
         return false;
      } else {
         int charges = (Integer)state.get(RespawnAnchorBlock.CHARGES);
         if (charges < 4) {
            InvUtils.swap(gSlot, true);
            this.interactPacket(pos, Direction.UP);
            InvUtils.swapBack();
            return true;
         } else {
            InvUtils.swap(aSlot, true);
            this.interactPacket(pos, Direction.UP);
            InvUtils.swapBack();
            return true;
         }
      }
   }

   private void placePacket(BlockPos p) {
      this.sendPacketSafe(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(p.toCenterPos(), Direction.UP, p, false), 0));
      this.sendPacketSafe(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void interactPacket(BlockPos p, Direction d) {
      this.sendPacketSafe(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(p.toCenterPos(), d, p, false), 0));
      this.sendPacketSafe(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void sendPacketSafe(Packet<?> packet) {
      long now = System.currentTimeMillis();
      long cd = 0L;
      if (this.packetsThisTick < 20) {
         if (now - this.lastPacketTime >= cd) {
            this.lastPacketTime = now;
            this.packetsThisTick++;
            this.mc.getNetworkHandler().sendPacket(packet);
         }
      }
   }

   private Vec3d findSmartTpSpot(BlockPos anchorPos) {
      BlockPos[] tests = new BlockPos[]{
         anchorPos.north(2),
         anchorPos.south(2),
         anchorPos.east(2),
         anchorPos.west(2),
         anchorPos.north(1),
         anchorPos.south(1),
         anchorPos.east(1),
         anchorPos.west(1),
         anchorPos.up(2),
         anchorPos.down(2),
         anchorPos.up(1),
         anchorPos.down(1)
      };

      for (BlockPos p : tests) {
         Vec3d v = p.toCenterPos();
         if (!this.invalid(v)) {
            return v;
         }
      }

      return null;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if ((Boolean)this.renderTarget.get() && this.currentTarget != null) {
         event.renderer.box(this.currentTarget.getBoundingBox(), (Color)this.targetColor.get(), (Color)this.targetColor.get(), ShapeMode.Lines, 0);
      }

      if ((Boolean)this.renderPath.get() && !this.renderPathNodes.isEmpty()) {
         for (int i = 0; i < this.renderPathNodes.size() - 1; i++) {
            Vec3d n1 = this.renderPathNodes.get(i);
            Vec3d n2 = this.renderPathNodes.get(i + 1);
            event.renderer.line(n1.x, n1.y + 1.0, n1.z, n2.x, n2.y + 1.0, n2.z, (Color)this.pathColor.get());
            event.renderer
               .box(
                  new Box(n1.x - 0.2, n1.y, n1.z - 0.2, n1.x + 0.2, n1.y + 2.0, n1.z + 0.2),
                  (Color)this.pathColor.get(),
                  (Color)this.pathColor.get(),
                  ShapeMode.Lines,
                  0
               );
         }
      }
   }

   private static class AStarPathFinder {
      private final World world;
      private final AnchorTP.CollisionHelper collision;
      private static final int MAX_ITER = 15000;
      private static final long TIMEOUT_MS = 150L;

      public AStarPathFinder(World world, AnchorTP.CollisionHelper collision) {
         this.world = world;
         this.collision = collision;
      }

      public List<Vec3d> findPath(Vec3d startVec, Vec3d endVec, double maxStep) {
         BlockPos start = BlockPos.ofFloored(startVec);
         BlockPos end = BlockPos.ofFloored(endVec);
         if (this.collision.canSweep(startVec, endVec)) {
            return new ArrayList<>(List.of(startVec, endVec));
         }

         Vec3d vclip = this.tryVClip(startVec, endVec);
         if (vclip != null) {
            Vec3d upEnd = new Vec3d(endVec.x, vclip.y, endVec.z);
            List<Vec3d> path = new ArrayList<>();
            path.add(startVec);
            path.add(vclip);
            path.add(upEnd);
            path.add(endVec);
            return path;
         }

         PriorityQueue<AnchorTP.AStarPathFinder.Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
         Map<BlockPos, AnchorTP.AStarPathFinder.Node> visited = new HashMap<>();
         AnchorTP.AStarPathFinder.Node startNode = new AnchorTP.AStarPathFinder.Node(start, null, 0.0, this.heuristic(start, end));
         open.add(startNode);
         visited.put(start, startNode);
         long startTime = System.currentTimeMillis();
         int iterations = 0;
         AnchorTP.AStarPathFinder.Node goal = null;

         while (!open.isEmpty()) {
            if (++iterations > 15000 || System.currentTimeMillis() - startTime > 150L) {
               break;
            }

            AnchorTP.AStarPathFinder.Node current = open.poll();
            if (current.pos.equals(end)) {
               goal = current;
               break;
            }

            for (int dx = -1; dx <= 1; dx++) {
               for (int dy = -1; dy <= 1; dy++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     if (dx != 0 || dy != 0 || dz != 0) {
                        BlockPos nextPos = current.pos.add(dx, dy, dz);
                        if (this.collision.isPassable(nextPos)) {
                           double moveCost = Math.sqrt(dx * dx + dy * dy + dz * dz);
                           double newG = current.g + moveCost;
                           AnchorTP.AStarPathFinder.Node neighbor = visited.getOrDefault(nextPos, new AnchorTP.AStarPathFinder.Node(nextPos));
                           if (newG < neighbor.g) {
                              neighbor.parent = current;
                              neighbor.g = newG;
                              neighbor.f = newG + this.heuristic(nextPos, end);
                              if (!visited.containsKey(nextPos)) {
                                 visited.put(nextPos, neighbor);
                                 open.add(neighbor);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (goal == null) {
            return null;
         }

         List<BlockPos> gridPath = this.reconstruct(goal);
         return this.smooth(gridPath, startVec, endVec);
      }

      private double heuristic(BlockPos a, BlockPos b) {
         return Math.sqrt(a.getSquaredDistance(b));
      }

      private List<BlockPos> reconstruct(AnchorTP.AStarPathFinder.Node goal) {
         List<BlockPos> path = new ArrayList<>();

         for (AnchorTP.AStarPathFinder.Node curr = goal; curr != null; curr = curr.parent) {
            path.add(curr.pos);
         }

         Collections.reverse(path);
         return path;
      }

      private List<Vec3d> smooth(List<BlockPos> grid, Vec3d start, Vec3d end) {
         List<Vec3d> result = new ArrayList<>();
         result.add(start);
         Vec3d last = start;

         for (int i = 1; i < grid.size(); i++) {
            Vec3d curr = grid.get(i).toCenterPos().add(0.0, 0.1, 0.0);
            if (!this.collision.canSweep(last, curr)) {
               Vec3d prev = grid.get(i - 1).toCenterPos().add(0.0, 0.1, 0.0);
               result.add(prev);
               last = prev;
            }
         }

         result.add(end);
         return result;
      }

      private Vec3d tryVClip(Vec3d start, Vec3d end) {
         double maxY = Math.max(start.y, end.y);
         double base = maxY + 1.0;

         for (double y = base; y < base + 50.0; y++) {
            Vec3d upStart = new Vec3d(start.x, y, start.z);
            Vec3d upEnd = new Vec3d(end.x, y, end.z);
            if (this.isSpaceEmpty(upStart) && this.isSpaceEmpty(upEnd) && this.collision.canSweep(upStart, upEnd)) {
               return upStart;
            }
         }

         return null;
      }

      private boolean isSpaceEmpty(Vec3d pos) {
         Box box = new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3);
         return this.world.isSpaceEmpty(null, box) && this.world.getOtherEntities(null, box).isEmpty();
      }

      private static class Node {
         BlockPos pos;
         AnchorTP.AStarPathFinder.Node parent;
         double g = Double.MAX_VALUE;
         double f = Double.MAX_VALUE;

         Node(BlockPos pos) {
            this.pos = pos;
         }

         Node(BlockPos pos, AnchorTP.AStarPathFinder.Node parent, double g, double f) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
         }
      }
   }

   public enum AttackMode {
      Automatic,
      ManualKeybind;
   }

   public enum SwingMode {
      Packet,
      Client,
      None;
   }

   private static class AttackPos {
      BlockPos pos;
      Vec3d tpPos;
      double score;

      public AttackPos(BlockPos p, Vec3d tp, double s) {
         this.pos = p;
         this.tpPos = tp;
         this.score = s;
      }
   }

   private static class CollisionHelper {
      private final World world;
      private static final double FAT_WIDTH = 0.85;
      private static final double PLAYER_HEIGHT = 1.8;

      public CollisionHelper(World world) {
         this.world = world;
      }

      public boolean isPassable(BlockPos pos) {
         BlockState state = this.world.getBlockState(pos);
         return state.getCollisionShape(this.world, pos).isEmpty();
      }

      public boolean canSweep(Vec3d start, Vec3d end) {
         double dist = start.distanceTo(end);
         if (dist < 0.001) {
            return true;
         }

         int steps = (int)Math.ceil(dist / 0.05);
         Vec3d dir = end.subtract(start).multiply(1.0 / steps);
         Vec3d current = start;

         for (int i = 1; i <= steps; i++) {
            current = current.add(dir);
            if (!this.isSafe(current)) {
               return false;
            }
         }

         return true;
      }

      public boolean isSafe(Vec3d pos) {
         return this.isSafeBox(pos.x, pos.y, pos.z);
      }

      private boolean isSafeBox(double x, double y, double z) {
         Box box = new Box(x - 0.425, y + 0.01, z - 0.425, x + 0.425, y + 1.8, z + 0.425);
         return this.world.getOtherEntities(null, box).isEmpty() && this.world.isSpaceEmpty(null, box);
      }

      public double getFloorHeight(BlockPos pos) {
         BlockState state = this.world.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(this.world, pos);
         return shape.isEmpty() ? 0.0 : shape.getMax(Axis.Y);
      }
   }

   private static class SmartTPCore {
      private final World world;
      private final PlayerEntity player;
      private final AnchorTP.CollisionHelper collisionHelper;
      private final AnchorTP.AStarPathFinder pathFinder;
      public Vec3d desyncPos = null;
      private volatile List<Vec3d> currentPath = new ArrayList<>();

      public SmartTPCore(World world, PlayerEntity player) {
         this.world = world;
         this.player = player;
         this.collisionHelper = new AnchorTP.CollisionHelper(world);
         this.pathFinder = new AnchorTP.AStarPathFinder(world, this.collisionHelper);
      }

      public void updatePathfinding(Vec3d start, Vec3d target, double maxStep) {
         List<Vec3d> path = this.pathFinder.findPath(start, target, maxStep);
         if (path != null) {
            this.currentPath = path;
         }
      }

      public List<Vec3d> getEfficientPath(double maxStep) {
         return this.chunkPath(this.currentPath, maxStep);
      }

      private List<Vec3d> chunkPath(List<Vec3d> input, double maxStep) {
         if (input != null && !input.isEmpty()) {
            List<Vec3d> raw = new ArrayList<>(input);
            List<Vec3d> corners = new ArrayList<>();
            corners.add(raw.get(0));

            for (int i = 1; i < raw.size() - 1; i++) {
               Vec3d prev = raw.get(i - 1);
               Vec3d curr = raw.get(i);
               Vec3d next = raw.get(i + 1);
               Vec3d d1 = curr.subtract(prev).normalize();
               Vec3d d2 = next.subtract(curr).normalize();
               if (d1.squaredDistanceTo(d2) > 0.01) {
                  corners.add(curr);
               }
            }

            corners.add(raw.get(raw.size() - 1));
            List<Vec3d> finalPath = new ArrayList<>();
            finalPath.add(corners.get(0));
            int i = 0;

            while (i < corners.size() - 1) {
               int furthest = i + 1;

               for (int j = i + 1; j < corners.size(); furthest = j++) {
                  Vec3d p1 = corners.get(i);
                  Vec3d p2 = corners.get(j);
                  if (p1.distanceTo(p2) > maxStep - 0.05 || !this.collisionHelper.canSweep(p1, p2)) {
                     break;
                  }
               }

               finalPath.add(corners.get(furthest));
               i = furthest;
            }

            return finalPath;
         } else {
            return new ArrayList<>();
         }
      }

      public double getFloorHeightAt(Vec3d pos) {
         return this.collisionHelper.getFloorHeight(BlockPos.ofFloored(pos));
      }
   }

   public enum SortPriority {
      Closest,
      Farthest,
      LowestHealth,
      HighestHealth;
   }
}
