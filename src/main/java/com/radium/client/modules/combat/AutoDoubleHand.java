package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AutoDoubleHand extends Module implements TickListener {
    private final BooleanSetting onPop = new BooleanSetting("On Pop", true);
    private final BooleanSetting onHealth = new BooleanSetting("On Health", true);
    private final NumberSetting healthThreshold = new NumberSetting("Health", 10.0, 0.0, 20.0, 1.0);

    private int previousSlot = -1;
    private float lastHealth = 0.0f;
    private boolean hasTriggered = false;

    public AutoDoubleHand() {
        super("AutoDoubleHand", "Automatically switches to totems on pop or low health", Module.Category.COMBAT);
        Setting<?>[] settingArray = new Setting<?>[]{this.onPop, this.onHealth, this.healthThreshold};
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
        this.previousSlot = -1;
        this.lastHealth = 0.0f;
        this.hasTriggered = false;
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
        this.previousSlot = -1;
        this.lastHealth = 0.0f;
        this.hasTriggered = false;
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (this.onHealth.getValue()) {
            float playerHealth = mc.player.getHealth();

            if (playerHealth <= this.healthThreshold.getValue() && !this.hasTriggered) {
                switchToTotem();
                this.hasTriggered = true;
            } else if (playerHealth > this.healthThreshold.getValue()) {
                this.hasTriggered = false;
            }

            this.lastHealth = playerHealth;
        }
    }

    public void onPacketReceive(EntityStatusS2CPacket packet) {
        if (packet.getStatus() != 35 || mc.player == null || mc.world == null) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);

        if (entity == null || !entity.equals(mc.player) || !this.onPop.getValue()) {
            return;
        }

        switchToTotem();
    }

    private void switchToTotem() {
        if (mc.player == null) {
            return;
        }

        Module autoTotem = RadiumClient.getModuleManager().getModule("AutoTotem");
        boolean useSecondTotem = autoTotem != null && autoTotem.isEnabled();

        int totemSlot = useSecondTotem ? findSecondTotemInHotbar() : findTotemInHotbar();
        if (totemSlot == -1) {
            return;
        }

        if (this.previousSlot == -1) {
            this.previousSlot = mc.player.getInventory().selectedSlot;
        }

        mc.player.getInventory().selectedSlot = totemSlot;
    }

    private int findSecondTotemInHotbar() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count++;
                if (count == 2) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findTotemInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }
}

