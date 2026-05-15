package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.PlayerEspRenderer;

import java.awt.*;

public class PlayerESP extends Module {


    public final NumberSetting range = new NumberSetting("Range", 100.0, 10.0, 256.0, 1.0);
    public final BooleanSetting fill = new BooleanSetting("Fill", true);
    public final BooleanSetting outline = new BooleanSetting("Outline", true);
    public final BooleanSetting tracers = new BooleanSetting("Tracers", true);


    public final ColorSetting fillColor = new ColorSetting("Fill Color", new Color(255, 50, 50, 50));
    public final ColorSetting outlineColor = new ColorSetting("Outline Color", new Color(255, 50, 50, 255));


    public final BooleanSetting ignoreSelf = new BooleanSetting("Ignore Self", true);

    public PlayerESP() {
        super("PlayerESP", "Highlights other players in the world", Category.VISUAL);
        addSettings(
                range, fill, outline, tracers,
                fillColor, outlineColor,
                ignoreSelf
        );
        PlayerEspRenderer.register();
    }

    public int getFillColor() {
        return fillColor.getValue().getRGB();
    }

    public int getOutlineColor() {
        return outlineColor.getValue().getRGB();
    }
}

