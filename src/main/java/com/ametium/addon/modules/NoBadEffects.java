package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class NoBadEffects extends Module {
    private final SettingGroup sgMovement =
            this.settings.createGroup("Movement");

    private final SettingGroup sgVisuals =
            this.settings.createGroup("Visuals");

    private final Setting<Boolean> levitation =
            this.sgMovement.add(
                    new Builder()
                            .name("anti-levitation")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> slowFalling =
            this.sgMovement.add(
                    new Builder()
                            .name("anti-slow-falling")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> darkness =
            this.sgVisuals.add(
                    new Builder()
                            .name("anti-darkness")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> blindness =
            this.sgVisuals.add(
                    new Builder()
                            .name("anti-blindness")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> nausea =
            this.sgVisuals.add(
                    new Builder()
                            .name("anti-nausea")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> miningFatigue =
            this.sgVisuals.add(
                    new Builder()
                            .name("anti-mining-fatigue")
                            .defaultValue(true)
                            .build()
            );

    public NoBadEffects() {
        super(
                AmetiumAddon.Ametium,
                "NoBadEffects",
                "Removes active negative effects."
        );
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player == null) {
            return;
        }

        if (this.levitation.get()
                && this.mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {

            this.mc.player.removeStatusEffect(StatusEffects.LEVITATION);

            Vec3d velocity = this.mc.player.getVelocity();

            if (velocity.y > 0.0) {
                this.mc.player.setVelocity(
                        velocity.x,
                        0.0,
                        velocity.z
                );
            }
        }

        if (this.slowFalling.get()
                && this.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {

            this.mc.player.removeStatusEffect(StatusEffects.SLOW_FALLING);
        }

        if (this.darkness.get()
                && this.mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {

            this.mc.player.removeStatusEffect(StatusEffects.DARKNESS);
        }

        if (this.blindness.get()
                && this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {

            this.mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        }

        if (this.nausea.get()
                && this.mc.player.hasStatusEffect(StatusEffects.NAUSEA)) {

            this.mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        }

        if (this.miningFatigue.get()
                && this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {

            this.mc.player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        }
    }
}

