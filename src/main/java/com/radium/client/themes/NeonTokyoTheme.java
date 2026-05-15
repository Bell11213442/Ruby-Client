package com.radium.client.themes;
// radium client

import com.radium.client.modules.client.HUD;

public class NeonTokyoTheme implements Theme {

    // Neon Tokyo theme colors (Bright neon colors, cyberpunk vibe)
    private static final int BACKGROUND = 0xF0100510; // Very dark purple-black
    private static final int SHADOW = 0x50FF00FF; // Magenta glow shadow
    private static final int BORDER = 0x80FF00FF; // Bright magenta border
    private static final int TEXT = 0xFFFF00FF; // Bright magenta text
    private static final int SECONDARY_TEXT = 0xFF00FFFF; // Cyan secondary text
    private static final int HEADER_TEXT = 0xFFFF00FF; // Bright magenta header
    private static final int SEPARATOR_RGB = 0xFF00FF; // Magenta separator
    private static final int HEALTH_BAR_BG = 0xFF1A0A1A; // Dark purple
    private static final int HEALTH_BAR = 0xFFFF00FF; // Bright magenta fill

    // ClickGUI colors
    private static final int GUI_BACKGROUND = 0xFF100510;
    private static final int GUI_CATEGORY_BG = 0xF0150A15;
    private static final int GUI_CATEGORY_HEADER = 0xF01A0F1A;
    private static final int GUI_MODULE_BG = 0xF0120812;
    private static final int GUI_SETTINGS_BG = 0xF0100510;
    private static final int GUI_BORDER = 0x90FF00FF; // Bright magenta border
    private static final int GUI_HOVER = 0xB0FF00FF; // Magenta hover glow
    private static final int GUI_SEPARATOR = 0x60FF00FF; // Magenta separator
    private static final int GUI_TEXT = 0xFFFF00FF; // Bright magenta text
    private static final int GUI_TEXT_ENABLED = 0xFFFF00FF; // Bright magenta enabled
    private static final int GUI_TEXT_DISABLED = 0xFF00FFFF; // Cyan disabled

    @Override
    public int getBackgroundColor(int alphaValue) {
        // Apply transparency to background
        float alpha = Math.max(0.0f, Math.min(1.0f, alphaValue / 255.0f));
        return com.radium.client.gui.RadiumGuiTheme.applyAlpha(GUI_CATEGORY_BG & 0x00FFFFFF, alpha * 0.5f); // 50% of alpha for transparency
    }

    @Override
    public int getShadowColor() {
        return SHADOW;
    }

    @Override
    public int getBorderColor() {
        // Match ClickGUI border color exactly
        return GUI_BORDER;
    }

    @Override
    public int getTextColor(HUD hud) {
        // White text for HUD
        return 0xFFFFFFFF;
    }

    @Override
    public int getSecondaryTextColor(int pixelY, int totalHeight, HUD hud) {
        return 0xFFFFFFFF;
    }

    @Override
    public int getHeaderColor(HUD hud) {
        return 0xFFFFFFFF;
    }

    @Override
    public int getSeparatorColor(int alphaValue) {
        int separatorAlpha = (int) (alphaValue * 0.35f);
        return (separatorAlpha << 24) | SEPARATOR_RGB;
    }

    @Override
    public int getHealthBarBgColor() {
        return HEALTH_BAR_BG;
    }

    @Override
    public int getHealthBarColor(HUD hud) {
        return HEALTH_BAR;
    }

    @Override
    public int getItemPadding() {
        return 4;
    }

    @Override
    public int getBoxPadding() {
        return 8;
    }

    @Override
    public float getTextScale() {
        return 1.14f;
    }

    @Override
    public int getRadius(int cornerRadius) {
        return 6;
    }

    @Override
    public boolean useShadows() {
        return true;
    }

    @Override
    public boolean useBorders() {
        return true;
    }

    @Override
    public boolean useCustomFont() {
        return true;
    }

    // ClickGUI colors
    @Override
    public int getGuiBackground() {
        return GUI_BACKGROUND;
    }

    @Override
    public int getGuiCategoryBackground() {
        return GUI_CATEGORY_BG;
    }

    @Override
    public int getGuiCategoryHeader() {
        return GUI_CATEGORY_HEADER;
    }

    @Override
    public int getGuiModuleBackground() {
        return GUI_MODULE_BG;
    }

    @Override
    public int getGuiSettingsBackground() {
        return GUI_SETTINGS_BG;
    }

    @Override
    public int getGuiHoverColor() {
        return GUI_HOVER;
    }

    @Override
    public int getGuiBorderColor() {
        return GUI_BORDER;
    }

    @Override
    public int getGuiSeparatorColor() {
        return GUI_SEPARATOR;
    }

    @Override
    public int getGuiTextColor() {
        return GUI_TEXT;
    }

    @Override
    public int getGuiEnabledTextColor() {
        return GUI_TEXT_ENABLED;
    }

    @Override
    public int getGuiDisabledTextColor() {
        return GUI_TEXT_DISABLED;
    }

    @Override
    public int getGuiAccentColor() {
        return 0xFFFF00FF; // Bright magenta accent
    }

    @Override
    public boolean isModernStyle() {
        return true;
    }
}

