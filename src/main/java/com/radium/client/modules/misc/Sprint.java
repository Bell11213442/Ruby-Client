package com.radium.client.modules.misc;
// radium client

import com.radium.client.modules.Module;

public class Sprint extends Module {

    public boolean hadSprintToggled;

    public Sprint() {
        super("Sprint", "Automatically sprints to save your pinky finger.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }

        if (mc.options.getSprintToggled().getValue()) {
            mc.options.getSprintToggled().setValue(false);
        }

        mc.options.sprintKey.setPressed(true);
    }

    @Override
    public void onEnable() {
        hadSprintToggled = mc.options.getSprintToggled().getValue();
        mc.options.getSprintToggled().setValue(false);
        super.onEnable();
    }


    @Override
    public void onDisable() {
        mc.options.getSprintToggled().setValue(hadSprintToggled);
        mc.options.sprintKey.setPressed(false);
        super.onDisable();
    }
}
