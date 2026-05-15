package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;

public interface PacketSendListener extends Listener {
    void onPacketSend(PacketSendEvent event);

    class PacketSendEvent extends CancellableEvent<PacketSendListener> {
        public Packet<?> packet;

        public PacketSendEvent(Packet<?> packet) {
            this.packet = packet;
        }

        @Override
        public void fire(ArrayList<PacketSendListener> listeners) {
            for (PacketSendListener listener : listeners) {
                listener.onPacketSend(this);
            }
        }

        @Override
        public Class<PacketSendListener> getListenerType() {
            return PacketSendListener.class;
        }
    }
}

