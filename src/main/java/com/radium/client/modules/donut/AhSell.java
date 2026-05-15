package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AhSell extends Module {

    private static final int CONFIRM_SLOT = 15;
    private final StringSetting sellPrice = new StringSetting("Sell Price", "15000");
    private final NumberSetting delay = new NumberSetting("Delay (ticks)", 20.0, 5.0, 100.0, 1.0);
    private int cooldown = 0;
    private State currentState = State.IDLE;
    private int guiActionDelay = 0;


    public AhSell() {
        super("AhSell", "Automatically sells items from your hotbar.", Category.DONUT);
        addSettings(sellPrice, delay);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }


        if (currentState == State.WAITING_FOR_GUI) {
            guiActionDelay--;
            if (mc.currentScreen instanceof GenericContainerScreen) {
                currentState = State.CLICKING_CONFIRM;

                guiActionDelay = 2;
            } else if (guiActionDelay <= 0) {

                resetToIdle();
            }
            return;
        }

        if (currentState == State.CLICKING_CONFIRM) {
            guiActionDelay--;
            if (guiActionDelay > 0) return;

            if (mc.currentScreen instanceof GenericContainerScreen) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, CONFIRM_SLOT, 0, SlotActionType.PICKUP, mc.player);
                mc.player.closeHandledScreen();
            }

            resetToIdle();
            return;
        }


        if (currentState == State.IDLE) {

            int sellableSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    sellableSlot = i;
                    break;
                }
            }

            if (sellableSlot != -1) {

                try {

                    mc.player.getInventory().selectedSlot = sellableSlot;

                    Integer.parseInt(sellPrice.getValue());
                    mc.player.networkHandler.sendChatCommand("ah sell " + sellPrice.getValue());


                    currentState = State.WAITING_FOR_GUI;
                    guiActionDelay = 10;
                } catch (NumberFormatException e) {
                    ChatUtils.e("Invalid sell price. Disabling.");
                    this.toggle();
                }
            } else {
                ChatUtils.m("Hotbar is empty. Disabling.");
                this.toggle();
            }
        }
    }

    private void resetToIdle() {
        currentState = State.IDLE;
        cooldown = delay.getValue().intValue();
    }

    @Override
    public void onEnable() {

        resetToIdle();
    }

    @Override
    public void onDisable() {

        resetToIdle();
    }

    private enum State {
        IDLE,
        WAITING_FOR_GUI,
        CLICKING_CONFIRM
    }
}


