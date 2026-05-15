package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import com.radium.client.modules.misc.AutoReconnect;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class AutoLog extends Module {
    private final NumberSetting health = new NumberSetting("Health", 6.0, 0.0, 19.0, 1.0);

    private final BooleanSetting totemPops = new BooleanSetting("Totem Pop", true);

    private final BooleanSetting toggleOff = new BooleanSetting("Toggle Off", true);

    private final BooleanSetting toggleAutoReconnect = new BooleanSetting("Toggle Auto Reconnect", true);

    private int pops;

    public AutoLog() {
        super("AutoLog", "Automatically disconnects when certain requirements are met", Module.Category.COMBAT);
        Setting<?>[] settingArray = new Setting<?>[]{this.health, this.totemPops, this.toggleOff, this.toggleAutoReconnect};
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.pops = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        float playerHealth = mc.player.getHealth();

        if (playerHealth <= 0.0f) {
            this.toggle();
            return;
        }

        if (this.health.getValue() > 0.0 && playerHealth <= this.health.getValue()) {
            this.disconnect("Health was lower than " + this.health.getValue().intValue() + ".");
            if (this.toggleOff.getValue()) {
                this.toggle();
            }
        }
    }

    public void onPacketReceive(EntityStatusS2CPacket packet) {
        if (packet.getStatus() != 35) {
            return;
        }

        if (mc.player == null || mc.world == null) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);

        if (entity == null || !entity.equals(mc.player)) {
            return;
        }

        if (this.totemPops.getValue()) {
            this.disconnect("Popped totem.");
            if (this.toggleOff.getValue()) {
                this.toggle();
            }
        }
    }

    private void disconnect(String reason) {
        this.disconnect(Text.literal(reason));
    }

    private void disconnect(Text reason) {
        MutableText text = Text.literal("[AutoLog] ");
        text.append(reason);

        AutoReconnect autoReconnect = RadiumClient.getModuleManager().getModule(AutoReconnect.class);

        if (autoReconnect != null && autoReconnect.isEnabled() && this.toggleAutoReconnect.getValue()) {
            text.append(Text.literal("\n\nINFO - AutoReconnect was disabled").withColor(Colors.GRAY));
            autoReconnect.toggle();
        }

        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
    }
}

