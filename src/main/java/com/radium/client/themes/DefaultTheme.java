package com.radium.client.themes;
// radium client

import com.radium.client.modules.client.HUD;

public class DefaultTheme implements Theme {

    // Default theme colors
    private static final int BACKGROUND = 0xFF0A0A0C;
    private static final int CATEGORY_BG = 0xFF1F1F22;
    private static final int CATEGORY_HEADER = 0xFF28282B;
    private static final int MODULE_BG = 0xFF232326;
    private static final int SETTINGS_BG = 0xFF1A1A1D;
    private static final int BORDER = 0xFF3D3D60;
    private static final int HOVER = 0xFF3A3A3E;
    private static final int SEPARATOR = 0xFF3A3A3D;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_ENABLED = 0xFFFFFFFF;
    private static final int TEXT_DISABLED = 0xFFB0B0B0;

    @Override
    public int getBackgroundColor(int alphaValue) {
        // Apply transparency to background
        float alpha = Math.max(0.0f, Math.min(1.0f, alphaValue / 255.0f));
        return com.radium.client.gui.RadiumGuiTheme.applyAlpha(CATEGORY_BG & 0x00FFFFFF, alpha * 0.5f); // 50% of alpha for transparency
    }

    @Override
    public int getShadowColor() {
        return 0x00000000;
    }

    @Override
    public int getBorderColor() {
        return 0x00000000;
    }

    @Override
    public int getTextColor(HUD hud) {
        return hud.getHudColorInt();
    }

    @Override
    public int getSecondaryTextColor(int pixelY, int totalHeight, HUD hud) {
        return hud.getColorAtHeight(pixelY, totalHeight);
    }

    @Override
    public int getHeaderColor(HUD hud) {
        return hud.getHudColorInt();
    }

    @Override
    public int getSeparatorColor(int alphaValue) {
        int separatorAlpha = (int) (alphaValue * 0.15f);
        return (separatorAlpha << 24) | 0xFFFFFF;
    }

    @Override
    public int getHealthBarBgColor() {
        return 0x40000000;
    }

    @Override
    public int getHealthBarColor(HUD hud) {
        return hud.getHudColorInt();
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
        return 1.235f;
    }

    @Override
    public int getRadius(int cornerRadius) {
        return cornerRadius * 2;
    }

    @Override
    public boolean useShadows() {
        return false;
    }

    @Override
    public boolean useBorders() {
        return false;
    }

    @Override
    public boolean useCustomFont() {
        return false;
    }

    // ClickGUI colors
    @Override
    public int getGuiBackground() {
        return BACKGROUND;
    }

    @Override
    public int getGuiCategoryBackground() {
        return CATEGORY_BG;
    }

    @Override
    public int getGuiCategoryHeader() {
        return CATEGORY_HEADER;
    }

    @Override
    public int getGuiModuleBackground() {
        return MODULE_BG;
    }

    @Override
    public int getGuiSettingsBackground() {
        return SETTINGS_BG;
    }

    @Override
    public int getGuiHoverColor() {
        return HOVER;
    }

    @Override
    public int getGuiBorderColor() {
        return BORDER;
    }

    @Override
    public int getGuiSeparatorColor() {
        return SEPARATOR;
    }

    @Override
    public int getGuiTextColor() {
        return TEXT;
    }

    @Override
    public int getGuiEnabledTextColor() {
        return TEXT_ENABLED;
    }

    @Override
    public int getGuiDisabledTextColor() {
        return TEXT_DISABLED;
    }

    @Override
    public int getGuiAccentColor() {
        return 0xFFFF4444; // Default red accent
    }

    @Override
    public boolean isModernStyle() {
        return false;
    }
}

