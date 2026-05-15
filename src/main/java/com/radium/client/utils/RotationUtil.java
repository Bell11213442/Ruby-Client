package com.radium.client.utils;
// radium client

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();


    private static boolean usingSilentRotation = false;
    private static float silentYaw = 0.0f;
    private static float silentPitch = 0.0f;
    private static float previousYaw = 0.0f;
    private static float previousPitch = 0.0f;


    public static float[] getRotationsTo(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;

        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(diffY, distance) * 180.0 / Math.PI);

        return new float[]{yaw, pitch};
    }


    public static float[] getRotationsToEntity(Entity entity) {
        if (mc.player == null) {
            return new float[]{0.0f, 0.0f};
        }
        return getRotationsTo(mc.player.getEyePos(), entity.getPos().add(0, entity.getHeight() / 2, 0));
    }


    public static float[] getRotationsTo(Vec3d to) {
        if (mc.player == null) {
            return new float[]{0.0f, 0.0f};
        }
        return getRotationsTo(mc.player.getEyePos(), to);
    }


    public static void setSilentRotation(float yaw, float pitch) {
        if (mc.player == null) return;

        if (!usingSilentRotation) {
            previousYaw = mc.player.getYaw();
            previousPitch = mc.player.getPitch();
            usingSilentRotation = true;
        }

        silentYaw = normalizeYaw(yaw);
        silentPitch = MathHelper.clamp(pitch, -90.0f, 90.0f);


    }


    public static void resetSilentRotation() {
        if (mc.player == null || !usingSilentRotation) return;

        mc.player.setYaw(previousYaw);
        mc.player.setPitch(previousPitch);

        usingSilentRotation = false;
        silentYaw = 0.0f;
        silentPitch = 0.0f;
    }


    public static float getSilentYaw() {
        if (usingSilentRotation) {
            return silentYaw;
        }
        return mc.player != null ? mc.player.getYaw() : 0.0f;
    }


    public static float getSilentPitch() {
        if (usingSilentRotation) {
            return silentPitch;
        }
        return mc.player != null ? mc.player.getPitch() : 0.0f;
    }


    public static boolean isUsingSilentRotation() {
        return usingSilentRotation;
    }


    public static float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw >= 180.0f) {
            yaw -= 360.0f;
        }
        if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }


    public static float getAngleDifference(float a, float b) {
        float diff = Math.abs(a - b) % 360.0f;
        if (diff > 180.0f) {
            diff = 360.0f - diff;
        }
        return diff;
    }


    public static float smoothRotate(float current, float target, float speed) {
        float diff = normalizeYaw(target - current);

        if (Math.abs(diff) <= speed) {
            return target;
        }

        if (diff > 0) {
            return current + speed;
        } else {
            return current - speed;
        }
    }


    public static float[] smoothRotate(float currentYaw, float currentPitch,
                                       float targetYaw, float targetPitch, float speed) {
        float newYaw = smoothRotate(currentYaw, targetYaw, speed);
        float newPitch = smoothRotate(currentPitch, targetPitch, speed);
        return new float[]{newYaw, newPitch};
    }


    public static boolean isLookingAt(Vec3d pos, float threshold) {
        if (mc.player == null) return false;

        float[] targetRotation = getRotationsTo(pos);
        float yawDiff = getAngleDifference(mc.player.getYaw(), targetRotation[0]);
        float pitchDiff = getAngleDifference(mc.player.getPitch(), targetRotation[1]);

        return yawDiff <= threshold && pitchDiff <= threshold;
    }


    public static boolean isLookingAtEntity(Entity entity, float threshold) {
        if (mc.player == null) return false;

        float[] targetRotation = getRotationsToEntity(entity);
        float yawDiff = getAngleDifference(mc.player.getYaw(), targetRotation[0]);
        float pitchDiff = getAngleDifference(mc.player.getPitch(), targetRotation[1]);

        return yawDiff <= threshold && pitchDiff <= threshold;
    }


    public static float getRotationDistance(Vec3d pos) {
        if (mc.player == null) return 0.0f;

        float[] targetRotation = getRotationsTo(pos);
        float yawDiff = getAngleDifference(mc.player.getYaw(), targetRotation[0]);
        float pitchDiff = getAngleDifference(mc.player.getPitch(), targetRotation[1]);

        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }
}
