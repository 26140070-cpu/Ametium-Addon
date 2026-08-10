package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoGrind extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> sharedDelay = this.sgGeneral.add(((Builder)((Builder)new Builder().name("delay")).defaultValue(0)).min(0).max(20).build());
   private final Setting<AutoGrind.Mode> grindMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("mode"))
               .defaultValue(AutoGrind.Mode.Whitelist))
            .build()
      );
   private final List<Item> GRINDABLE_ITEMS = Arrays.asList(
      Items.DIAMOND_HELMET,
      Items.DIAMOND_CHESTPLATE,
      Items.DIAMOND_LEGGINGS,
      Items.DIAMOND_BOOTS,
      Items.NETHERITE_HELMET,
      Items.NETHERITE_CHESTPLATE,
      Items.NETHERITE_LEGGINGS,
      Items.NETHERITE_BOOTS,
      Items.IRON_HELMET,
      Items.IRON_CHESTPLATE,
      Items.IRON_LEGGINGS,
      Items.IRON_BOOTS,
      Items.GOLDEN_HELMET,
      Items.GOLDEN_CHESTPLATE,
      Items.GOLDEN_LEGGINGS,
      Items.GOLDEN_BOOTS,
      Items.CHAINMAIL_HELMET,
      Items.CHAINMAIL_CHESTPLATE,
      Items.CHAINMAIL_LEGGINGS,
      Items.CHAINMAIL_BOOTS,
      Items.LEATHER_HELMET,
      Items.LEATHER_CHESTPLATE,
      Items.LEATHER_LEGGINGS,
      Items.LEATHER_BOOTS,
      Items.TURTLE_HELMET,
      Items.DIAMOND_SWORD,
      Items.NETHERITE_SWORD,
      Items.IRON_SWORD,
      Items.GOLDEN_SWORD,
      Items.STONE_SWORD,
      Items.WOODEN_SWORD,
      Items.DIAMOND_PICKAXE,
      Items.NETHERITE_PICKAXE,
      Items.IRON_PICKAXE,
      Items.GOLDEN_PICKAXE,
      Items.STONE_PICKAXE,
      Items.WOODEN_PICKAXE,
      Items.DIAMOND_AXE,
      Items.NETHERITE_AXE,
      Items.IRON_AXE,
      Items.GOLDEN_AXE,
      Items.STONE_AXE,
      Items.WOODEN_AXE,
      Items.DIAMOND_SHOVEL,
      Items.NETHERITE_SHOVEL,
      Items.IRON_SHOVEL,
      Items.GOLDEN_SHOVEL,
      Items.STONE_SHOVEL,
      Items.WOODEN_SHOVEL,
      Items.DIAMOND_HOE,
      Items.NETHERITE_HOE,
      Items.IRON_HOE,
      Items.GOLDEN_HOE,
      Items.STONE_HOE,
      Items.WOODEN_HOE,
      Items.BOW,
      Items.CROSSBOW,
      Items.TRIDENT,
      Items.FISHING_ROD,
      Items.SHEARS,
      Items.FLINT_AND_STEEL,
      Items.ENCHANTED_BOOK,
      Items.CARROT_ON_A_STICK,
      Items.WARPED_FUNGUS_ON_A_STICK,
      Items.ELYTRA
   );
   private final Setting<List<Item>> grindTargetItems = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder().name("items"))
            .filter(this.GRINDABLE_ITEMS::contains)
            .build()
      );
   private boolean grindActive = false;
   private int grindTimer = 0;
   private final List<Integer> grindItemsToProcess = new ArrayList<>();
   private int grindCurrentIndex = 0;
   private int grindProcessedCount = 0;
   private boolean grindWaiting = false;

   public AutoGrind() {
      super(AmetiumAddon.Ametium_Utils, "AutoGrind", " Disenchants items automatically using grindstones.");
   }

   public void onActivate() {
      this.grindActive = false;
      this.grindTimer = 0;
      this.grindItemsToProcess.clear();
      this.grindCurrentIndex = 0;
      this.grindProcessedCount = 0;
      this.grindWaiting = false;
   }

   public void onDeactivate() {
      this.grindActive = false;
   }

   public String getInfoString() {
      return this.grindActive ? "Grind: " + this.grindProcessedCount : "Idle";
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.screen instanceof GrindstoneScreen) {
            this.grindStartProcess();
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.grindTick();
      }
   }

   private void grindStartProcess() {
      if (this.mc.player.currentScreenHandler instanceof GrindstoneScreenHandler) {
         this.grindActive = true;
         this.grindTimer = 2;
         this.grindCurrentIndex = 0;
         this.grindProcessedCount = 0;
         this.grindWaiting = false;
         this.grindItemsToProcess.clear();
      }
   }

   private void grindScanInventory() {
      this.grindItemsToProcess.clear();
      if (this.mc.player != null && this.mc.player.currentScreenHandler instanceof GrindstoneScreenHandler handler) {
         for (int i = 3; i < 39; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty()
               && !stack.getEnchantments().isEmpty()
               && !(stack.getItem() instanceof BlockItem)
               && this.GRINDABLE_ITEMS.contains(stack.getItem())) {
               boolean inList = ((List)this.grindTargetItems.get()).contains(stack.getItem());
               if (this.grindMode.get() == AutoGrind.Mode.Whitelist && inList) {
                  this.grindItemsToProcess.add(i);
               } else if (this.grindMode.get() == AutoGrind.Mode.Blacklist && !inList) {
                  this.grindItemsToProcess.add(i);
               }
            }
         }
      }
   }

   private void grindTick() {
      if (!this.grindActive || this.mc.player == null || !(this.mc.player.currentScreenHandler instanceof GrindstoneScreenHandler)) {
         this.grindActive = false;
      } else if (this.grindTimer > 0) {
         this.grindTimer--;
         if (this.grindTimer == 0 && this.grindItemsToProcess.isEmpty()) {
            this.grindScanInventory();
         }
      } else {
         GrindstoneScreenHandler handler = (GrindstoneScreenHandler)this.mc.player.currentScreenHandler;
         if (!handler.getSlot(2).getStack().isEmpty()) {
            this.mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, this.mc.player);
            this.grindProcessedCount++;
            this.grindWaiting = false;
            this.grindTimer = (Integer)this.sharedDelay.get();
         } else if (!this.grindWaiting) {
            if (handler.getSlot(0).getStack().isEmpty() && this.grindCurrentIndex < this.grindItemsToProcess.size()) {
               int slot = this.grindItemsToProcess.get(this.grindCurrentIndex);
               this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
               this.grindCurrentIndex++;
               this.grindWaiting = true;
               this.grindTimer = (Integer)this.sharedDelay.get();
            } else if (this.grindCurrentIndex >= this.grindItemsToProcess.size()
               && handler.getSlot(0).getStack().isEmpty()
               && handler.getSlot(2).getStack().isEmpty()) {
            }
         }
      }
   }

   private void grindDoInstant() {
      this.grindScanInventory();
      GrindstoneScreenHandler handler = (GrindstoneScreenHandler)this.mc.player.currentScreenHandler;

      for (int slot : this.grindItemsToProcess) {
         this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
         this.mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, this.mc.player);
         this.grindProcessedCount++;
      }
   }

   public enum Mode {
      Whitelist,
      Blacklist;
   }
}
