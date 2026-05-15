package com.radium.client.modules.visual;

import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import java.awt.Color;

public final class CustomCrosshair extends Module {
    private static CustomCrosshair INSTANCE;
    public final NumberSetting size = new NumberSetting("Size", 5, 1, 20, 1);
    public final NumberSetting gap = new NumberSetting("Gap", 2, 0, 10, 1);
    public final NumberSetting thickness = new NumberSetting("Thickness", 1, 0.5, 5, 0.5);
    public final ColorSetting color = new ColorSetting("Color", Color.WHITE);

    public CustomCrosshair() {
        super("Custom Crosshair", "Renders a custom crosshair", Category.VISUAL);
        addSettings(size, gap, thickness, color);
        INSTANCE = this;
    }

    public static CustomCrosshair get() {
        return INSTANCE;
    }
}
