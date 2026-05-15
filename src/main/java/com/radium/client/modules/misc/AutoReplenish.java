package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoReplenish extends Module {

    private final NumberSetting threshold = new NumberSetting("Threshold", 1.0, 1.0, 64.0, 1.0);
    private final BooleanSetting onlyHotbar = new BooleanSetting("Only Hotbar", true);
    private final BooleanSetting onlyBlocks = new BooleanSetting("Only Blocks", false);

    private int cooldown = 0;
    private int lastReplenishedSlot = -1;
    private int moveStep = 0;
    private int moveFromSlot = -1;
    private int moveToSlot = -1;
    private int moveCooldown = 0;

    public AutoReplenish() {
        super("AutoReplenish", "Automatically moves the next stack of the same item into your hotbar when you use the last one.", Category.MISC);
        this.addSettings(this.threshold, this.onlyHotbar, this.onlyBlocks);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cooldown = 0;
        lastReplenishedSlot = -1;
        moveStep = 0;
        moveFromSlot = -1;
        moveToSlot = -1;
        moveCooldown = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }


        if (moveStep > 0) {
            if (moveCooldown > 0) {
                moveCooldown--;
                return;
            }

            var handler = mc.player.playerScreenHandler;
            int syncId = handler.syncId;

            switch (moveStep) {
                case 1 -> {

                    mc.execute(() -> mc.interactionManager.clickSlot(syncId, moveFromSlot, 0, SlotActionType.PICKUP, mc.player));
                    moveStep = 2;
                    moveCooldown = 2;
                }
                case 2 -> {

                    mc.execute(() -> mc.interactionManager.clickSlot(syncId, moveToSlot, 0, SlotActionType.PICKUP, mc.player));
                    moveStep = 3;
                    moveCooldown = 2;
                }
                case 3 -> {

                    mc.execute(() -> mc.interactionManager.clickSlot(syncId, moveFromSlot, 0, SlotActionType.PICKUP, mc.player));
                    moveStep = 0;
                    cooldown = 5;
                }
            }
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int currentSlot = mc.player.getInventory().selectedSlot;
        ItemStack heldItem = mc.player.getMainHandStack();


        if (heldItem.isEmpty() || heldItem.getCount() <= threshold.getValue().intValue()) {


            int replacementSlot = -1;

            if (!heldItem.isEmpty()) {

                replacementSlot = findReplacementSlot(heldItem, currentSlot);
            } else {

                for (int i = 0; i < 9; i++) {
                    if (i == currentSlot) continue;
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty()) {
                        replacementSlot = findReplacementSlot(stack, currentSlot);
                        if (replacementSlot != -1) break;
                    }
                }
            }

            if (replacementSlot != -1) {

                if (replacementSlot < 9) {
                    if (replacementSlot != currentSlot) {
                        mc.player.getInventory().selectedSlot = replacementSlot;
                        cooldown = 5;
                    }
                } else {

                    startMoveToHotbarSlot(replacementSlot, currentSlot);
                }
                lastReplenishedSlot = currentSlot;
            }
        } else if (lastReplenishedSlot == currentSlot && heldItem.getCount() > threshold.getValue().intValue()) {

            lastReplenishedSlot = -1;
        }
    }

    private int findReplacementSlot(ItemStack targetStack, int currentSlot) {

        if (targetStack.isEmpty()) {
            return -1;
        }


        if (onlyBlocks.getValue() && !(targetStack.getItem() instanceof net.minecraft.item.BlockItem)) {
            return -1;
        }


        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) {
                continue;
            }
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (areItemsEqual(stack, targetStack)) {
                return i;
            }
        }


        if (!onlyHotbar.getValue()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (areItemsEqual(stack, targetStack)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean areItemsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }


        return stack1.getItem() == stack2.getItem();
    }

    private int convertSlotIndex(int slotIndex) {
        if (slotIndex < 9) {
            return 36 + slotIndex;
        }
        return slotIndex;
    }

    private void startMoveToHotbarSlot(int inventorySlot, int hotbarSlot) {
        if (inventorySlot < 9 || inventorySlot >= 36 || hotbarSlot < 0 || hotbarSlot >= 9) {
            return;
        }

        if (mc.player.playerScreenHandler == null) {
            return;
        }


        moveFromSlot = inventorySlot;
        moveToSlot = 36 + hotbarSlot;
        moveStep = 1;
        moveCooldown = 0;
    }
}


