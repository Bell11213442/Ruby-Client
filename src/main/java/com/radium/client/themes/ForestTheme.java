package com.radium.client.themes;
// radium client

import com.radium.client.modules.client.HUD;

public class ForestTheme implements Theme {

    // Forest theme colors (Greens, browns, earthy tones, natural feel)
    private static final int BACKGROUND = 0xF01A2E1A; // Dark forest green
    private static final int SHADOW = 0x4050C878; // Green glow shadow
    private static final int BORDER = 0x6060C878; // Forest green border
    private static final int TEXT = 0xFF90EE90; // Light green text
    private static final int SECONDARY_TEXT = 0xFF8FBC8F; // Medium green secondary
    private static final int HEADER_TEXT = 0xFF98FB98; // Pale green header
    private static final int SEPARATOR_RGB = 0x8B7355; // Brown separator
    private static final int HEALTH_BAR_BG = 0xFF2A3E2A; // Dark green
    private static final int HEALTH_BAR = 0xFF90EE90; // Light green fill

    // ClickGUI colors
    private static final int GUI_BACKGROUND = 0xFF1A2E1A;
    private static final int GUI_CATEGORY_BG = 0xF0203520;
    private static final int GUI_CATEGORY_HEADER = 0xF0253A25;
    private static final int GUI_MODULE_BG = 0xF01D301D;
    private static final int GUI_SETTINGS_BG = 0xF0182A18;
    private static final int GUI_BORDER = 0x708B7355; // Brown border
    private static final int GUI_HOVER = 0x9050C878; // Green hover glow
    private static final int GUI_SEPARATOR = 0x508B7355; // Brown separator
    private static final int GUI_TEXT = 0xFF90EE90; // Light green text
    private static final int GUI_TEXT_ENABLED = 0xFF98FB98; // Pale green enabled
    private static final int GUI_TEXT_DISABLED = 0xFF8FBC8F; // Medium green disabled

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
        int separatorAlpha = (int) (alphaValue * 0.2f);
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
        return 7;
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
        return 0xFF90EE90; // Light green accent
    }

    @Override
    public boolean isModernStyle() {
        return true;
    }
}

