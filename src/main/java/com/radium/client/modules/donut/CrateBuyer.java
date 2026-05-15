package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class CrateBuyer extends Module {

    private static final int HELMET_SLOT = 10;
    private static final int CHESTPLATE_SLOT = 11;
    private static final int LEGGINGS_SLOT = 12;
    private static final int BOOTS_SLOT = 13;
    private static final int SWORD_SLOT = 14;
    private static final int PICKAXE_SLOT = 15;
    private static final int SHOVEL_SLOT = 16;
    private static final int CONFIRM_SLOT_DEFAULT = 15;
    private final ModeSetting<CrateBuyer.ItemType> action = new ModeSetting<>("Action", CrateBuyer.ItemType.All, CrateBuyer.ItemType.class);
    private final int delay = 4;
    private int tickCounter = 0;
    private int warningCooldown = 0;
    private int currentStep = 0;
    private int currentItemIndex = 0;
    private boolean hasClickedOnce = false;

    public CrateBuyer() {
        super("CrateBuyer", "Automatically buys items from the common crate", Category.DONUT);
        this.addSettings(action);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;

        if (warningCooldown > 0) {
            warningCooldown--;
        }

        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
            if (isEnabled() && warningCooldown == 0) {
                ChatUtils.e("You need to be on the crate screen to use this module.");
                warningCooldown = 20;
            }
            return;
        }

        if (!hasClickedOnce && !isValidCrateScreen(screen)) {
            if (warningCooldown == 0) {
                ChatUtils.e("This doesn't appear to be a valid crate screen. Closing screen.");
                warningCooldown = 20;
                mc.setScreen(null);
            }
            return;
        }

        tickCounter++;

        if (tickCounter < delay) {
            return;
        }

        tickCounter = 0;

        if (action.getValue() == ItemType.All) {
            handleAllItems(screen);
        } else {
            handleSingleItem(screen);
        }
    }

    private boolean isValidCrateScreen(HandledScreen<?> screen) {
        for (int i = 0; i <= 9; i++) {
            if (!screen.getScreenHandler().getSlot(i).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE)) {
                return false;
            }
        }

        for (int i = 17; i <= 26; i++) {
            if (!screen.getScreenHandler().getSlot(i).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE)) {
                return false;
            }
        }

        return true;
    }

    private void handleAllItems(HandledScreen<?> screen) {
        ItemType[] items = {ItemType.Helmet, ItemType.Chestplate, ItemType.Leggings,
                ItemType.Boots, ItemType.Sword, ItemType.Pickaxe, ItemType.Shovel};

        if (currentStep == 0) {
            int itemSlot = getItemSlot(items[currentItemIndex]);
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
            hasClickedOnce = true;
            currentStep = 1;
        } else {
            int confirmSlot = getConfirmSlot(items[currentItemIndex]);
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, confirmSlot, 0, SlotActionType.PICKUP, mc.player);
            currentStep = 0;

            currentItemIndex++;
            if (currentItemIndex >= items.length) {
                currentItemIndex = 0;
            }
        }
    }

    private void handleSingleItem(HandledScreen<?> screen) {
        if (currentStep == 0) {
            int itemSlot = getItemSlot(action.getValue());
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
            hasClickedOnce = true;
            currentStep = 1;
        } else {
            int confirmSlot = getConfirmSlot(action.getValue());
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, confirmSlot, 0, SlotActionType.PICKUP, mc.player);
            currentStep = 0;
        }
    }

    private int getItemSlot(ItemType itemType) {
        switch (itemType) {
            case Helmet:
                return HELMET_SLOT;
            case Chestplate:
                return CHESTPLATE_SLOT;
            case Leggings:
                return LEGGINGS_SLOT;
            case Boots:
                return BOOTS_SLOT;
            case Sword:
                return SWORD_SLOT;
            case Pickaxe:
                return PICKAXE_SLOT;
            case Shovel:
                return SHOVEL_SLOT;
            default:
                return HELMET_SLOT;
        }
    }

    private int getConfirmSlot(ItemType itemType) {
        return CONFIRM_SLOT_DEFAULT;
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        warningCooldown = 0;
        currentStep = 0;
        currentItemIndex = 0;
        hasClickedOnce = false;
        ChatUtils.m("Activated. Mode: " + action.getValue().toString());
    }

    @Override
    public void onDisable() {
        currentStep = 0;
        currentItemIndex = 0;
        hasClickedOnce = false;
    }


    public enum ItemType {
        All,
        Helmet,
        Chestplate,
        Leggings,
        Boots,
        Sword,
        Pickaxe,
        Shovel
    }
}
