package com.radium.client.utils.Character;
// radium client

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class CenterCharacter {
    private static final double TOLERANCE = 0.15;
    private final MinecraftClient mc;
    private boolean isActive = false;
    private BlockPos targetBlock = null;
    private int tickCount = 0;

    public CenterCharacter(MinecraftClient mc) {
        this.mc = mc;
    }

    public boolean initiate() {
        if (mc.player == null)
            return false;

        BlockPos playerBlock = mc.player.getBlockPos();
        Vec3d playerPos = mc.player.getPos();

        double offsetX = playerPos.x - (playerBlock.getX() + 0.5);
        double offsetZ = playerPos.z - (playerBlock.getZ() + 0.5);

        if (Math.abs(offsetX) < TOLERANCE && Math.abs(offsetZ) < TOLERANCE) {
            return false;
        }

        targetBlock = playerBlock;
        isActive = true;
        tickCount = 0;
        return true;
    }

    public boolean update() {
        if (!isActive || mc.player == null || targetBlock == null) {
            return false;
        }

        tickCount++;

        Vec3d playerPos = mc.player.getPos();
        double targetX = targetBlock.getX() + 0.5;
        double targetZ = targetBlock.getZ() + 0.5;

        double worldOffsetX = playerPos.x - targetX;
        double worldOffsetZ = playerPos.z - targetZ;

        if (Math.abs(worldOffsetX) < TOLERANCE && Math.abs(worldOffsetZ) < TOLERANCE) {
            haltCentering();
            return false;
        }

        stopMovement();

        boolean shouldTap = (tickCount % 2 == 0);
        if (!shouldTap) {
            return true;
        }

        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);

        double moveX = -worldOffsetX;
        double moveZ = -worldOffsetZ;

        double relativeForward = moveX * (-Math.sin(yawRad)) + moveZ * Math.cos(yawRad);
        double relativeStrafe = moveX * (-Math.cos(yawRad)) + moveZ * (-Math.sin(yawRad));

        if (Math.abs(relativeForward) > TOLERANCE * 0.5) {
            if (relativeForward > 0) {
                mc.options.forwardKey.setPressed(true);
            } else {
                mc.options.backKey.setPressed(true);
            }
        }

        if (Math.abs(relativeStrafe) > TOLERANCE * 0.5) {
            if (relativeStrafe > 0) {
                mc.options.rightKey.setPressed(true);
            } else {
                mc.options.leftKey.setPressed(true);
            }
        }

        if (tickCount > 100) {
            haltCentering();
            return false;
        }

        return true;
    }

    public void haltCentering() {
        isActive = false;
        targetBlock = null;
        stopMovement();
        tickCount = 0;
    }

    private void stopMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    public boolean isCentering() {
        return isActive;
    }
}
