package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;

public interface PacketReceiveListener extends Listener {
    void onPacketReceive(PacketReceiveEvent event);

    class PacketReceiveEvent extends CancellableEvent<PacketReceiveListener> {
        public Packet<?> packet;

        public PacketReceiveEvent(Packet<?> packet) {
            this.packet = packet;
        }

        @Override
        public void fire(ArrayList<PacketReceiveListener> listeners) {
            listeners.forEach(listener -> listener.onPacketReceive(this));
        }

        @Override
        public Class<PacketReceiveListener> getListenerType() {
            return PacketReceiveListener.class;
        }
    }
}

