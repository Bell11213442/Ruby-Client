package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.BeehiveEspRenderer;

import java.awt.*;

public class BeehiveESP extends Module {

    public final NumberSetting range = new NumberSetting("Range", 100.0, 10.0, 256.0, 1.0);
    public final BooleanSetting fill = new BooleanSetting("Fill", true);
    public final BooleanSetting outline = new BooleanSetting("Outline", true);
    public final BooleanSetting tracers = new BooleanSetting("Tracers", false);

    public final ColorSetting fillColor = new ColorSetting("Fill Color", new Color(255, 200, 0, 50));
    public final ColorSetting outlineColor = new ColorSetting("Outline Color", new Color(255, 200, 0, 200));
    public final ColorSetting tracerColor = new ColorSetting("Tracer Color", new Color(255, 215, 0, 200));

    public final BooleanSetting includeLevel0 = new BooleanSetting("Level 0 (Empty - 0%)", false);
    public final BooleanSetting includeLevel1 = new BooleanSetting("Level 1 (20% Honey)", false);
    public final BooleanSetting includeLevel2 = new BooleanSetting("Level 2 (40% Honey)", false);
    public final BooleanSetting includeLevel3 = new BooleanSetting("Level 3 (60% Honey)", false);
    public final BooleanSetting includeLevel4 = new BooleanSetting("Level 4 (80% Honey)", false);
    public final BooleanSetting includeLevel5 = new BooleanSetting("Level 5 (Full - 100%)", true);

    public final BooleanSetting includeBeehives = new BooleanSetting("Beehives", true);
    public final BooleanSetting includeBeeNests = new BooleanSetting("Bee Nests", true);

    public BeehiveESP() {
        super("BeehiveESP", "Highlights beehives and bee nests", Category.VISUAL);
        addSettings(
                range, fill, outline, tracers,
                fillColor, outlineColor, tracerColor,
                includeLevel0, includeLevel1, includeLevel2, includeLevel3, includeLevel4, includeLevel5,
                includeBeehives, includeBeeNests
        );
        BeehiveEspRenderer.register();
    }

    public int getFillColor() {
        return fillColor.getValue().getRGB();
    }

    public int getOutlineColor() {
        return outlineColor.getValue().getRGB();
    }

    public int getTracerColor() {
        return tracerColor.getValue().getRGB();
    }
}

