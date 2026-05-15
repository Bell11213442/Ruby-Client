package com.radium.client.themes;
// radium client

import com.radium.client.modules.client.HUD;

public class MidnightTheme implements Theme {

    // Midnight theme colors (SpotiPlay style)
    private static final int BACKGROUND = 0xE0131A2E;
    private static final int SHADOW = 0x35000000;
    private static final int BORDER = 0x20FFFFFF;
    private static final int TEXT = 0xFFF0F0F0;
    private static final int SECONDARY_TEXT = 0xFF9AA4B8;
    private static final int HEADER_TEXT = 0xFFC0C8D8;
    private static final int SEPARATOR_RGB = 0xB0B8C0;
    private static final int HEALTH_BAR_BG = 0xFF2D384A;
    private static final int HEALTH_BAR = 0xFFF5F5F5;

    // ClickGUI colors
    private static final int GUI_BACKGROUND = 0xFF0D1117;
    private static final int GUI_CATEGORY_BG = 0xF0131A2E;
    private static final int GUI_CATEGORY_HEADER = 0xF0181F35;
    private static final int GUI_MODULE_BG = 0xF0151B2F;
    private static final int GUI_SETTINGS_BG = 0xF010161F;
    private static final int GUI_BORDER = 0x30FFFFFF;
    private static final int GUI_HOVER = 0x40FFFFFF;
    private static final int GUI_SEPARATOR = 0x25FFFFFF;
    private static final int GUI_TEXT = 0xFFF0F0F0;
    private static final int GUI_TEXT_ENABLED = 0xFFFFFFFF;
    private static final int GUI_TEXT_DISABLED = 0xFF9AA4B8;

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
        int separatorAlpha = (int) (alphaValue * 0.15f);
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
        return 0xFFFF4444; // Default red accent (can be overridden by ClickGUI settings)
    }

    @Override
    public boolean isModernStyle() {
        return true;
    }
}

