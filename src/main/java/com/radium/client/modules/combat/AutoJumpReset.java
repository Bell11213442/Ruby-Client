package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.PacketSendListener;
import com.radium.client.modules.Module;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class AutoJumpReset extends Module implements PacketSendListener {

    public AutoJumpReset() {
        super("AutoJumpReset", "Jumps when you take damage to reduce knockback.", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(PacketSendListener.class, this);
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(PacketSendListener.class, this);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == mc.player.getId()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }
}

