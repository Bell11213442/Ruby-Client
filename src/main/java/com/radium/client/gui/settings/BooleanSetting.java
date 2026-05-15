package com.radium.client.gui.settings;
// radium client

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, Boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        setValue(!getValue());
    }
}
