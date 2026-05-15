package com.radium.client.gui;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.ClickGUI;
import com.radium.client.themes.Theme;
import com.radium.client.themes.ThemeManager;

public class RadiumGuiTheme {

    public static final long ANIMATION_DURATION = 3000L;
    public static final float HOVER_ALPHA = 0.75f;

    public static float getPanelAlpha() {
        ClickGUI clickGUI = RadiumClient.moduleManager != null ? RadiumClient.moduleManager.getModule(ClickGUI.class) : null;
        if (clickGUI != null && clickGUI.guiOpacity != null) {
            return clickGUI.guiOpacity.getValue().floatValue() / 100.0f;
        }
        return 0.35f;
    }

    private static Theme getCurrentTheme() {
        return ThemeManager.getClickGuiTheme();
    }

    public static int applyAlpha(int color, float alpha) {
        if (alpha > 1.0f) {
            alpha = alpha / 255.0f;
        }
        int a = (int) (alpha * 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int getBackground() {
        return getCurrentTheme().getGuiBackground();
    }

    public static int getCategoryBackground() {
        return getCurrentTheme().getGuiCategoryBackground();
    }

    public static int getCategoryHeader() {
        return getCurrentTheme().getGuiCategoryHeader();
    }

    public static int getModuleBackground() {
        return getCurrentTheme().getGuiModuleBackground();
    }

    public static int getSettingsBackground() {
        return getCurrentTheme().getGuiSettingsBackground();
    }

    public static int getHoverColor() {
        return getCurrentTheme().getGuiHoverColor();
    }

    public static int getBorderColor() {
        return getCurrentTheme().getGuiBorderColor();
    }

    public static int getSeparatorColor() {
        return getCurrentTheme().getGuiSeparatorColor();
    }

    public static int getAccentColor() {
        Theme theme = getCurrentTheme();
        int themeAccent = theme.getGuiAccentColor();
        // For DEFAULT and MIDNIGHT themes, allow ClickGUI color settings to override
        if (themeAccent == 0xFFFF4444) { // Default red accent
            ClickGUI colorsModule = RadiumClient.moduleManager.getModule(ClickGUI.class);
            if (colorsModule != null) {
                return colorsModule.getHudColor(0);
            }
        }
        return themeAccent;
    }

    public static int getAccentColorDark() {
        return blendColors(getAccentColor(), 0xFF000000, 0.2f);
    }

    public static int getAccentColorLight() {
        return blendColors(getAccentColor(), 0xFFFFFFFF, 0.2f);
    }

    public static int getEnabledColor() {
        return getAccentColor();
    }

    public static int getTextColor() {
        return getCurrentTheme().getGuiTextColor();
    }

    public static int getEnabledTextColor() {
        return getCurrentTheme().getGuiEnabledTextColor();
    }

    public static int getDisabledTextColor() {
        return getCurrentTheme().getGuiDisabledTextColor();
    }

    public static int getAccentTextColor() {
        return getAccentColor();
    }

    public static int getHoverAccent() {
        return blendColors(getAccentColor(), getHoverColor(), 0.6f);
    }

    public static int getActiveModuleBackground() {
        return blendColors(getModuleBackground(), getAccentColor(), 0.1f);
    }

    public static int getFocusedBorder() {
        return getAccentColor();
    }

    public static int getTransitionColor(int from, int to, float progress) {
        return blendColors(from, to, progress);
    }

    public static int getDisabledModuleColor() {
        return getModuleBackground();
    }

    public static int getModuleTextColor(boolean enabled) {
        Theme theme = getCurrentTheme();
        return enabled ? theme.getGuiEnabledTextColor() : theme.getGuiDisabledTextColor();
    }

    public static int getSettingsPanelColor() {
        return getCurrentTheme().getGuiCategoryBackground();
    }
}
