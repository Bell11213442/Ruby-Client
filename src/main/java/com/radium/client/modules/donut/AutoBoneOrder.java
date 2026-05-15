package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.ItemSetting;
import com.radium.client.gui.settings.SliderSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AutoBoneOrder extends Module {

    private final StringSetting orderName = new StringSetting("Order Name", "bones");
    private final ItemSetting orderItem = new ItemSetting("Order Item", Items.BONE);
    private final SliderSetting clickDelay = new SliderSetting("Click Delay (ticks)", 2, 1, 10, 1);
    private final SliderSetting guiTimeout = new SliderSetting("GUI Timeout (ticks)", 60, 20, 200, 5);
    private State state = State.IDLE;
    private BlockPos spawnerPos;
    private int waitTicks = 0;
    private int timeoutTicks = 0;
    private int doubleEscapeRemaining = 0;
    private boolean hasClickedConfirm = false;
    private float yaw;
    private float pitch;
    private boolean hasPressed;

    public AutoBoneOrder() {
        super("AutoBoneOrder", "Automates ordering bones", Category.DONUT);
        addSettings(orderName, orderItem, clickDelay, guiTimeout);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            toggle();
            return;
        }
        resetState();
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        spawnerPos = null;
        waitTicks = 0;
        timeoutTicks = 0;
        doubleEscapeRemaining = 0;
        hasClickedConfirm = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            toggle();
            return;
        }

        if (mc.currentScreen instanceof GameMenuScreen) {
            mc.player.closeHandledScreen();
            resetState();
            return;
        }

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (hasPressed) {
            hasPressed = false;
            mc.options.useKey.setPressed(false);
        }

        switch (state) {
            case FINDING_SPAWNER -> findSpawner();
            case OPENING_SPAWNER -> openSpawner();
            case WAITING_SPAWNER_GUI -> waitSpawnerGui();
            case LOOTING_BONES -> lootBones();
            case CLOSING_SPAWNER -> closeSpawner();
            case ORDER_COMMAND -> sendOrderCommand();
            case WAIT_ORDER_GUI -> waitOrderGui();
            case SELECT_ORDER_ITEM -> selectOrderItem();
            case WAIT_DELIVERY_GUI -> waitDeliveryGui();
            case DELIVERING_BONES -> deliverBones();
            case WAIT_AFTER_DELIVERY_1 -> waitAfterDelivery1();
            case CLOSING_DELIVERY -> closeDelivery();
            case WAIT_AFTER_CLOSE_DELIVERY -> waitAfterCloseDelivery();
            case WAIT_CONFIRM_GUI -> waitConfirmGui();
            case WAIT_CONFIRM_SETTLE -> waitConfirmSettle();
            case CLICK_CONFIRM_SLOT -> clickConfirmSlot();
            case WAIT_AFTER_CONFIRM_1 -> waitAfterConfirm1();
            case WAIT_AFTER_CONFIRM_2 -> waitAfterConfirm2();
            case WAIT_AFTER_CONFIRM_3 -> waitAfterConfirm3();
            case DOUBLE_ESCAPE -> performDoubleEscape();
            case DOUBLE_RIGHTCLICK_FIRST -> doubleRightClickFirst();
            case DOUBLE_RIGHTCLICK_SECOND -> doubleRightClickSecond();
            case POST_CYCLE_DELAY -> postCycleDelay();
            case IDLE -> {
            }
        }
    }

    private void resetState() {
        state = State.FINDING_SPAWNER;
        spawnerPos = null;
        waitTicks = 0;
        timeoutTicks = guiTimeout.getValue().intValue();
        doubleEscapeRemaining = 0;
        hasClickedConfirm = false;
        if (mc.player != null) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
        hasPressed = false;
    }

    private void findSpawner() {
        if (setSpawnerFromCrosshairOrKeep()) {
            state = State.OPENING_SPAWNER;
            waitTicks = clickDelay.getValue().intValue();
        } else {
            toggle();
        }
    }

    private void openSpawner() {
        if (spawnerPos == null) {
            state = State.FINDING_SPAWNER;
            return;
        }
        mc.options.useKey.setPressed(true);
        hasPressed = true;
        state = State.WAITING_SPAWNER_GUI;
        timeoutTicks = guiTimeout.getValue().intValue();
        waitTicks = clickDelay.getValue().intValue();
    }

    private void waitSpawnerGui() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            state = State.LOOTING_BONES;
            waitTicks = 0;
            return;
        }
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            toggle();
        }
    }

    private void lootBones() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        if (isInventoryFull()) {
            state = State.CLOSING_SPAWNER;
            waitTicks = clickDelay.getValue().intValue();
            return;
        }

        int movedCount = 0;
        for (int slot = 0; slot < mc.player.currentScreenHandler.slots.size(); slot++) {
            if (slot >= 36) continue;
            ItemStack stack = mc.player.currentScreenHandler.getSlot(slot).getStack();
            if (!stack.isEmpty() && stack.isOf(Items.BONE)) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
                movedCount++;
                if (movedCount >= 3) {
                    waitTicks = 1;
                    return;
                }
            }
        }

        if (movedCount == 0) {
            state = State.CLOSING_SPAWNER;
            waitTicks = clickDelay.getValue().intValue();
        } else {
            waitTicks = 1;
        }
    }

    private void closeSpawner() {
        mc.player.closeHandledScreen();
        state = State.ORDER_COMMAND;
        waitTicks = clickDelay.getValue().intValue() * 2;
    }

    private void sendOrderCommand() {
        mc.player.networkHandler.sendChatCommand("order " + orderName.getValue());
        state = State.WAIT_ORDER_GUI;
        timeoutTicks = guiTimeout.getValue().intValue();
        waitTicks = clickDelay.getValue().intValue();
    }

    private void waitOrderGui() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            state = State.SELECT_ORDER_ITEM;
            waitTicks = clickDelay.getValue().intValue();
            return;
        }
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            toggle();
        }
    }

    private void selectOrderItem() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        boolean clicked = false;
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.isOf(orderItem.getItem())) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                clicked = true;
                waitTicks = clickDelay.getValue().intValue() * 2;
                break;
            }
        }

        if (clicked) {
            state = State.WAIT_DELIVERY_GUI;
            timeoutTicks = guiTimeout.getValue().intValue();
        } else {
            toggle();
        }
    }

    private void waitDeliveryGui() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            boolean hasPlayerInventorySlots = false;
            for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
                if (mc.player.currentScreenHandler.getSlot(i).inventory == mc.player.getInventory()) {
                    hasPlayerInventorySlots = true;
                    break;
                }
            }
            if (hasPlayerInventorySlots) {
                state = State.DELIVERING_BONES;
                waitTicks = 0;
                return;
            }
        }
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            toggle();
        }
    }

    private void deliverBones() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        int movedCount = 0;
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (mc.player.currentScreenHandler.getSlot(i).inventory == mc.player.getInventory()) {
                ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
                if (!stack.isEmpty() && stack.isOf(Items.BONE)) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    movedCount++;
                    if (movedCount >= 3) {
                        waitTicks = 1;
                        return;
                    }
                }
            }
        }

        if (movedCount == 0) {
            state = State.WAIT_AFTER_DELIVERY_1;
            waitTicks = 5;
        } else {
            waitTicks = 1;
        }
    }

    private void waitAfterDelivery1() {
        state = State.CLOSING_DELIVERY;
        waitTicks = 5;
    }

    private void closeDelivery() {
        if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
        state = State.WAIT_AFTER_CLOSE_DELIVERY;
        waitTicks = 5;
        hasClickedConfirm = false;
    }

    private void waitAfterCloseDelivery() {
        state = State.WAIT_CONFIRM_GUI;
        timeoutTicks = guiTimeout.getValue().intValue();
        waitTicks = 5;
    }

    private void waitConfirmGui() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            state = State.WAIT_CONFIRM_SETTLE;
            waitTicks = 5;
            return;
        }
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            toggle();
        }
    }

    private void waitConfirmSettle() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            state = State.WAIT_CONFIRM_GUI;
            return;
        }

        if (mc.player.currentScreenHandler.slots.size() > 15) {
            ItemStack slot15 = mc.player.currentScreenHandler.getSlot(15).getStack();
            if (slot15.isOf(Items.LIME_STAINED_GLASS_PANE)) {
                state = State.CLICK_CONFIRM_SLOT;
                waitTicks = 5;
            } else {
                waitTicks = 5;
            }
        }
    }

    private void clickConfirmSlot() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        if (!hasClickedConfirm && mc.player.currentScreenHandler.slots.size() > 15) {
            ItemStack slot15 = mc.player.currentScreenHandler.getSlot(15).getStack();
            if (slot15.isOf(Items.LIME_STAINED_GLASS_PANE)) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
                hasClickedConfirm = true;
            }
        }

        state = State.WAIT_AFTER_CONFIRM_1;
        waitTicks = 5;
    }

    private void waitAfterConfirm1() {
        state = State.WAIT_AFTER_CONFIRM_2;
        waitTicks = 5;
    }

    private void waitAfterConfirm2() {
        state = State.WAIT_AFTER_CONFIRM_3;
        waitTicks = 5;
    }

    private void waitAfterConfirm3() {
        state = State.DOUBLE_ESCAPE;
        doubleEscapeRemaining = 2;
        waitTicks = 5;
    }

    private boolean isInventoryFull() {
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void performDoubleEscape() {
        if (doubleEscapeRemaining > 0) {
            if (mc.currentScreen != null) {
                mc.player.closeHandledScreen();
            }
            doubleEscapeRemaining--;
            waitTicks = 5;
            return;
        }
        state = State.DOUBLE_RIGHTCLICK_FIRST;
        waitTicks = 5;
    }

    private void doubleRightClickFirst() {
        if (!setSpawnerFromCrosshairOrKeep()) {
            toggle();
            return;
        }
        mc.options.useKey.setPressed(true);
        hasPressed = true;
        state = State.DOUBLE_RIGHTCLICK_SECOND;
        waitTicks = 5;
    }

    private void doubleRightClickSecond() {
        if (!setSpawnerFromCrosshairOrKeep()) {
            toggle();
            return;
        }
        mc.options.useKey.setPressed(true);
        hasPressed = true;
        state = State.POST_CYCLE_DELAY;
        waitTicks = 5;
    }

    private void postCycleDelay() {
        state = State.FINDING_SPAWNER;
        timeoutTicks = guiTimeout.getValue().intValue();
        waitTicks = 20;
    }

    private boolean setSpawnerFromCrosshairOrKeep() {
        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            BlockPos targetPos = bhr.getBlockPos();
            if (mc.world.getBlockState(targetPos).isOf(Blocks.SPAWNER)) {
                spawnerPos = targetPos.toImmutable();
                return true;
            }
        }
        return spawnerPos != null && mc.world.getBlockState(spawnerPos).isOf(Blocks.SPAWNER);
    }

    private enum State {
        IDLE,
        FINDING_SPAWNER,
        OPENING_SPAWNER,
        WAITING_SPAWNER_GUI,
        LOOTING_BONES,
        CLOSING_SPAWNER,
        ORDER_COMMAND,
        WAIT_ORDER_GUI,
        SELECT_ORDER_ITEM,
        WAIT_DELIVERY_GUI,
        DELIVERING_BONES,
        WAIT_AFTER_DELIVERY_1,
        CLOSING_DELIVERY,
        WAIT_AFTER_CLOSE_DELIVERY,
        WAIT_CONFIRM_GUI,
        WAIT_CONFIRM_SETTLE,
        CLICK_CONFIRM_SLOT,
        WAIT_AFTER_CONFIRM_1,
        WAIT_AFTER_CONFIRM_2,
        WAIT_AFTER_CONFIRM_3,
        DOUBLE_ESCAPE,
        DOUBLE_RIGHTCLICK_FIRST,
        DOUBLE_RIGHTCLICK_SECOND,
        POST_CYCLE_DELAY
    }
}
