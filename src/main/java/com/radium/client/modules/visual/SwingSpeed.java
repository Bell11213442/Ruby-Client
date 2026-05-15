package com.radium.client.modules.visual;
// radium client


import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.DoubleSetting;
import com.radium.client.modules.Module;

import static com.radium.client.client.RadiumClient.eventManager;

public final class SwingSpeed extends Module implements TickListener {
    private final DoubleSetting speed = new DoubleSetting(
            "Speed",
            0.25,
            0.1,
            2
    );

    public SwingSpeed() {
        super(
                ("SwingSpeed"),
                ("Control hand swing animation speed"),
                Category.VISUAL
        );
        this.addSettings(this.speed);
        eventManager.add(TickListener.class, this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            resetHandSwingSpeed();
        }
    }

    @Override
    public void onTick2() {
        if (mc.player == null) return;

        float swingSpeed = getCalculatedSwingSpeed();
        applyHandSwingSpeed(swingSpeed);
    }

    private float getCalculatedSwingSpeed() {
        float baseSpeed = speed.getValue().floatValue();

        return baseSpeed;
    }

    private void applyHandSwingSpeed(float speed) {
        if (mc.player.handSwinging) {
            float normalDelta = 1.0f / 6.0f;
            float modifiedDelta = normalDelta * speed;
            modifiedDelta = Math.max(0.001f, Math.min(modifiedDelta, 1.0f));
        }
    }

    private void resetHandSwingSpeed() {
    }

    public float getSwingSpeedMultiplier() {
        if (!this.isEnabled()) return 1.0f;
        return getCalculatedSwingSpeed();
    }
}
