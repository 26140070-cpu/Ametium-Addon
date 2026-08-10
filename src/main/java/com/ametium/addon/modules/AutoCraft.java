package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoCraft extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Item> craftTargetItem = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("target")).defaultValue(Items.CRAFTING_TABLE)).build());
   private final Setting<Integer> sharedDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("delay"))
               .defaultValue(0))
            .min(0)
            .max(20)
            .build()
      );
   private final Setting<Boolean> sharedAutoClose = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("auto-close"))
               .defaultValue(true))
            .build()
      );
   private boolean craftIsCrafting = false;
   private int craftCount = 0;
   private int craftTickCounter = 0;
   private final Item[] craftCurrentPattern = new Item[9];
   private boolean craftWaitingForCraft = false;
   private Item craftCurrentTarget = null;
   private final Map<Item, Item[]> recipes = new HashMap<>();

   public AutoCraft() {
      super(AmetiumAddon.Ametium_Utils, "AutoCraft", "Crafts any item automatically.");
      this.initAllRecipes();
   }

   public void onActivate() {
      this.craftReset();
   }

   public void onDeactivate() {
      this.craftIsCrafting = false;
   }

   public String getInfoString() {
      return this.craftIsCrafting && this.craftCurrentTarget != null
         ? "Craft: " + this.craftCurrentTarget.getName().getString() + " " + this.craftCount
         : "Idle";
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.screen instanceof CraftingScreen) {
            this.craftOnOpenScreen();
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.craftTick();
      }
   }

   private void initAllRecipes() {
      this.recipes.put(Items.OAK_PLANKS, new Item[]{Items.OAK_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.SPRUCE_PLANKS, new Item[]{Items.SPRUCE_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.BIRCH_PLANKS, new Item[]{Items.BIRCH_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.JUNGLE_PLANKS, new Item[]{Items.JUNGLE_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.ACACIA_PLANKS, new Item[]{Items.ACACIA_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.DARK_OAK_PLANKS, new Item[]{Items.DARK_OAK_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.MANGROVE_PLANKS, new Item[]{Items.MANGROVE_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.CHERRY_PLANKS, new Item[]{Items.CHERRY_LOG, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.BAMBOO_PLANKS, new Item[]{Items.BAMBOO_BLOCK, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.STICK, new Item[]{Items.OAK_PLANKS, null, null, Items.OAK_PLANKS, null, null, null, null, null});
      this.recipes.put(Items.OAK_PRESSURE_PLATE, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, null, null, null, null, null, null, null});
      this.recipes.put(Items.STONE_PRESSURE_PLATE, new Item[]{Items.STONE, Items.STONE, null, null, null, null, null, null, null});
      this.recipes.put(Items.CRAFTING_TABLE, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.OAK_PLANKS, null, null, null, null});
      this.recipes
         .put(Items.WOODEN_PICKAXE, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.WOODEN_AXE, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.WOODEN_SWORD, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.STICK, null, null, Items.STICK, null});
      this.recipes.put(Items.WOODEN_SHOVEL, new Item[]{Items.OAK_PLANKS, null, null, Items.STICK, null, null, null, Items.STICK, null});
      this.recipes
         .put(Items.STONE_PICKAXE, new Item[]{Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, null, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.STONE_AXE, new Item[]{Items.COBBLESTONE, Items.COBBLESTONE, null, Items.COBBLESTONE, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.STONE_SWORD, new Item[]{Items.COBBLESTONE, Items.COBBLESTONE, null, Items.COBBLESTONE, Items.STICK, null, null, Items.STICK, null});
      this.recipes.put(Items.STONE_SHOVEL, new Item[]{Items.COBBLESTONE, null, null, Items.STICK, null, null, null, Items.STICK, null});
      this.recipes
         .put(Items.IRON_PICKAXE, new Item[]{Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.IRON_AXE, new Item[]{Items.IRON_INGOT, Items.IRON_INGOT, null, Items.IRON_INGOT, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.IRON_SWORD, new Item[]{Items.IRON_INGOT, Items.IRON_INGOT, null, Items.IRON_INGOT, Items.STICK, null, null, Items.STICK, null});
      this.recipes.put(Items.IRON_SHOVEL, new Item[]{Items.IRON_INGOT, null, null, Items.STICK, null, null, null, Items.STICK, null});
      this.recipes
         .put(Items.DIAMOND_PICKAXE, new Item[]{Items.DIAMOND, Items.DIAMOND, Items.DIAMOND, null, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.DIAMOND_AXE, new Item[]{Items.DIAMOND, Items.DIAMOND, null, Items.DIAMOND, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.DIAMOND_SWORD, new Item[]{Items.DIAMOND, Items.DIAMOND, null, Items.DIAMOND, Items.STICK, null, null, Items.STICK, null});
      this.recipes.put(Items.DIAMOND_SHOVEL, new Item[]{Items.DIAMOND, null, null, Items.STICK, null, null, null, Items.STICK, null});
      this.recipes
         .put(Items.GOLDEN_PICKAXE, new Item[]{Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.GOLDEN_AXE, new Item[]{Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.GOLD_INGOT, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(Items.GOLDEN_SWORD, new Item[]{Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.GOLD_INGOT, Items.STICK, null, null, Items.STICK, null});
      this.recipes
         .put(
            Items.NETHERITE_PICKAXE, new Item[]{Items.NETHERITE_INGOT, Items.NETHERITE_INGOT, Items.NETHERITE_INGOT, null, Items.STICK, null, null, Items.STICK, null}
         );
      this.recipes
         .put(
            Items.NETHERITE_AXE, new Item[]{Items.NETHERITE_INGOT, Items.NETHERITE_INGOT, null, Items.NETHERITE_INGOT, Items.STICK, null, null, Items.STICK, null}
         );
      this.recipes
         .put(
            Items.NETHERITE_SWORD, new Item[]{Items.NETHERITE_INGOT, Items.NETHERITE_INGOT, null, Items.NETHERITE_INGOT, Items.STICK, null, null, Items.STICK, null}
         );
      this.recipes
         .put(Items.IRON_HELMET, new Item[]{Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.IRON_INGOT, null, null, null});
      this.recipes
         .put(Items.DIAMOND_HELMET, new Item[]{Items.DIAMOND, Items.DIAMOND, Items.DIAMOND, Items.DIAMOND, null, Items.DIAMOND, null, null, null});
      this.recipes
         .put(Items.GOLDEN_HELMET, new Item[]{Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.GOLD_INGOT, null, null, null});
      this.recipes
         .put(
            Items.IRON_CHESTPLATE,
            new Item[]{
               Items.IRON_INGOT,
               null,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT
            }
         );
      this.recipes
         .put(
            Items.DIAMOND_CHESTPLATE,
            new Item[]{
               Items.DIAMOND,
               null,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND
            }
         );
      this.recipes
         .put(
            Items.IRON_LEGGINGS,
            new Item[]{
               Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.IRON_INGOT
            }
         );
      this.recipes
         .put(
            Items.DIAMOND_LEGGINGS,
            new Item[]{
               Items.DIAMOND, Items.DIAMOND, Items.DIAMOND, Items.DIAMOND, null, Items.DIAMOND, Items.DIAMOND, null, Items.DIAMOND
            }
         );
      this.recipes.put(Items.IRON_BOOTS, new Item[]{Items.IRON_INGOT, null, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.IRON_INGOT, null, null, null});
      this.recipes.put(Items.DIAMOND_BOOTS, new Item[]{Items.DIAMOND, null, Items.DIAMOND, Items.DIAMOND, null, Items.DIAMOND, null, null, null});
      this.recipes.put(Items.GOLDEN_BOOTS, new Item[]{Items.GOLD_INGOT, null, Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.GOLD_INGOT, null, null, null});
      this.recipes
         .put(
            Items.GOLDEN_LEGGINGS,
            new Item[]{
               Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.GOLD_INGOT, Items.GOLD_INGOT, null, Items.GOLD_INGOT
            }
         );
      this.recipes
         .put(
            Items.GOLDEN_CHESTPLATE,
            new Item[]{
               Items.GOLD_INGOT,
               null,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT
            }
         );
      this.recipes
         .put(
            Items.DIAMOND_BLOCK,
            new Item[]{
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND,
               Items.DIAMOND
            }
         );
      this.recipes
         .put(
            Items.IRON_BLOCK,
            new Item[]{
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT,
               Items.IRON_INGOT
            }
         );
      this.recipes
         .put(
            Items.GOLD_BLOCK,
            new Item[]{
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT,
               Items.GOLD_INGOT
            }
         );
      this.recipes
         .put(
            Items.COAL_BLOCK,
            new Item[]{
               Items.COAL,
               Items.COAL,
               Items.COAL,
               Items.COAL,
               Items.COAL,
               Items.COAL,
               Items.COAL,
               Items.COAL,
               Items.COAL
            }
         );
      this.recipes
         .put(
            Items.CHEST,
            new Item[]{
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               null,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS
            }
         );
      this.recipes
         .put(
            Items.BARREL,
            new Item[]{
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               null,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS,
               Items.OAK_PLANKS
            }
         );
      this.recipes
         .put(
            Items.FURNACE,
            new Item[]{
               Items.COBBLESTONE,
               Items.COBBLESTONE,
               Items.COBBLESTONE,
               Items.COBBLESTONE,
               null,
               Items.COBBLESTONE,
               Items.COBBLESTONE,
               Items.COBBLESTONE,
               Items.COBBLESTONE
            }
         );
      this.recipes.put(Items.TORCH, new Item[]{Items.STICK, Items.COAL, null, null, null, null, null, null, null});
      this.recipes
         .put(
            Items.LANTERN,
            new Item[]{
               Items.IRON_NUGGET,
               Items.IRON_NUGGET,
               Items.IRON_NUGGET,
               Items.IRON_NUGGET,
               Items.TORCH,
               Items.IRON_NUGGET,
               Items.IRON_NUGGET,
               Items.IRON_NUGGET,
               Items.IRON_NUGGET
            }
         );
      this.recipes.put(Items.REDSTONE_TORCH, new Item[]{Items.STICK, Items.REDSTONE, null, null, null, null, null, null, null});
      this.recipes
         .put(
            Items.REPEATER,
            new Item[]{Items.REDSTONE_TORCH, Items.REDSTONE_TORCH, Items.REDSTONE_TORCH, Items.STONE, Items.REDSTONE, Items.STONE, null, null, null}
         );
      this.recipes
         .put(
            Items.COMPARATOR, new Item[]{Items.STONE, Items.STONE, Items.STONE, Items.REDSTONE_TORCH, Items.QUARTZ, Items.REDSTONE_TORCH, null, null, null}
         );
      this.recipes.put(Items.BREAD, new Item[]{Items.WHEAT, Items.WHEAT, Items.WHEAT, null, null, null, null, null, null});
      this.recipes.put(Items.SUGAR, new Item[]{Items.SUGAR_CANE, null, null, null, null, null, null, null, null});
      this.recipes
         .put(
            Items.CAKE,
            new Item[]{
               Items.MILK_BUCKET,
               Items.MILK_BUCKET,
               Items.MILK_BUCKET,
               Items.SUGAR,
               Items.EGG,
               Items.SUGAR,
               Items.WHEAT,
               Items.WHEAT,
               Items.WHEAT
            }
         );
      this.recipes.put(Items.FIREWORK_ROCKET, new Item[]{Items.PAPER, Items.GUNPOWDER, null, null, null, null, null, null, null});
      this.recipes.put(Items.FIREWORK_STAR, new Item[]{Items.GUNPOWDER, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.GLASS_PANE, new Item[]{Items.GLASS, Items.GLASS, Items.GLASS, Items.GLASS, Items.GLASS, Items.GLASS, null, null, null});
      this.recipes.put(Items.GLASS_BOTTLE, new Item[]{Items.GLASS, null, null, null, null, null, null, null, null});
      this.recipes
         .put(Items.RAIL, new Item[]{Items.IRON_INGOT, null, Items.IRON_INGOT, Items.STICK, Items.IRON_INGOT, Items.STICK, null, null, null});
      this.recipes
         .put(
            Items.MINECART,
            new Item[]{Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, null, null}
         );
      this.recipes
         .put(
            Items.LADDER,
            new Item[]{
               Items.STICK, Items.STICK, Items.STICK, Items.STICK, Items.STICK, Items.STICK, Items.STICK, null, null
            }
         );
      this.recipes
         .put(
            Items.OAK_DOOR,
            new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.OAK_PLANKS, null}
         );
      this.recipes
         .put(
            Items.OAK_FENCE,
            new Item[]{Items.STICK, Items.OAK_PLANKS, Items.STICK, Items.STICK, Items.OAK_PLANKS, Items.STICK, null, null, null}
         );
      this.recipes
         .put(
            Items.OAK_FENCE_GATE,
            new Item[]{Items.STICK, Items.OAK_PLANKS, Items.STICK, Items.STICK, Items.OAK_PLANKS, Items.STICK, null, null, null}
         );
      this.recipes.put(Items.OAK_SLAB, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, null, null, null, null, null, null});
      this.recipes
         .put(
            Items.OAK_STAIRS,
            new Item[]{Items.OAK_PLANKS, null, null, Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS}
         );
      this.recipes
         .put(
            Items.PAINTING,
            new Item[]{
               Items.STICK,
               Items.STICK,
               Items.STICK,
               Items.STICK,
               Items.WHITE_WOOL,
               Items.STICK,
               Items.STICK,
               Items.STICK,
               Items.STICK
            }
         );
      this.recipes
         .put(
            Items.ITEM_FRAME,
            new Item[]{
               Items.STICK,
               Items.STICK,
               Items.STICK,
               Items.STICK,
               Items.LEATHER,
               Items.STICK,
               Items.STICK,
               Items.STICK,
               Items.STICK
            }
         );
      this.recipes.put(Items.LEVER, new Item[]{Items.STICK, Items.COBBLESTONE, null, null, null, null, null, null, null});
      this.recipes.put(Items.STONE_BUTTON, new Item[]{Items.STONE, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.OAK_BUTTON, new Item[]{Items.OAK_PLANKS, null, null, null, null, null, null, null, null});
      this.recipes.put(Items.PAPER, new Item[]{Items.SUGAR_CANE, Items.SUGAR_CANE, Items.SUGAR_CANE, null, null, null, null, null, null});
      this.recipes.put(Items.BOOK, new Item[]{Items.PAPER, Items.PAPER, Items.PAPER, Items.LEATHER, null, null, null, null, null});
   }

   private boolean craftCanCraftOne() {
      if (this.craftCurrentTarget != null && this.craftCurrentPattern != null) {
         Map<Item, Integer> needed = new HashMap<>();

         for (int i = 0; i < 9; i++) {
            Item neededItem = this.craftCurrentPattern[i];
            if (neededItem != null) {
               needed.put(neededItem, needed.getOrDefault(neededItem, 0) + 1);
            }
         }

         for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
            int available = this.craftCountItem(entry.getKey());
            if (available < entry.getValue()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private int craftCountItem(Item item) {
      if (this.mc.player == null) {
         return 0;
      }

      int count = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private void craftReset() {
      this.craftIsCrafting = false;
      this.craftCount = 0;
      this.craftTickCounter = 0;
      this.craftWaitingForCraft = false;
      this.craftCurrentTarget = null;
      Arrays.fill(this.craftCurrentPattern, null);
   }

   private void craftOnOpenScreen() {
      this.craftCurrentTarget = (Item)this.craftTargetItem.get();
      Item[] pattern = this.recipes.get(this.craftCurrentTarget);
      if (pattern != null) {
         System.arraycopy(pattern, 0, this.craftCurrentPattern, 0, 9);
         if (!this.craftCanCraftOne()) {
            if ((Boolean)this.sharedAutoClose.get()) {
               this.mc.player.closeHandledScreen();
            }

            return;
         }

         this.craftIsCrafting = true;
         this.craftCount = 0;
         this.craftWaitingForCraft = false;
      }
   }

   private void craftTick() {
      if (this.craftIsCrafting && this.mc.player != null) {
         if (!(this.mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            this.craftIsCrafting = false;
         } else if (this.craftTickCounter > 0) {
            this.craftTickCounter--;
         } else {
            CraftingScreenHandler handler = (CraftingScreenHandler)this.mc.player.currentScreenHandler;
            if (!handler.getSlot(0).getStack().isEmpty()) {
               this.craftTakeResult(handler);
            } else if (!this.craftWaitingForCraft) {
               if (!this.craftCanCraftOne()) {
                  this.craftFinish();
               } else {
                  this.craftPlaceAllMaterialsAtOnce(handler);
               }
            }
         }
      }
   }

   private void craftPlaceAllMaterialsAtOnce(CraftingScreenHandler handler) {
      for (int slot = 1; slot <= 9; slot++) {
         Item needed = this.craftCurrentPattern[slot - 1];
         if (needed == null && !handler.getSlot(slot).getStack().isEmpty()) {
            this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
         }
      }

      for (int slot = 1; slot <= 9; slot++) {
         Item needed = this.craftCurrentPattern[slot - 1];
         if (needed != null) {
            ItemStack current = handler.getSlot(slot).getStack();
            if (current.isEmpty() || current.getItem() != needed) {
               int sourceSlot = this.craftFindItemInInventory(handler, needed);
               if (sourceSlot != -1) {
                  this.mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, this.mc.player);
                  this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, this.mc.player);
               }
            }
         }
      }

      this.craftWaitingForCraft = true;
      this.craftTickCounter = (Integer)this.sharedDelay.get();
   }

   private int craftFindItemInInventory(CraftingScreenHandler handler, Item item) {
      for (int i = 10; i < 46; i++) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private void craftTakeResult(CraftingScreenHandler handler) {
      this.mc.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, this.mc.player);
      this.craftCount++;
      this.craftWaitingForCraft = false;
      this.craftTickCounter = (Integer)this.sharedDelay.get();
   }

   private void craftFinish() {
      if ((Boolean)this.sharedAutoClose.get()) {
         this.mc.player.closeHandledScreen();
      }

      this.craftIsCrafting = false;
      this.craftWaitingForCraft = false;
      this.craftCount = 0;
   }
}
