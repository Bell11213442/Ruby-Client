package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class HoverTotem extends Module implements TickListener {
    private final BooleanSetting hotbarTotem = new BooleanSetting("Hotbar Totem", true);
    private final NumberSetting hotbarSlot = new NumberSetting("Hotbar Slot", 1.0, 1.0, 9.0, 1.0);
    private final BooleanSetting autoSwitchToTotem = new BooleanSetting("Auto Switch To Totem", false);
    private final BooleanSetting autoInvOpen = new BooleanSetting("Auto Inv Open", false);

    private boolean shouldOpenInv = false;
    private boolean totemEquipped = false;
    private boolean wasAutoOpened = false;

    public HoverTotem() {
        super("HoverTotem", "Equips a totem in offhand when hovered", Module.Category.COMBAT);
        this.addSettings(hotbarTotem, hotbarSlot, autoSwitchToTotem, autoInvOpen);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
        shouldOpenInv = false;
        totemEquipped = false;
        wasAutoOpened = false;
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
        shouldOpenInv = false;
        totemEquipped = false;
        wasAutoOpened = false;
    }

    @Override
    public void onTick2() {
        if (mc.player == null) return;

        if (autoInvOpen.getValue()) {
            if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && hasTotemInInventory()) {
                if (mc.currentScreen == null) {
                    shouldOpenInv = true;
                }
            }
        }

        if (shouldOpenInv && mc.currentScreen == null) {
            if (hasTotemInInventory()) {
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.setScreen(new InventoryScreen(mc.player));
                        shouldOpenInv = false;
                        totemEquipped = false;
                        wasAutoOpened = true;
                    }
                });
            } else {
                shouldOpenInv = false;
            }
            return;
        }

        Screen currentScreen = mc.currentScreen;
        if (!(currentScreen instanceof InventoryScreen inventoryScreen)) {
            if (wasAutoOpened && !totemEquipped && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                totemEquipped = true;
                wasAutoOpened = false;
            }

            if (wasAutoOpened && !totemEquipped) {
                shouldOpenInv = true;
            }
            return;
        }

        if (autoInvOpen.getValue() && !totemEquipped) {
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                totemEquipped = true;
                wasAutoOpened = false;
                mc.execute(() -> {
                    mc.player.closeHandledScreen();
                    mc.mouse.lockCursor();
                });
                return;
            }
        }

        Slot focusedSlot = getFocusedSlotSafe(inventoryScreen);

        if (focusedSlot == null || focusedSlot.getIndex() > 35) return;

        if (autoSwitchToTotem.getValue()) {
            mc.player.getInventory().selectedSlot = hotbarSlot.getValue().intValue() - 1;
        }

        if (!focusedSlot.getStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        int slotIndex = focusedSlot.getIndex();
        int syncId = inventoryScreen.getScreenHandler().syncId;

        if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            equipOffhandTotem(syncId, slotIndex);
            return;
        }

        if (hotbarTotem.getValue()) {
            int hotbarIndex = hotbarSlot.getValue().intValue() - 1;
            if (!mc.player.getInventory().getStack(hotbarIndex).isOf(Items.TOTEM_OF_UNDYING)) {
                equipHotbarTotem(syncId, slotIndex, hotbarIndex);
            }
        }
    }

    public void onPacketReceive(Object packet) {
        if (packet instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 35 && mc.player != null && statusPacket.getEntity(mc.world) == mc.player) {
                if (autoInvOpen.getValue() && mc.currentScreen == null && hasTotemInInventory()) {
                    shouldOpenInv = true;
                    totemEquipped = false;
                    wasAutoOpened = true;
                }
            }
        }
    }

    private boolean hasTotemInInventory() {
        if (mc.player == null) return false;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return true;
            }
        }

        return false;
    }

    private void equipOffhandTotem(int syncId, int slotIndex) {
        mc.interactionManager.clickSlot(syncId, slotIndex, 40, SlotActionType.SWAP, mc.player);
    }

    private void equipHotbarTotem(int syncId, int slotIndex, int hotbarIndex) {
        mc.interactionManager.clickSlot(syncId, slotIndex, hotbarIndex, SlotActionType.SWAP, mc.player);
    }

    private Slot getFocusedSlotSafe(InventoryScreen screen) {
        return ((com.radium.client.mixins.HandledScreenMixin) screen).radium$getFocusedSlot();
    }
}

