package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.BaseEspRenderer;

public final class BaseESP extends Module {
    private final NumberSetting alphaValue = new NumberSetting("Alpha", 70, 0, 255, 1);

    public BaseESP() {
        super("BaseESP", "Renders blocks/entities used in farms/bases", Category.VISUAL);
        addSettings(alphaValue);
        BaseEspRenderer.register();
    }

    public NumberSetting getAlphaValue() {
        return alphaValue;
    }
}

