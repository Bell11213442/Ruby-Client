package com.radium.client.modules.misc;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ChestStealer extends Module implements TickListener {
    private final NumberSetting delay = new NumberSetting("Delay", 100, 0, 1000, 10);
    private long lastTime = 0;

    public ChestStealer() {
        super("Chest Stealer", "Automatically loots chests", Category.MISC);
        addSettings(delay);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick2() {
        if (mc.player == null) return;
        
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler instanceof GenericContainerScreenHandler container) {
            if (System.currentTimeMillis() - lastTime < delay.getValue()) return;

            for (int i = 0; i < container.getRows() * 9; i++) {
                Slot slot = container.getSlot(i);
                ItemStack stack = slot.getStack();
                
                if (!stack.isEmpty()) {
                    mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    lastTime = System.currentTimeMillis();
                    return;
                }
            }
            
            // If all items are taken, close the chest
            mc.player.closeHandledScreen();
        }
    }
}
