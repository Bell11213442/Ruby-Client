package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;

public class AutoTPA extends Module {
    private final StringSetting playerName = new StringSetting("Player", "DrDonutt");
    private final ModeSetting<Mode> mode = new ModeSetting<>("Mode", Mode.TPAHERE, Mode.class);
    private final NumberSetting minDelay = new NumberSetting("Min Delay", 10.0, 1.0, 100.0, 1.0);
    private final NumberSetting maxDelay = new NumberSetting("Max Delay", 30.0, 1.0, 100.0, 1.0);

    private int delayCounter;

    public AutoTPA() {
        super("AutoTPA", "Automatically sends TPA requests to a player", Category.MISC);
        this.addSettings(this.playerName, this.mode, this.minDelay, this.maxDelay);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.delayCounter = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.delayCounter = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        if (this.delayCounter > 0) {
            --this.delayCounter;
            return;
        }

        String command;
        if (this.mode.isMode(Mode.TPA)) {
            command = "tpa " + this.playerName.getValue();
        } else {
            command = "tpahere " + this.playerName.getValue();
        }

        mc.getNetworkHandler().sendChatCommand(command);

        int minDelayValue = this.minDelay.getValue().intValue();
        int maxDelayValue = this.maxDelay.getValue().intValue();

        if (minDelayValue > maxDelayValue) {
            this.delayCounter = minDelayValue;
        } else {
            this.delayCounter = minDelayValue + (int) (Math.random() * (maxDelayValue - minDelayValue + 1));
        }
    }

    public enum Mode {
        TPA("tpa", 0),
        TPAHERE("tpahere", 1);

        Mode(final String name, final int ordinal) {
        }
    }
}
