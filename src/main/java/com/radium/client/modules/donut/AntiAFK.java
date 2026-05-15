package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.util.math.MathHelper;

public class AntiAFK extends Module {
    private final NumberSetting interval = new NumberSetting("Interval", 100.0, 20.0, 600.0, 10.0);
    private int tickCounter = 0;

    public AntiAFK() {
        super("AntiAFK", "Prevents you from being kicked for being AFK by rotating your view", Category.DONUT);
        this.addSettings(interval);
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        tickCounter++;

        int intervalTicks = (int) interval.getValue().doubleValue();
        if (tickCounter >= intervalTicks) {
            tickCounter = 0;


            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();


            float yawOffset = (float) (Math.random() * 10.0 - 5.0);
            float pitchOffset = (float) (Math.random() * 10.0 - 5.0);

            float newYaw = currentYaw + yawOffset;
            float newPitch = MathHelper.clamp(currentPitch + pitchOffset, -90.0f, 90.0f);

            mc.player.setYaw(newYaw);
            mc.player.setPitch(newPitch);
        }
    }

    @Override
    public void onDisable() {
        tickCounter = 0;
    }
}

