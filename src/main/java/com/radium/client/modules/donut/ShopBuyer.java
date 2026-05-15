package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ShopBuyer extends Module {

    private final ModeSetting<ShopBuyer.ItemType> itemToBuy = new ModeSetting<>("Item", ShopBuyer.ItemType.Obsidian, ShopBuyer.ItemType.class);
    private final BooleanSetting autoDrop = new BooleanSetting("Auto Drop", true);
    private final int delay = 1;
    private int delayCounter = 0;
    private boolean inPvpCategory = false;
    private boolean inBuyingScreen = false;

    public ShopBuyer() {
        super("ShopBuyer", "Automatically buys selected items from PVP shop category", Category.DONUT);
        this.addSettings(itemToBuy, autoDrop);
    }

    @Override
    public void onEnable() {
        delayCounter = 0;
        inPvpCategory = false;
        inBuyingScreen = false;
    }

    @Override
    public void onDisable() {
        delayCounter = 0;
        inPvpCategory = false;
        inBuyingScreen = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (delayCounter > 0) {
            delayCounter--;
            return;
        }

        ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;

        if (!(currentScreenHandler instanceof GenericContainerScreenHandler containerHandler)) {
            mc.player.networkHandler.sendChatCommand("shop");
            delayCounter = delay;
            resetState();
            return;
        }

        int rows = containerHandler.getRows();

        updateScreenState(currentScreenHandler, rows);

        if (rows == 3) {
            if (isBuyingScreen(currentScreenHandler)) {
                handleBuyingScreen(currentScreenHandler);
                return;
            } else if (isPvpCategoryScreen(currentScreenHandler)) {
                handlePvpCategory(currentScreenHandler);
                return;
            } else if (isMainShopScreen(currentScreenHandler)) {
                handleMainShop(currentScreenHandler);
                return;
            }
        }

        resetState();
    }

    private void updateScreenState(ScreenHandler handler, int rows) {
        if (isBuyingScreen(handler)) {
            inBuyingScreen = true;
        } else if (isPvpCategoryScreen(handler)) {
            inPvpCategory = true;
            inBuyingScreen = false;
        } else if (isMainShopScreen(handler)) {
            inPvpCategory = false;
            inBuyingScreen = false;
        }
    }

    private boolean isMainShopScreen(ScreenHandler handler) {
        return handler.getSlot(13).getStack().isOf(Items.TOTEM_OF_UNDYING) &&
                !isBuyingScreen(handler);
    }

    private boolean isPvpCategoryScreen(ScreenHandler handler) {
        return handler.getSlot(9).getStack().isOf(Items.OBSIDIAN) ||
                handler.getSlot(10).getStack().isOf(Items.END_CRYSTAL) ||
                handler.getSlot(11).getStack().isOf(Items.RESPAWN_ANCHOR) ||
                handler.getSlot(12).getStack().isOf(Items.GLOWSTONE);
    }

    private boolean isBuyingScreen(ScreenHandler handler) {
        for (int i = 0; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                return true;
            }
        }
        return false;
    }

    private void handleMainShop(ScreenHandler handler) {
        mc.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, mc.player);
        delayCounter = delay;
        inPvpCategory = true;
    }

    private void handlePvpCategory(ScreenHandler handler) {
        ItemType selectedItem = itemToBuy.getValue();
        int slot = getItemSlot(selectedItem);

        if (slot != -1 && isCorrectItemInSlot(handler, slot, selectedItem)) {
            clickItem(handler, slot);
        }
    }

    private int getItemSlot(ItemType itemType) {
        switch (itemType) {
            case Obsidian:
                return 9;
            case EndCrystal:
                return 10;
            case RespawnAnchor:
                return 11;
            case Glowstone:
                return 12;
            case TotemOfUndying:
                return 13;
            case EnderPearl:
                return 14;
            case GoldenApple:
                return 15;
            case ExperienceBottle:
                return 16;
            case SlowFallingArrow:
                return 17;
            default:
                return -1;
        }
    }

    private boolean isCorrectItemInSlot(ScreenHandler handler, int slot, ItemType itemType) {
        switch (itemType) {
            case Obsidian:
                return handler.getSlot(slot).getStack().isOf(Items.OBSIDIAN);
            case EndCrystal:
                return handler.getSlot(slot).getStack().isOf(Items.END_CRYSTAL);
            case RespawnAnchor:
                return handler.getSlot(slot).getStack().isOf(Items.RESPAWN_ANCHOR);
            case Glowstone:
                return handler.getSlot(slot).getStack().isOf(Items.GLOWSTONE);
            case TotemOfUndying:
                return handler.getSlot(slot).getStack().isOf(Items.TOTEM_OF_UNDYING);
            case EnderPearl:
                return handler.getSlot(slot).getStack().isOf(Items.ENDER_PEARL);
            case GoldenApple:
                return handler.getSlot(slot).getStack().isOf(Items.GOLDEN_APPLE);
            case ExperienceBottle:
                return handler.getSlot(slot).getStack().isOf(Items.EXPERIENCE_BOTTLE);
            case SlowFallingArrow:
                return handler.getSlot(slot).getStack().isOf(Items.TIPPED_ARROW);
            default:
                return false;
        }
    }

    private void handleBuyingScreen(ScreenHandler handler) {
        for (int i = 0; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE) &&
                    handler.getSlot(i).getStack().getCount() == 64) {

                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                delayCounter = delay;
                return;
            }
        }

        for (int i = 0; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                delayCounter = delay;

                if (autoDrop.getValue()) {
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                            BlockPos.ORIGIN,
                            Direction.DOWN
                    ));
                }

                resetState();
                return;
            }
        }
    }

    private void clickItem(ScreenHandler handler, int slot) {
        mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        delayCounter = delay;
        inBuyingScreen = true;
    }

    private void resetState() {
        inPvpCategory = false;
        inBuyingScreen = false;
    }

    public enum ItemType {
        Obsidian,
        EndCrystal,
        RespawnAnchor,
        Glowstone,
        TotemOfUndying,
        EnderPearl,
        GoldenApple,
        ExperienceBottle,
        SlowFallingArrow
    }
}
