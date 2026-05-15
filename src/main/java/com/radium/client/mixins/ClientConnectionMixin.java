package com.radium.client.mixins;
// radium client

import com.radium.client.events.EventManager;
import com.radium.client.events.event.PacketReceiveListener;
import com.radium.client.events.event.PacketSendListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onPacketReceive(Packet<T> packet, T listener, CallbackInfo ci) {
        PacketReceiveListener.PacketReceiveEvent event = new PacketReceiveListener.PacketReceiveEvent(packet);
        EventManager.fire(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        PacketSendListener.PacketSendEvent event = new PacketSendListener.PacketSendEvent(packet);
        EventManager.fire(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}

