package com.radium.client.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import static com.radium.client.client.RadiumClient.mc;

public class CombatUtils {
    public static boolean isShieldFacingAway(PlayerEntity target) {
        if (mc.player == null || target == null) return false;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = target.getPos();

        Vec3d toPlayer = playerPos.subtract(targetPos);
        Vec3d toPlayerHorizontal = new Vec3d(toPlayer.x, 0.0, toPlayer.z);
        if (toPlayerHorizontal.lengthSquared() == 0.0) return false;
        toPlayerHorizontal = toPlayerHorizontal.normalize();


        final double yaw = Math.toRadians(target.getYaw());
        final double pitch = Math.toRadians(target.getPitch());

        Vec3d facing = new Vec3d(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        ).normalize();

        double dot = facing.dotProduct(toPlayerHorizontal);
        return dot < 0.0;
    }
}
