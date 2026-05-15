package com.radium.client.modules.combat;
// radium client

import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.KeybindSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.InventoryUtil;
import com.radium.client.utils.KeyUtils;
import net.minecraft.item.Items;

import static com.radium.client.client.RadiumClient.eventManager;

public final class PearlThrow extends Module implements GameRenderListener, TickListener {
    private final KeybindSetting triggerKey = new KeybindSetting("Trigger Key", -1);
    private boolean isActivated;
    private boolean hasThrown;
    private int currentThrowDelay;
    private int previousSlot;
    private int currentSwitchBackDelay;

    public PearlThrow() {
        super("PearlThrow", "Throws an ender pearl on Activate.", Category.COMBAT);
        this.addSettings(this.triggerKey);
    }

    @Override
    public void onEnable() {
        this.resetState();
        super.onEnable();
        eventManager.add(GameRenderListener.class, this);
        eventManager.add(TickListener.class, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(GameRenderListener.class, this);
        eventManager.add(TickListener.class, this);
        if (mc != null && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
    }

    @Override
    public void onTick2() {
        if (mc.currentScreen != null) {
            return;
        }
        if (KeyUtils.isKeyPressed(this.triggerKey.getValue())) {
            this.isActivated = true;
        }
        if (this.isActivated) {
            if (this.previousSlot == -1) {
                this.previousSlot = mc.player.getInventory().selectedSlot;
            }
            InventoryUtil.swap(Items.ENDER_PEARL);
            if (!this.hasThrown) {
                mc.options.useKey.setPressed(true);
                this.hasThrown = true;
                this.currentSwitchBackDelay = 0;
                return;
            }
            mc.options.useKey.setPressed(false);
            this.currentSwitchBackDelay++;
            if (this.currentSwitchBackDelay >= 1) {
                this.handleSwitchBack();
            }
        }
    }

    private void handleSwitchBack() {
        InventoryUtil.swap(this.previousSlot);
        this.resetState();
    }

    private void resetState() {
        this.previousSlot = -1;
        this.currentSwitchBackDelay = 0;
        this.currentThrowDelay = 0;
        this.isActivated = false;
        this.hasThrown = false;
        if (mc != null && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
    }

    @Override
    public void onGameRender(GameRenderListener.GameRenderEvent event) {

    }
}

