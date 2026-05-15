package com.radium.client.themes;
// radium client

import com.radium.client.modules.client.HUD;

public class MatrixTheme implements Theme {

    // Matrix theme colors (Green on black, terminal/code aesthetic)
    private static final int BACKGROUND = 0xF0000000; // Pure black
    private static final int SHADOW = 0x3000FF00; // Green glow shadow
    private static final int BORDER = 0x5000FF00; // Bright green border
    private static final int TEXT = 0xFF00FF00; // Bright green text
    private static final int SECONDARY_TEXT = 0xFF00CC00; // Slightly dimmer green
    private static final int HEADER_TEXT = 0xFF00FF00; // Bright green header
    private static final int SEPARATOR_RGB = 0x00FF00; // Green separator
    private static final int HEALTH_BAR_BG = 0xFF001100; // Dark green
    private static final int HEALTH_BAR = 0xFF00FF00; // Bright green fill

    // ClickGUI colors
    private static final int GUI_BACKGROUND = 0xFF000000;
    private static final int GUI_CATEGORY_BG = 0xF0050505;
    private static final int GUI_CATEGORY_HEADER = 0xF0080808;
    private static final int GUI_MODULE_BG = 0xF0030303;
    private static final int GUI_SETTINGS_BG = 0xF0010101;
    private static final int GUI_BORDER = 0x6000FF00; // Bright green border
    private static final int GUI_HOVER = 0x8000FF00; // Green hover glow
    private static final int GUI_SEPARATOR = 0x4000FF00; // Green separator
    private static final int GUI_TEXT = 0xFF00FF00; // Bright green text
    private static final int GUI_TEXT_ENABLED = 0xFF00FF00; // Bright green enabled
    private static final int GUI_TEXT_DISABLED = 0xFF00AA00; // Dimmer green disabled

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
        int separatorAlpha = (int) (alphaValue * 0.3f);
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
        return 0xFF00FF00; // Bright green accent
    }

    @Override
    public boolean isModernStyle() {
        return true;
    }
}

