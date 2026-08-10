package com.ametium.addon.modules;

import com.ametium.addon.AmetiumAddon;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class AutoEnchant extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private static final int FIRST_INPUT_SLOT = 0;
   private static final int SECOND_INPUT_SLOT = 1;
   private static final int OUTPUT_SLOT = 2;
   private final Setting<Integer> delay = this.sgGeneral.add(((Builder)((Builder)new Builder().name("delay")).defaultValue(0)).min(0).max(20).build());
   private final Setting<AutoEnchant.Mode> itemMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("item-mode"))
               .defaultValue(AutoEnchant.Mode.Whitelist))
            .build()
      );
   private final Setting<List<Item>> items = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("items"))
               .defaultValue(Collections.emptyList()))
            .build()
      );
   private final Setting<AutoEnchant.Mode> enchantMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("enchant-mode"))
               .defaultValue(AutoEnchant.Mode.Whitelist))
            .build()
      );
   private final Setting<Set<RegistryKey<Enchantment>>> enchantments = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)new meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder()
               .name("enchantments"))
            .defaultValue(
               new RegistryKey[]{Enchantments.MENDING, Enchantments.UNBREAKING, Enchantments.EFFICIENCY, Enchantments.PROTECTION, Enchantments.SHARPNESS}
            )
            .build()
      );
   private final Setting<Boolean> preferHigherLevel = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("prefer-higher-level"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> minLevel = this.sgGeneral.add(((Builder)((Builder)new Builder().name("min-level")).defaultValue(0)).min(0).max(30).build());
   private int actionTimer = 0;

   public AutoEnchant() {
      super(AmetiumAddon.Ametium_Utils, "AutoEnchant", "Automatically enchant items in anvils.");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.handleAutoEnchant();
      }
   }

   private void handleAutoEnchant() {
      if (this.mc.player.currentScreenHandler instanceof AnvilScreenHandler) {
         if (this.actionTimer > 0) {
            this.actionTimer--;
         } else {
            ScreenHandler handler = this.mc.player.currentScreenHandler;
            ItemStack output = handler.getSlot(2).getStack();
            if (!output.isEmpty()) {
               if (this.mc.player.experienceLevel >= (Integer)this.minLevel.get()) {
                  this.mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                  this.actionTimer = (Integer)this.delay.get();
               }
            } else {
               ItemStack firstInput = handler.getSlot(0).getStack();
               ItemStack secondInput = handler.getSlot(1).getStack();
               if (firstInput.isEmpty() && secondInput.isEmpty()) {
                  int[] pair = this.findPair(handler);
                  if (pair != null) {
                     this.mc.interactionManager.clickSlot(handler.syncId, pair[0], 0, SlotActionType.QUICK_MOVE, this.mc.player);
                     this.mc.interactionManager.clickSlot(handler.syncId, pair[1], 0, SlotActionType.QUICK_MOVE, this.mc.player);
                     this.actionTimer = (Integer)this.delay.get();
                  }
               }
            }
         }
      }
   }

   private int[] findPair(ScreenHandler handler) {
      for (int i = 0; i < handler.slots.size(); i++) {
         Slot itemSlot = handler.getSlot(i);
         if (itemSlot.inventory == this.mc.player.getInventory()) {
            ItemStack item = itemSlot.getStack();
            if (!item.isEmpty() && !item.isOf(Items.ENCHANTED_BOOK)) {
               boolean inList = ((List)this.items.get()).contains(item.getItem());
               boolean itemAllowed = this.itemMode.get() == AutoEnchant.Mode.Whitelist && inList
                  || this.itemMode.get() == AutoEnchant.Mode.Blacklist && !inList;
               if (itemAllowed) {
                  int bestBook = -1;
                  int bestLevel = -1;

                  for (int j = 0; j < handler.slots.size(); j++) {
                     if (j != i) {
                        Slot bookSlot = handler.getSlot(j);
                        if (bookSlot.inventory == this.mc.player.getInventory()) {
                           ItemStack book = bookSlot.getStack();
                           if (book.isOf(Items.ENCHANTED_BOOK) && this.bookQualifies(book, item)) {
                              if (!(Boolean)this.preferHigherLevel.get()) {
                                 return new int[]{i, j};
                              }

                              int level = this.bookLevel(book);
                              if (level > bestLevel) {
                                 bestLevel = level;
                                 bestBook = j;
                              }
                           }
                        }
                     }
                  }

                  if (bestBook != -1) {
                     return new int[]{i, bestBook};
                  }
               }
            }
         }
      }

      return null;
   }

   private boolean bookQualifies(ItemStack book, ItemStack item) {
      ItemEnchantmentsComponent bookEnchants = (ItemEnchantmentsComponent)book.get(DataComponentTypes.STORED_ENCHANTMENTS);
      if (bookEnchants != null && !bookEnchants.isEmpty()) {
         ItemEnchantmentsComponent itemEnchants = (ItemEnchantmentsComponent)item.get(DataComponentTypes.ENCHANTMENTS);
         boolean hasUsefulEnchant = false;

         for (RegistryEntry<Enchantment> entry : bookEnchants.getEnchantments()) {
            if (!entry.value().isAcceptableItem(item)) {
               return false;
            }

            Optional<RegistryKey<Enchantment>> key = entry.getKey();
            if (!key.isEmpty()) {
               boolean inList = ((Set)this.enchantments.get()).contains(key.get());
               boolean allowed = this.enchantMode.get() == AutoEnchant.Mode.Whitelist && inList
                  || this.enchantMode.get() == AutoEnchant.Mode.Blacklist && !inList;
               if (allowed && (itemEnchants == null || itemEnchants.getLevel(entry) == 0)) {
                  hasUsefulEnchant = true;
               }
            }
         }

         return hasUsefulEnchant;
      } else {
         return false;
      }
   }

   private int bookLevel(ItemStack book) {
      ItemEnchantmentsComponent component = (ItemEnchantmentsComponent)book.get(DataComponentTypes.STORED_ENCHANTMENTS);
      if (component == null) {
         return 0;
      }

      int max = 0;

      for (Entry<RegistryEntry<Enchantment>> entry : component.getEnchantmentEntries()) {
         max = Math.max(max, entry.getIntValue());
      }

      return max;
   }

   public enum Mode {
      Whitelist,
      Blacklist;
   }
}
