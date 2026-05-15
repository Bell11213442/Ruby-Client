package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.mixins.MinecraftClientAccessor;
import com.radium.client.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class MaceTrigger extends Module implements TickListener {

    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.1);
    private final NumberSetting attackDelay = new NumberSetting("Attack Delay", 3, 0, 10, 1);
    private final NumberSetting fovCheck = new NumberSetting("FOV Check", 30.0, 0.0, 180.0, 5.0);

    private int attackCooldown = 0;

    public MaceTrigger() {
        super("MaceTriggerBot", "Automatically attacks players in your crosshair when holding a mace", Category.COMBAT);
        addSettings(range, attackDelay, fovCheck);
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.world == null) return;


        if (!isHoldingMace()) {
            attackCooldown = 0;
            return;
        }


        Entity entity = null;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
        }


        if (entity instanceof PlayerEntity target && entity != mc.player) {

            if (isTargetValid(target)) {

                if (attackCooldown <= 0) {
                    ((MinecraftClientAccessor) mc).invokeDoAttack();
                    attackCooldown = attackDelay.getValue().intValue();
                } else {
                    attackCooldown--;
                }
            }
        }
    }

    private boolean isHoldingMace() {
        return mc.player != null && mc.player.getMainHandStack().getItem() == Items.MACE;
    }

    private boolean isTargetValid(PlayerEntity target) {

        double distance = mc.player != null ? mc.player.distanceTo(target) : Double.MAX_VALUE;
        if (distance > range.getValue()) return false;


        if (!target.isAlive()) return false;


        if (fovCheck.getValue() > 0) {
            if (mc.player == null) return false;

            Vec3d playerLook = mc.player.getRotationVec(1.0F);
            Vec3d toTarget = target.getPos().subtract(mc.player.getPos()).normalize();

            double angle = Math.toDegrees(Math.acos(playerLook.dotProduct(toTarget)));
            return !(angle > fovCheck.getValue());
        }

        return true;
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        attackCooldown = 0;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
    }
}

