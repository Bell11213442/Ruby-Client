package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.donut.TunnelBaseFinder;
import com.radium.client.modules.misc.BaseDigger;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 0, 0, 5, 1);
    private final NumberSetting lowTotemThreshold = new NumberSetting("Low Totem Alert", 3.0, 1.0, 10.0, 1.0);
    private int delayCounter = 0;
    private int lastTotemCount = -1;

    public AutoTotem() {
        super("AutoTotem", "Automatically equips a totem to your off-hand.", Category.COMBAT);
        this.addSettings(delay, lowTotemThreshold);
    }

    @Override
    public void onTick() {
        if (RadiumClient.moduleManager.getModule(TunnelBaseFinder.class).isEnabled()) {
            return;
        }
        if (RadiumClient.moduleManager.getModule(BaseDigger.class).isEnabled() && RadiumClient.moduleManager.getModule(BaseDigger.class).diggingState != BaseDigger.DiggingState.NONE) {
            return;
        }

        if (mc.player == null || mc.interactionManager == null) {
            return;
        }


        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            delayCounter = delay.getValue().intValue();
            return;
        }


        if (delayCounter > 0) {
            delayCounter--;
            return;
        }


        int totemSlot = findItemSlot(Items.TOTEM_OF_UNDYING);

        if (totemSlot != -1) {


            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    convertSlotIndex(totemSlot),
                    40,
                    SlotActionType.SWAP,
                    mc.player
            );
            delayCounter = delay.getValue().intValue();
        }

        // Check for low totems and show toast notification
        int totemCount = countTotems();
        if (totemCount != lastTotemCount && totemCount >= 0 && totemCount <= lowTotemThreshold.getValue().intValue()) {
            lastTotemCount = totemCount;

            if (RadiumClient.getModuleManager() != null) {
                com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            "Low Totems!",
                            "You have " + totemCount + " totem(s) remaining",
                            com.radium.client.utils.ToastNotification.ToastType.LOW_TOTEM
                    );
                }
            }
        } else if (totemCount != lastTotemCount) {
            lastTotemCount = totemCount;
        }
    }


    private int findItemSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }


    private int convertSlotIndex(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 9) {

            return 36 + slotIndex;
        }
        if (slotIndex >= 9 && slotIndex < 36) {

            return slotIndex;
        }
        return slotIndex;
    }

    /**
     * Count total totems in inventory (including offhand)
     */
    private int countTotems() {
        if (mc.player == null) return 0;

        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }

        // Also check offhand
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            count += mc.player.getOffHandStack().getCount();
        }

        return count;
    }
}

