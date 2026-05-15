package com.radium.client.modules.combat;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.PacketSendListener;
import com.radium.client.modules.Module;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class Blink extends Module implements PacketSendListener {
    private final List<Packet<?>> packets = new ArrayList<>();

    public Blink() {
        super("Blink", "Suspends movement packets and sends them all at once", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(PacketSendListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(PacketSendListener.class, this);
        if (mc.player != null && mc.getNetworkHandler() != null) {
            synchronized (packets) {
                for (Packet<?> packet : packets) {
                    mc.getNetworkHandler().sendPacket(packet);
                }
                packets.clear();
            }
        }
        super.onDisable();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            event.cancel();
            synchronized (packets) {
                packets.add(event.packet);
            }
        }
    }
}
