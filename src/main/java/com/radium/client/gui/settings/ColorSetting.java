package com.radium.client.gui.settings;
// radium client

import java.awt.*;

public class ColorSetting extends Setting<Color> {
    private Color value;

    public ColorSetting(String name, Color defaultColor) {
        super(name, defaultColor);
        this.value = defaultColor;
    }

    @Override
    public Color getValue() {
        return value;
    }

    @Override
    public void setValue(Color value) {
        this.value = value;
    }
}


