package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.SliderSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoSell extends Module {

    private final SliderSetting delay = new SliderSetting("Click Delay (ticks)", 2, 1, 10, 1);
    private State currentState = State.IDLE;
    private int currentSlotIndex = 0;
    private int guiOpenTimeout = 0;
    private int waitTicks = 0;

    public AutoSell() {
        super("AutoSell", "Automatically sells all items in your inventory.", Category.DONUT);
        addSettings(delay);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        currentState = State.OPENING_GUI;
        mc.player.networkHandler.sendChatCommand("sell");
        guiOpenTimeout = 40;
    }

    @Override
    public void onDisable() {
        currentState = State.IDLE;
        currentSlotIndex = 0;
        guiOpenTimeout = 0;
        waitTicks = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) {
            mc.execute(this::toggle);
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        switch (currentState) {
            case OPENING_GUI:
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    currentState = State.DEPOSITING;


                    currentSlotIndex = mc.player.playerScreenHandler.slots.size() - 36;
                } else {
                    guiOpenTimeout--;
                    if (guiOpenTimeout <= 0) {
                        ChatUtils.e("Timed out waiting for sell GUI.");
                        mc.execute(this::toggle);
                    }
                }
                break;

            case DEPOSITING:
                if (!(mc.currentScreen instanceof GenericContainerScreen)) {
                    ChatUtils.e("Sell GUI closed unexpectedly.");
                    mc.execute(this::toggle);
                    return;
                }


                int nextItemSlot = -1;
                for (int i = currentSlotIndex; i < mc.player.playerScreenHandler.slots.size(); i++) {
                    if (mc.player.playerScreenHandler.getSlot(i).inventory == mc.player.getInventory()) {
                        ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
                        if (!stack.isEmpty()) {
                            nextItemSlot = i;
                            break;
                        }
                    }
                }

                if (nextItemSlot != -1) {
                    final int slotToClick = nextItemSlot;

                    mc.execute(() -> {

                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotToClick, 0, SlotActionType.QUICK_MOVE, mc.player);
                    });

                    currentSlotIndex = nextItemSlot + 1;
                    waitTicks = delay.getValue().intValue();
                } else {

                    currentState = State.CLOSING;
                }
                break;

            case CLOSING:
                mc.execute(() -> {
                    if (mc.currentScreen != null) {
                        mc.player.closeHandledScreen();
                    }
                    ChatUtils.m("Finished selling items.");
                    toggle();
                });
                currentState = State.IDLE;
                break;
        }
    }

    private enum State {
        IDLE,
        OPENING_GUI,
        DEPOSITING,
        CLOSING
    }
}


