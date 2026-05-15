package com.radium.client.themes;
// radium client

import com.radium.client.modules.client.HUD;

public interface Theme {
    // HUD colors
    int getBackgroundColor(int alphaValue);

    int getShadowColor();

    int getBorderColor();

    int getTextColor(HUD hud);

    int getSecondaryTextColor(int pixelY, int totalHeight, HUD hud);

    int getHeaderColor(HUD hud);

    int getSeparatorColor(int alphaValue);

    int getHealthBarBgColor();

    int getHealthBarColor(HUD hud);

    // HUD settings
    int getItemPadding();

    int getBoxPadding();

    float getTextScale();

    int getRadius(int cornerRadius);

    boolean useShadows();

    boolean useBorders();

    boolean useCustomFont();

    // ClickGUI colors
    int getGuiBackground();

    int getGuiCategoryBackground();

    int getGuiCategoryHeader();

    int getGuiModuleBackground();

    int getGuiSettingsBackground();

    int getGuiHoverColor();

    int getGuiBorderColor();

    int getGuiSeparatorColor();

    int getGuiTextColor();

    int getGuiEnabledTextColor();

    int getGuiDisabledTextColor();

    int getGuiAccentColor();

    boolean isModernStyle();
}

