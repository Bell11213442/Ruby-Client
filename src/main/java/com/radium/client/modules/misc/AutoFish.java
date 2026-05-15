package com.radium.client.modules.misc;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.PacketReceiveListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.modules.Module;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public final class AutoFish extends Module implements PacketReceiveListener, TickListener {
    private int recatchDelay = 0;

    public AutoFish() {
        super("Auto Fish", "Automatically catches fish", Category.MISC);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(PacketReceiveListener.class, this);
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(PacketReceiveListener.class, this);
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick2() {
        if (recatchDelay > 0) {
            recatchDelay--;
            if (recatchDelay == 0) {
                if (mc.player != null && mc.player.getMainHandStack().getItem() == Items.FISHING_ROD) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.packet instanceof PlaySoundS2CPacket packet) {
            if (packet.getSound().value().equals(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH)) {
                if (mc.player != null && mc.player.fishHook != null) {
                    // Check if the sound is near the bobber
                    double dist = mc.player.fishHook.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ());
                    if (dist <= 1.0) {
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        recatchDelay = 20; // 1 second delay before recasting
                    }
                }
            }
        }
    }
}
