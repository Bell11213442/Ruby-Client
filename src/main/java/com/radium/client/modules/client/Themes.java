package com.radium.client.modules.client;
// radium client

import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.modules.Module;

public class Themes extends Module {
    public final ModeSetting<HudTheme> hudTheme = new ModeSetting<>("HUD Theme", HudTheme.MIDNIGHT, HudTheme.class);
    public final ModeSetting<ClickGuiTheme> clickGuiTheme = new ModeSetting<>("ClickGUI Theme", ClickGuiTheme.MIDNIGHT, ClickGuiTheme.class);

    public Themes() {
        super("Themes", "Customize HUD and GUI themes", Category.CLIENT);
        addSettings(hudTheme, clickGuiTheme);
    }

    public enum HudTheme {
        DEFAULT,
        MIDNIGHT,
        SCIFI,
        NEBULA,
        MATRIX,
        OCEAN,
        FOREST,
        NEON_TOKYO
    }

    public enum ClickGuiTheme {
        DEFAULT,
        MIDNIGHT,
        SCIFI,
        NEBULA,
        MATRIX,
        OCEAN,
        FOREST,
        NEON_TOKYO
    }
}

