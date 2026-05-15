package com.radium.client.themes;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.Themes;

public class ThemeManager {

    public static Theme getHudTheme() {
        Themes themesModule = RadiumClient.moduleManager.getModule(Themes.class);
        Themes.HudTheme theme = themesModule != null ? themesModule.hudTheme.getValue() : Themes.HudTheme.MIDNIGHT;

        return switch (theme) {
            case DEFAULT -> new DefaultTheme();
            case MIDNIGHT -> new MidnightTheme();
            case SCIFI -> new SciFiTheme();
            case NEBULA -> new NebulaTheme();
            case MATRIX -> new MatrixTheme();
            case OCEAN -> new OceanTheme();
            case FOREST -> new ForestTheme();
            case NEON_TOKYO -> new NeonTokyoTheme();
        };
    }

    public static Theme getClickGuiTheme() {
        Themes themesModule = RadiumClient.moduleManager.getModule(Themes.class);
        Themes.ClickGuiTheme theme = themesModule != null ? themesModule.clickGuiTheme.getValue() : Themes.ClickGuiTheme.MIDNIGHT;

        return switch (theme) {
            case DEFAULT -> new DefaultTheme();
            case MIDNIGHT -> new MidnightTheme();
            case SCIFI -> new SciFiTheme();
            case NEBULA -> new NebulaTheme();
            case MATRIX -> new MatrixTheme();
            case OCEAN -> new OceanTheme();
            case FOREST -> new ForestTheme();
            case NEON_TOKYO -> new NeonTokyoTheme();
        };
    }
}

