package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Double> range = this.sgGeneral.add(((Builder)new Builder().name("range")).defaultValue(5.0).min(1.0).sliderMax(20.0).build());
    private final Setting<Double> fov = this.sgGeneral.add(((Builder)new Builder().name("fov")).defaultValue(60.0).min(1.0).sliderMax(180.0).build());
    private final Setting<Double> speed = this.sgGeneral
            .add(((Builder)new Builder().name("speed")).defaultValue(35.0).min(1.0).max(100.0).sliderRange(1.0, 100.0).build());
    private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                            .name("entities"))
                            .onlyAttackable()
                            .defaultValue(new EntityType[]{EntityType.PLAYER})
                            .build()
            );
    private final Setting<Boolean> ignoreFriends = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("ignore-friends"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<Boolean> ignoreBots = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("ignore-bots"))
                            .defaultValue(true))
                            .build()
            );
    private final Setting<Boolean> ignoreInvisible = this.sgGeneral
            .add(
                    ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                            .name("ignore-invisible"))
                            .defaultValue(true))
                            .build()
            );
    private Entity currentTarget;

    public AimAssist() {
        super(AmetiumAddon.Ametium_PvP, "AimAssist", "Keeps your crosshair on the target's hitbox.");
    }

    public void onDeactivate() {
        this.currentTarget = null;
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player != null && this.mc.world != null) {
            if (this.currentTarget == null || !this.isValid(this.currentTarget)) {
                this.currentTarget = this.findTarget();
            }

            if (this.currentTarget != null) {
                Vec3d aimPoint = this.closestVisiblePoint(this.currentTarget.getBoundingBox());
                if (aimPoint != null) {
                    this.aimAt(aimPoint);
                }
            }
        }
    }

    private Entity findTarget() {
        Entity best = null;
        double bestAngle = (Double)this.fov.get();

        for (Entity e : this.mc.world.getEntities()) {
            if (this.isValid(e)) {
                double angle = this.angleTo(e.getBoundingBox());
                if (angle < bestAngle) {
                    bestAngle = angle;
                    best = e;
                }
            }
        }

        return best;
    }

    private boolean isValid(Entity e) {
        if (e instanceof LivingEntity living) {
            if (living == this.mc.player) {
                return false;
            }

            if (!living.isAlive()) {
                return false;
            }

            if (!((Set)this.entities.get()).contains(e.getType())) {
                return false;
            }

            if (this.mc.player.squaredDistanceTo(living) > (Double)this.range.get()) {
                return false;
            }

            if ((Boolean)this.ignoreInvisible.get() && living.isInvisible()) {
                return false;
            }

            if (living instanceof PlayerEntity p) {
                if (p.isCreative() || p.isSpectator()) {
                    return false;
                }

                if ((Boolean)this.ignoreFriends.get() && Friends.get().isFriend(p)) {
                    return false;
                }

                if ((Boolean)this.ignoreBots.get()) {
                    PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                    if (entry == null) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private double angleTo(Box box) {
        Vec3d eyes = this.mc.player.getEyePos();
        Vec3d view = this.mc.player.getRotationVec(1.0F);
        Vec3d toBox = this.closestPoint(box, eyes).subtract(eyes).normalize();
        return Math.toDegrees(Math.acos(MathHelper.clamp(view.dotProduct(toBox), -1.0, 1.0)));
    }

    private Vec3d closestPoint(Box box, Vec3d from) {
        double x = MathHelper.clamp(from.x, box.minX, box.maxX);
        double y = MathHelper.clamp(from.y, box.minY, box.maxY);
        double z = MathHelper.clamp(from.z, box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    private Vec3d closestVisiblePoint(Box box) {
        Vec3d eyes = this.mc.player.getEyePos();
        Vec3d view = this.mc.player.getRotationVec(1.0F);
        if (box.raycast(eyes, eyes.add(view.multiply((Double)this.range.get() * 2.0))).isPresent()) {
            return null;
        }

        double distance = eyes.distanceTo(box.getCenter());
        Vec3d projected = eyes.add(view.multiply(distance));
        return this.closestPoint(box, projected);
    }

    private void aimAt(Vec3d point) {
        Vec3d eyes = this.mc.player.getEyePos();
        Vec3d toPoint = point.subtract(eyes);
        float targetYaw = (float)(Math.toDegrees(Math.atan2(toPoint.z, toPoint.x)) - 90.0);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(toPoint.y, Math.sqrt(toPoint.x * toPoint.x + toPoint.z * toPoint.z))));
        targetPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);
        float currentYaw = this.mc.player.getYaw();
        float currentPitch = this.mc.player.getPitch();
        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        float ease = MathHelper.clamp(((Double)this.speed.get()).floatValue() / 100.0F, 0.01F, 1.0F);
        this.mc.player.setYaw(currentYaw + yawDiff * ease);
        this.mc.player.setPitch(MathHelper.clamp(currentPitch + pitchDiff * ease, -90.0F, 90.0F));
    }

    public String getInfoString() {
        return this.currentTarget != null ? EntityUtils.getName(this.currentTarget) : null;
    }
}
