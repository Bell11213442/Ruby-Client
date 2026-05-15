package com.radium.client.utils.Character;
// radium client

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class RotateCharacter {
    private final MinecraftClient mc;
    private final Random random = new Random();
    private boolean isRotating = false;
    private float targetYaw;
    private float targetPitch;
    private Runnable onComplete;

    public RotateCharacter(MinecraftClient mc) {
        this.mc = mc;
    }

    public void rotate(float yaw, float pitch, Runnable onComplete) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.onComplete = onComplete;
        this.isRotating = true;
    }

    public void update(boolean human, boolean fast) {
        if (!isRotating || mc.player == null) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float speed = 1f;
        if (fast) speed = 3.0f;


        if (human) {

            speed += (random.nextFloat() - 0.5f) * 1.5f;

            float distance = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            float easeFactor = Math.min(1.0f, distance / 20.0f);
            speed *= (0.2f + 0.8f * easeFactor);

             if (distance > 5) {
                if (random.nextFloat() < 0.05f) speed *= 0.1f;
            }
        }

        float maxStep = Math.max(0.15f, speed);

        float deltaYaw = MathHelper.clamp(yawDiff, -maxStep, maxStep);
        float deltaPitch = MathHelper.clamp(pitchDiff, -maxStep, maxStep);

        float newYaw = currentYaw + deltaYaw;
        float newPitch = currentPitch + deltaPitch;

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -90, 90));

        if (Math.abs(yawDiff) < 0.5f && Math.abs(pitchDiff) < 0.5f) {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(MathHelper.clamp(targetPitch, -90, 90));
            isRotating = false;
            if (onComplete != null) onComplete.run();
        }
    }


    public boolean isActive() {
        return isRotating;
    }
}
