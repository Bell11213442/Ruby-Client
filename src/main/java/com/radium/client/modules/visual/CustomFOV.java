package com.radium.client.modules.visual;

import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;

public final class CustomFOV extends Module {
    private static CustomFOV INSTANCE;
    public final NumberSetting fov = new NumberSetting("FOV", 110, 30, 160, 1);

    public CustomFOV() {
        super("Custom FOV", "Changes your field of view", Category.VISUAL);
        addSettings(fov);
        INSTANCE = this;
    }

    public static CustomFOV get() {
        return INSTANCE;
    }
}
