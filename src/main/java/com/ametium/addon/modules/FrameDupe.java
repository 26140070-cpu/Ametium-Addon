package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;

import java.util.List;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class FrameDupe extends Module {

    private final SettingGroup sgOnly =
            this.settings.createGroup(
                    "Only works on servers with Item Frame plugin"
            );

    private final SettingGroup sgGeneral =
            this.settings.getDefaultGroup();

    private final SettingGroup sgRender =
            this.settings.createGroup("Render");

    private final Setting<Mode> mode =
            this.sgGeneral.add(
                    new EnumSetting.Builder<Mode>()
                            .name("mode")
                            .defaultValue(Mode.WhiteList)
                            .build()
            );

    private final Setting<List<Item>> items =
            this.sgGeneral.add(
                    new ItemListSetting.Builder()
                            .name("items")
                            .defaultValue(
                                    List.of(
                                            Items.SHULKER_BOX,
                                            Items.WHITE_SHULKER_BOX,
                                            Items.ORANGE_SHULKER_BOX,
                                            Items.MAGENTA_SHULKER_BOX,
                                            Items.LIGHT_BLUE_SHULKER_BOX,
                                            Items.YELLOW_SHULKER_BOX,
                                            Items.LIME_SHULKER_BOX,
                                            Items.PINK_SHULKER_BOX,
                                            Items.GRAY_SHULKER_BOX,
                                            Items.LIGHT_GRAY_SHULKER_BOX,
                                            Items.CYAN_SHULKER_BOX,
                                            Items.PURPLE_SHULKER_BOX,
                                            Items.BLUE_SHULKER_BOX,
                                            Items.BROWN_SHULKER_BOX,
                                            Items.GREEN_SHULKER_BOX,
                                            Items.RED_SHULKER_BOX,
                                            Items.BLACK_SHULKER_BOX
                                    )
                            )
                            .build()
            );

    private final Setting<Integer> range =
            this.sgGeneral.add(
                    new IntSetting.Builder()
                            .name("range")
                            .defaultValue(6)
                            .min(0)
                            .sliderMax(6)
                            .build()
            );

    private final Setting<Integer> ticks =
            this.sgGeneral.add(
                    new IntSetting.Builder()
                            .name("ticks")
                            .defaultValue(0)
                            .min(0)
                            .sliderMax(10)
                            .build()
            );

    private final Setting<Boolean> autoPlace =
            this.sgGeneral.add(
                    new BoolSetting.Builder()
                            .name("auto-place")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> autoRefill =
            this.sgGeneral.add(
                    new BoolSetting.Builder()
                            .name("Auto Refill")
                            .defaultValue(true)
                            .build()
            );

    private final Setting<Boolean> render =
            this.sgRender.add(
                    new BoolSetting.Builder()
                            .name("render")
                            .defaultValue(true)
                            .build()
            );

    /*
     * ColorSetting = builder.
     * SettingColor = valor que contiene el color.
     */
    private final Setting<SettingColor> placeColor =
            this.sgRender.add(
                    new ColorSetting.Builder()
                            .name("place-color")
                            .defaultValue(new SettingColor(0, 255, 0, 75))
                            .visible(this.render::get)
                            .build()
            );

    private final Setting<SettingColor> breakColor =
            this.sgRender.add(
                    new ColorSetting.Builder()
                            .name("break-color")
                            .defaultValue(new SettingColor(255, 0, 0, 75))
                            .visible(this.render::get)
                            .build()
            );

    private int timer;
    private int renderTimer;

    private BlockPos lastPos;
    private Direction lastSide;

    private boolean isBreaking;

    public FrameDupe() {
        super(
                AmetiumAddon.Ametium_Utils,
                "FrameDupe",
                "Duplicates items using Item Frames."
        );
    }

    private void forceLookDown() {
        this.mc.getNetworkHandler().sendPacket(
                new LookAndOnGround(
                        this.mc.player.getYaw(),
                        90.0F,
                        this.mc.player.isOnGround()
                )
        );
    }

    private void restoreLook() {
        this.mc.getNetworkHandler().sendPacket(
                new LookAndOnGround(
                        this.mc.player.getYaw(),
                        this.mc.player.getPitch(),
                        this.mc.player.isOnGround()
                )
        );
    }

    private int getItemSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack =
                    this.mc.player.getInventory().getStack(i);

            boolean isMatch =
                    this.items.get().contains(stack.getItem());

            if (this.mode.get() == Mode.WhiteList
                    ? isMatch
                    : !isMatch) {

                return i;
            }
        }

        return this.autoRefill.get()
                ? this.refillFromInventory()
                : -1;
    }

    private int refillFromInventory() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack =
                    this.mc.player.getInventory().getStack(i);

            boolean isMatch =
                    this.items.get().contains(stack.getItem());

            if (this.mode.get() == Mode.WhiteList
                    ? isMatch
                    : !isMatch) {

                InvUtils.move()
                        .from(i)
                        .toHotbar(8);

                return 8;
            }
        }

        return -1;
    }

    private ItemFrameEntity getTargetFrame() {
        return this.mc.world.getEntitiesByClass(
                        ItemFrameEntity.class,
                        new Box(
                                this.mc.player.getBlockPos()
                        ).expand(this.range.get()),
                        entity ->
                                this.mc.player.distanceTo(entity)
                                        <= this.range.get()
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private boolean findStrategicSpot() {
        BlockPos feet =
                this.mc.player.getBlockPos();

        int r = this.range.get();

        double bestDist =
                Double.MAX_VALUE;

        BlockPos bestPos = null;
        Direction bestSide = null;

        for (BlockPos pos : BlockPos.iterate(
                feet.add(-r, -r, -r),
                feet.add(r, r, r)
        )) {

            double dist =
                    feet.getSquaredDistance(pos);

            if (dist > (double) r * r
                    || dist >= bestDist
                    || !this.mc.world
                    .getBlockState(pos)
                    .isAir()) {

                continue;
            }

            for (Direction direction : Direction.values()) {

                BlockPos neighbor =
                        pos.offset(direction);

                if (this.mc.world
                        .getBlockState(neighbor)
                        .isSolidBlock(
                                this.mc.world,
                                neighbor
                        )) {

                    bestDist = dist;
                    bestPos = neighbor;
                    bestSide = direction.getOpposite();

                    break;
                }
            }
        }

        if (bestPos == null) {
            return false;
        }

        this.lastPos = bestPos;
        this.lastSide = bestSide;

        return true;
    }

    @EventHandler
    private void onTick(Pre event) {
        if (this.mc.player == null
                || this.mc.world == null) {

            return;
        }

        if (this.renderTimer > 0) {
            this.renderTimer--;
        }

        this.timer++;

        if (this.timer >= this.ticks.get()) {
            this.timer = 0;

            int slot = this.getItemSlot();

            if (slot == -1) {
                return;
            }

            ItemFrameEntity frame =
                    this.getTargetFrame();

            if (frame == null) {

                if (this.autoPlace.get()
                        && this.findStrategicSpot()) {

                    this.placeFrame();
                }

            } else {

                this.lastPos =
                        frame.getBlockPos();

                this.lastSide =
                        frame.getHorizontalFacing();

                this.interactFrame(
                        frame,
                        slot
                );

                this.renderTimer = 5;
            }
        }
    }

    private void interactFrame(
            ItemFrameEntity frame,
            int slot
    ) {
        InvUtils.swap(
                slot,
                true
        );

        this.forceLookDown();

        if (frame.getHeldItemStack().isEmpty()) {

            this.mc.interactionManager.interactEntity(
                    this.mc.player,
                    frame,
                    Hand.MAIN_HAND
            );

            this.isBreaking = false;

        } else {

            this.mc.interactionManager.attackEntity(
                    this.mc.player,
                    frame
            );

            this.isBreaking = true;
        }

        this.restoreLook();

        InvUtils.swapBack();
    }

    private void placeFrame() {
        FindItemResult frame =
                InvUtils.findInHotbar(
                        Items.ITEM_FRAME,
                        Items.GLOW_ITEM_FRAME
                );

        if (!frame.found()) {
            return;
        }

        InvUtils.swap(
                frame.slot(),
                true
        );

        this.forceLookDown();

        this.mc.interactionManager.interactBlock(
                this.mc.player,
                Hand.MAIN_HAND,
                new BlockHitResult(
                        this.lastPos.toCenterPos(),
                        this.lastSide,
                        this.lastPos,
                        false
                )
        );

        this.restoreLook();

        InvUtils.swapBack();
    }

    private Box getRenderBox() {
        double x = this.lastPos.getX();
        double y = this.lastPos.getY();
        double z = this.lastPos.getZ();

        return switch (this.lastSide) {

            case DOWN ->
                    new Box(
                            x + 0.1,
                            y,
                            z + 0.1,
                            x + 0.9,
                            y + 0.05,
                            z + 0.9
                    );

            case UP ->
                    new Box(
                            x + 0.1,
                            y,
                            z + 0.1,
                            x + 0.9,
                            y + 0.05,
                            z + 0.9
                    );

            case NORTH ->
                    new Box(
                            x + 0.1,
                            y,
                            z,
                            x + 0.9,
                            y + 0.9,
                            z + 0.05
                    );

            case SOUTH ->
                    new Box(
                            x + 0.1,
                            y,
                            z + 0.95,
                            x + 0.9,
                            y + 0.9,
                            z + 1.0
                    );

            case WEST ->
                    new Box(
                            x,
                            y,
                            z + 0.1,
                            x + 0.05,
                            y + 0.9,
                            z + 0.9
                    );

            case EAST ->
                    new Box(
                            x + 0.95,
                            y,
                            z + 0.1,
                            x + 1.0,
                            y + 0.9,
                            z + 0.9
                    );
        };
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!this.render.get()
                || this.lastPos == null
                || this.renderTimer <= 0) {

            return;
        }

        SettingColor color =
                this.isBreaking
                        ? this.breakColor.get()
                        : this.placeColor.get();

        event.renderer.box(
                this.getRenderBox(),
                color,
                color,
                ShapeMode.Both,
                0
        );
    }

    public enum Mode {
        WhiteList,
        BlackList
    }
}
