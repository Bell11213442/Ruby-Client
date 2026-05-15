package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class Freelook extends Module {
    private final BooleanSetting seeThroughWalls = new BooleanSetting("See Through Walls", false);
    public float yaw;
    public float pitch;
    public float previousYaw;
    public float previousPitch;
    private Perspective previousPerspective;

    public Freelook() {
        super("Freelook", "Look around without changing movement", Category.MISC);
        addSettings(seeThroughWalls);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.options == null) {
            toggle();
            return;
        }
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        previousYaw = yaw;
        previousPitch = pitch;
        previousPerspective = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.options != null) {
            if (previousPerspective != null) mc.options.setPerspective(previousPerspective);
            else mc.options.setPerspective(Perspective.FIRST_PERSON);
        }
        super.onDisable();
    }

    public boolean isActive() {
        return enabled;
    }

    public boolean allowCameraNoClip() {
        return seeThroughWalls.getValue();
    }

    public void updateRotation(double deltaYaw, double deltaPitch) {
        previousYaw = yaw;
        previousPitch = pitch;
        yaw += (float) deltaYaw;
        pitch += (float) deltaPitch;
        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(pitch, -90f, 90f);
    }

    public double getInterpolatedYaw(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousYaw, yaw);
    }

    public double getInterpolatedPitch(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousPitch, pitch);
    }
}

