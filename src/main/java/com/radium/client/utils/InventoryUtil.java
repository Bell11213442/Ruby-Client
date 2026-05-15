package com.radium.client.utils;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.mixins.ClientPlayerInteractionManagerAccessor;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

import java.util.function.Predicate;

public final class InventoryUtil {
    public static void swap(final int selectedSlot) {
        if (selectedSlot < 0 || selectedSlot > 8) {
            return;
        }
        RadiumClient.mc.player.getInventory().selectedSlot = selectedSlot;
        ((ClientPlayerInteractionManagerAccessor) RadiumClient.mc.interactionManager).syncSlot();
    }

    public static boolean swapStack(final Predicate<ItemStack> predicate) {
        final PlayerInventory getInventory = RadiumClient.mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            if (predicate.test(getInventory.getStack(i))) {
                getInventory.selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    public static boolean swapItem(final Predicate<Item> predicate) {
        final PlayerInventory getInventory = RadiumClient.mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            if (predicate.test(getInventory.getStack(i).getItem())) {
                getInventory.selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    public static boolean swap(Item item) {
        return InventoryUtil.swapItem((Item item2) -> item2 == item);
    }

    public static int getSlot(final Item obj) {
        final ScreenHandler currentScreenHandler = RadiumClient.mc.player.currentScreenHandler;
        if (RadiumClient.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            int n = 0;
            for (int i = 0; i < ((GenericContainerScreenHandler) RadiumClient.mc.player.currentScreenHandler).getRows() * 9; ++i) {
                if (currentScreenHandler.getSlot(i).getStack().getItem().equals(obj)) {
                    ++n;
                }
            }
            return n;
        }
        return 0;
    }

    public static int findTotemSlot() {
        PlayerInventory inv = RadiumClient.mc.player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }

    public static int findRandomTotemSlot() {
        PlayerInventory inv = RadiumClient.mc.player.getInventory();
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) return -1;
        return slots.get(new java.util.Random().nextInt(slots.size()));
    }

    public static int countItemExceptHotbar(Item item) {
        PlayerInventory inv = RadiumClient.mc.player.getInventory();
        int count = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean isTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof net.minecraft.item.ToolItem || item instanceof net.minecraft.item.SwordItem || item instanceof net.minecraft.item.ArmorItem;
    }
}
