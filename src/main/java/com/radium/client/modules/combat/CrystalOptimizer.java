package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.PacketSendListener;
import com.radium.client.modules.Module;
import com.radium.client.utils.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class CrystalOptimizer extends Module implements PacketSendListener {

    public CrystalOptimizer() {
        super("Crystal Optimizer", "Marlowww based crystal optimizer - crystals faster", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(PacketSendListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(PacketSendListener.class, this);
        super.onDisable();
    }

    @Override
    public void onPacketSend(PacketSendListener.PacketSendEvent event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            if (mc.crosshairTarget instanceof EntityHitResult hit) {
                if (hit.getType() == HitResult.Type.ENTITY && hit.getEntity() instanceof EndCrystalEntity crystal) {
                    boolean weakness = mc.player.hasStatusEffect(StatusEffects.WEAKNESS);
                    boolean strength = mc.player.hasStatusEffect(StatusEffects.STRENGTH);
                    int weaknessLvl = weakness ? mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() : -1;
                    int strengthLvl = strength ? mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() : -1;

                    if (weakness && (strengthLvl <= weaknessLvl) && !WorldUtil.isTool(mc.player.getMainHandStack())) {
                        return;
                    }


                    crystal.discard();
                    crystal.setRemoved(Entity.RemovalReason.KILLED);
                    crystal.onRemoved();
                }
            }
        }
    }
}

