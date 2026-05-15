package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.CrystalEspRenderer;

import java.awt.*;

public class CrystalESP extends Module {


    public final NumberSetting range = new NumberSetting("Range", 100.0, 10.0, 256.0, 1.0);
    public final BooleanSetting fill = new BooleanSetting("Fill", true);
    public final BooleanSetting outline = new BooleanSetting("Outline", true);
    public final BooleanSetting tracers = new BooleanSetting("Tracers", false);
    public final BooleanSetting showDamage = new BooleanSetting("Show Damage", true);


    public final ColorSetting fillColor = new ColorSetting("Fill Color", new Color(255, 0, 255, 50));
    public final ColorSetting outlineColor = new ColorSetting("Outline Color", new Color(255, 0, 255, 200));

    public CrystalESP() {
        super("CrystalESP", "Highlights end crystals", Category.VISUAL);
        addSettings(
                range, fill, outline, tracers, showDamage,
                fillColor, outlineColor
        );
        CrystalEspRenderer.register();
    }

    public int getFillColor() {
        return fillColor.getValue().getRGB();
    }

    public int getOutlineColor() {
        return outlineColor.getValue().getRGB();
    }
}

