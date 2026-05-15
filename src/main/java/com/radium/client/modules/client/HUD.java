package com.radium.client.modules.client;

import com.radium.client.client.KeybindManager;
import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.settings.*;
import com.radium.client.modules.Module;
import com.radium.client.themes.Theme;
import com.radium.client.themes.ThemeManager;
import com.radium.client.utils.PingUtils;
import com.radium.client.utils.ScoreboardUtils;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module {
    public final BooleanSetting showWatermark = new BooleanSetting("Show Watermark", true);
    public final ModeSetting watermark = new ModeSetting("Watermark Style", WatermarkStyle.NORMAL,
            WatermarkStyle.class);
    public final BooleanSetting showFPS = new BooleanSetting("Show FPS", true);
    public final StringSetting fpsFormat = new StringSetting("FPS Format", "FPS: {value}");
    public final BooleanSetting showPing = new BooleanSetting("Show Ping", true);
    public final BooleanSetting showServerIP = new BooleanSetting("Show Server IP", true);
    public final BooleanSetting showCoordinates = new BooleanSetting("Show Coordinates", true);
    public final StringSetting coordsFormat = new StringSetting("Coords Format", "X: {x} Y: {y} Z: {z}");
    public final BooleanSetting showKeybinds = new BooleanSetting("Show Keybinds", true);
    public final BooleanSetting lowercaseKeybinds = new BooleanSetting("Lowercase Keybinds", false);
    public final BooleanSetting showTotems = new BooleanSetting("Show Totem Count", true);
    public final StringSetting totemFormat = new StringSetting("Totem Format", "Totems: {value}");
    public final BooleanSetting showModuleList = new BooleanSetting("Show Module List", true);
    public final BooleanSetting showTargetHUD = new BooleanSetting("Show Target HUD", true);
    public final BooleanSetting lowercase = new BooleanSetting("Lowercase Modules", false);
    public final NumberSetting alpha = new NumberSetting("Alpha", 80, 0, 255, 1);
    public final NumberSetting cornerRadius = new NumberSetting("Corner Radius", 3, 0, 32, 1);
    public final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 2.0, 0.05);
    public final BooleanSetting showRegionMap = new BooleanSetting("Show Region Map", false);
    public final NumberSetting regionMapCellSize = new NumberSetting("Region Cell Size", 22, 10, 50, 1);
    public final BooleanSetting regionMapShowCoords = new BooleanSetting("Region Show Coords", true);
    public final BooleanSetting regionMapShowLabels = new BooleanSetting("Region Show Labels", true);
    public final BooleanSetting regionMapShowGrid = new BooleanSetting("Region Show Grid", true);
    public final BooleanSetting regionMapShowPlayer = new BooleanSetting("Region Show Player", true);
    public final ColorSetting regionMapBgColor = new ColorSetting("Region Bg Color", new Color(0, 0, 0));
    public final ColorSetting regionMapGridColor = new ColorSetting("Region Grid Color", new Color(255, 255, 255));
    public final ColorSetting regionMapPlayerColor = new ColorSetting("Region Player Color", new Color(255, 0, 0));
    public final NumberSetting regionMapTransparency = new NumberSetting("Region Transparency", 0.5, 0.0, 1.0, 0.05);
    private final StringSetting pingFormat = new StringSetting("Ping Format", "Ping: {value}ms");
    private final BooleanSetting fakeServer = new BooleanSetting("Fake Server", false);
    private final StringSetting fakeServerIP = new StringSetting("Fake Server IP", "donutsmp.net");
    private final BooleanSetting lowercaseServer = new BooleanSetting("Lowercase Server", false);
    private final ModeSetting moduleListPos = new ModeSetting("Module List Pos", ModulePos.RIGHT,
            ModulePos.class);
    private final ColorSetting hudColor = new ColorSetting("HUD Color", new Color(255, 0, 0));
    private final ColorSetting secondaryColor = new ColorSetting("Secondary Color",
            new Color(255, 175, 0));
    private final BooleanSetting useGradient = new BooleanSetting("Use Gradient", false);
    private final BooleanSetting editHUD = new BooleanSetting("Edit HUD", false);
    private final RegionMapComponent regionMapComponent = new RegionMapComponent();
    private final BooleanSetting watermarkCentered = new BooleanSetting("Watermark Centered", false);
    private final BooleanSetting infoLinesCentered = new BooleanSetting("Info Lines Centered", false);
    private final BooleanSetting keybindsCentered = new BooleanSetting("Keybinds Centered", false);
    private final BooleanSetting coordinatesCentered = new BooleanSetting("Coordinates Centered", false);
    private final BooleanSetting totemsCentered = new BooleanSetting("Totems Centered", false);
    private final BooleanSetting moduleListCentered = new BooleanSetting("Module List Centered", false);
    private final BooleanSetting targetHUDCentered = new BooleanSetting("Target HUD Centered", false);
    private final BooleanSetting regionMapCentered = new BooleanSetting("Region Map Centered", false);
    private final long lastPingTime = 0;
    private final long lastPing = -1;
    private final NumberSetting watermarkX = new NumberSetting("Watermark X", 0, -1000, 10000, 1);
    private final NumberSetting watermarkY = new NumberSetting("Watermark Y", 8, -1000, 10000, 1);
    private final NumberSetting infoLinesX = new NumberSetting("Info Lines X", 0, -1000, 10000, 1);
    private final NumberSetting infoLinesY = new NumberSetting("Info Lines Y", 8, -1000, 10000, 1);
    private final NumberSetting keybindsX = new NumberSetting("Keybinds X", 0, -1000, 10000, 1);
    private final NumberSetting keybindsY = new NumberSetting("Keybinds Y", 8, -1000, 10000, 1);
    private final NumberSetting coordinatesX = new NumberSetting("Coordinates X", 0, -1000, 10000, 1);
    private final NumberSetting coordinatesY = new NumberSetting("Coordinates Y", 8, -1000, 10000, 1);
    private final NumberSetting totemsX = new NumberSetting("Totems X", 0, -1000, 10000, 1);
    private final NumberSetting totemsY = new NumberSetting("Totems Y", 8, -1000, 10000, 1);
    private final NumberSetting moduleListX = new NumberSetting("Module List X", 0, -1000, 10000, 1);
    private final NumberSetting moduleListY = new NumberSetting("Module List Y", 16, -1000, 10000, 1);
    private final NumberSetting targetHUDX = new NumberSetting("Target HUD X", 0, -1000, 10000, 1);
    private final NumberSetting targetHUDY = new NumberSetting("Target HUD Y", 8, -1000, 10000, 1);
    private final NumberSetting regionMapX = new NumberSetting("Region Map X", 100, -1000, 10000, 1);
    private final NumberSetting regionMapY = new NumberSetting("Region Map Y", 100, -1000, 10000, 1);
    public HUD() {
        super("HUD", "Config Your Hud To Your Finger Licking Goodness;)", Category.CLIENT);
        this.enabled = true;
        addSettings(showWatermark, showFPS, showPing, showServerIP, showCoordinates, showModuleList,
                showTotems, showKeybinds, showTargetHUD, fakeServer, fakeServerIP, lowercase,
                lowercaseKeybinds, lowercaseServer, totemFormat, fpsFormat, pingFormat, coordsFormat,
                watermark, hudColor, secondaryColor, useGradient,
                editHUD, hudScale,
                showRegionMap, regionMapCellSize, regionMapShowCoords, regionMapShowLabels,
                regionMapShowPlayer, regionMapBgColor, regionMapGridColor,
                regionMapPlayerColor, regionMapTransparency);
    }

    public RegionMapComponent getRegionMapComponent() {
        return regionMapComponent;
    }

    public StringSetting getFpsFormat() {
        return fpsFormat;
    }

    public StringSetting getCoordsFormat() {
        return coordsFormat;
    }

    public StringSetting getTotemFormat() {
        return totemFormat;
    }

    private boolean useCustomPositions() {
        return watermarkX.getValue() != 0 || watermarkY.getValue() != 8 ||
                infoLinesX.getValue() != 0 || infoLinesY.getValue() != 8 ||
                keybindsX.getValue() != 0 || keybindsY.getValue() != 8 ||
                coordinatesX.getValue() != 0 || coordinatesY.getValue() != 8 ||
                totemsX.getValue() != 0 || totemsY.getValue() != 8 ||
                moduleListX.getValue() != 0 || moduleListY.getValue() != 16 ||
                targetHUDX.getValue() != 0 || targetHUDY.getValue() != 8 ||
                regionMapX.getValue() != 100 || regionMapY.getValue() != 100;
    }

    @Override
    public void onTick() {
        if (editHUD.getValue()) {
            editHUD.setValue(false);
            if (RadiumClient.mc != null) {
                RadiumClient.mc.execute(() -> RadiumClient.mc.setScreen(new com.radium.client.gui.HudEditorScreen()));
            }
        }
    }

    public NumberSetting getWatermarkX() {
        return watermarkX;
    }

    public NumberSetting getWatermarkY() {
        return watermarkY;
    }

    public NumberSetting getInfoLinesX() {
        return infoLinesX;
    }

    public NumberSetting getInfoLinesY() {
        return infoLinesY;
    }

    public NumberSetting getKeybindsX() {
        return keybindsX;
    }

    public NumberSetting getKeybindsY() {
        return keybindsY;
    }

    public NumberSetting getCoordinatesX() {
        return coordinatesX;
    }

    public NumberSetting getCoordinatesY() {
        return coordinatesY;
    }

    public NumberSetting getTotemsX() {
        return totemsX;
    }

    public NumberSetting getTotemsY() {
        return totemsY;
    }

    public NumberSetting getModuleListX() {
        return moduleListX;
    }

    public NumberSetting getModuleListY() {
        return moduleListY;
    }

    public NumberSetting getTargetHUDX() {
        return targetHUDX;
    }

    public NumberSetting getTargetHUDY() {
        return targetHUDY;
    }

    public NumberSetting getRegionMapX() {
        return regionMapX;
    }

    public NumberSetting getRegionMapY() {
        return regionMapY;
    }

    public BooleanSetting getWatermarkCentered() {
        return watermarkCentered;
    }

    public BooleanSetting getInfoLinesCentered() {
        return infoLinesCentered;
    }

    public BooleanSetting getKeybindsCentered() {
        return keybindsCentered;
    }

    public BooleanSetting getCoordinatesCentered() {
        return coordinatesCentered;
    }

    public BooleanSetting getTotemsCentered() {
        return totemsCentered;
    }

    public BooleanSetting getModuleListCentered() {
        return moduleListCentered;
    }

    public BooleanSetting getTargetHUDCentered() {
        return targetHUDCentered;
    }

    public BooleanSetting getRegionMapCentered() {
        return regionMapCentered;
    }

    public String getPing() {
        PingUtils.updatePingAsync();
        String region = "Region";
        if (isOnDonutSMP()) {
            region = "NA West";
            String pingg = ScoreboardUtils.getPing();

            String formattedPing = (pingg == null) ? "N/A" : pingg;
            if (!formattedPing.endsWith("ms") && formattedPing.matches("\\d+")) {
                formattedPing += "ms";
            }
            return region + " " + formattedPing;
        }

        long ping = PingUtils.getCachedPing();
        String pingStr = (ping < 0) ? "N/A" : (ping + "ms");

        return "Ping " + pingStr;
    }

    private boolean isOnDonutSMP() {
        var mc = RadiumClient.mc;
        if (mc.getCurrentServerEntry() != null) {
            String serverAddress = mc.getCurrentServerEntry().address.toLowerCase();
            return serverAddress.contains("donutsmp");
        }
        return false;
    }

    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    private int getTotemCount() {
        int count = 0;
        var mc = RadiumClient.mc;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING)
                count += stack.getCount();
        }
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING)
            count += offhand.getCount();
        return count;
    }

    private int interpolateColor(Color color1, Color color2, float ratio) {
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * ratio);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * ratio);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * ratio);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int getGradientColor(int index, int totalCount) {
        if (!useGradient.getValue() || totalCount <= 1) {
            return getHudColor();
        }
        Color primary = hudColor.getValue();
        Color secondary = secondaryColor.getValue();
        float ratio = (float) index / (totalCount - 1);
        return interpolateColor(primary, secondary, ratio);
    }

    private BarPosition getBarPosition(int xPos, int screenWidth) {
        int threshold = 4;
        if (xPos <= threshold) {
            return BarPosition.LEFT;
        } else if (xPos >= screenWidth - threshold - 100) { // Generic check, will be refined in render
            var mc = RadiumClient.mc;
            if (xPos > screenWidth - 200) return BarPosition.RIGHT;
            return BarPosition.MIDDLE;
        } else {
            return BarPosition.MIDDLE;
        }
    }

    private int getTextX(BarPosition barPos, int boxX, int boxWidth, int textWidth, int padding, int fixedBarX) {
        switch (barPos) {
            case LEFT:
                return boxX + padding;
            case RIGHT:

                return boxX + boxWidth - textWidth - padding;
            case MIDDLE:
            default:

                return boxX + (boxWidth - textWidth) / 2 - 2;
        }
    }

    public int getColorAtHeight(int pixelY, int totalHeight) {
        if (!useGradient.getValue() || totalHeight <= 1) {
            return getHudColor();
        }
        Color primary = hudColor.getValue();
        Color secondary = secondaryColor.getValue();
        float ratio = Math.max(0, Math.min(1, (float) pixelY / (totalHeight - 1)));
        return interpolateColor(primary, secondary, ratio);
    }

    private int recalculateCenteredX(int elementWidth, int screenWidth) {
        return (screenWidth / 2) - (elementWidth / 2);
    }

    private int applyCenterSnap(int xPos, int elementWidth, int screenWidth, NumberSetting xSetting) {
        int screenCenter = screenWidth / 2;
        int elementCenter = xPos + elementWidth / 2;
        int CENTER_SNAP_THRESHOLD = 50;
        if (Math.abs(elementCenter - screenCenter) <= CENTER_SNAP_THRESHOLD) {
            int centeredX = screenCenter - elementWidth / 2;

            if (xSetting != null && useCustomPositions() && xSetting.getValue().intValue() != centeredX) {
                xSetting.setValue((double) centeredX);
            }
            return centeredX;
        }
        return xPos;
    }

    public String getElementAtPosition(int mouseX, int mouseY,
                                       int tempWX, int tempWY, int tempIX, int tempIY, int tempKX, int tempKY,
                                       int tempCX, int tempCY, int tempTX, int tempTY, int tempMX, int tempMY, int tempTHX, int tempTHY,
                                       int tempRMX, int tempRMY) {
        var mc = RadiumClient.mc;
        if (mc.getWindow() == null)
            return null;

        int screenWidth = mc.getWindow().getWidth();

        float scale = hudScale.getValue().floatValue();
        float textScale = 1.5f * scale;
        int textHeight = (int) (mc.textRenderer.fontHeight * textScale);
        int boxPadding = (int) (8 * scale);
        int barWidth = (int) (4 * scale);

        if (showWatermark.getValue()) {
            int x = tempWX;
            int y = tempWY;
            int boxHeight = 40;
            int boxWidth = 200;
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return "watermark";
            }
        }

        if (showFPS.getValue() || showPing.getValue() || showServerIP.getValue()) {
            int x = tempIX;
            int y = tempIY;
            int boxHeight = 100;
            int boxWidth = 200;
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return "infoLines";
            }
        }

        if (showKeybinds.getValue()) {
            int x = tempKX;
            int y = tempKY;
            int boxHeight = 150;
            int boxWidth = 200;
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return "keybinds";
            }
        }

        if (showCoordinates.getValue()) {
            int x = tempCX;
            int y = tempCY;
            int boxHeight = 30;
            int boxWidth = 200;
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return "coordinates";
            }
        }

        if (showTotems.getValue()) {
            int x = tempTX;
            int y = tempTY;
            int boxHeight = 30;
            int boxWidth = 200;
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return "totems";
            }
        }

        if (showModuleList.getValue()) {
            var enabledModules = RadiumClient.moduleManager.getModules().stream()
                    .filter(Module::isEnabled)
                    .filter(m -> !(m instanceof ClickGUI || m instanceof HUD))
                    .collect(Collectors.toList());

            if (!enabledModules.isEmpty()) {
                int moduleItemPadding = (int) (4 * scale);
                int itemHeight = textHeight + (moduleItemPadding * 2);

                BarPosition barPos = getBarPosition(tempMX, screenWidth);
                boolean isRight = barPos == BarPosition.RIGHT;
                boolean isMiddle = barPos == BarPosition.MIDDLE;

                int maxWidth = 0;
                for (var module : enabledModules) {
                    String moduleName = module.getName();
                    if (lowercase.getValue()) {
                        moduleName = moduleName.toLowerCase();
                    } else {
                        moduleName = toTitleCase(moduleName);
                    }
                    int nameWidth = (int) (mc.textRenderer.getWidth(moduleName) * textScale);
                    maxWidth = Math.max(maxWidth, nameWidth);
                }

                int moduleListMinX = Integer.MAX_VALUE;
                int moduleListMaxX = 0;
                int moduleListMinY = tempMY;
                int moduleListMaxY = tempMY;

                int yOffset = tempMY;
                for (var module : enabledModules) {
                    String moduleName = module.getName();
                    if (lowercase.getValue()) {
                        moduleName = moduleName.toLowerCase();
                    } else {
                        moduleName = toTitleCase(moduleName);
                    }
                    int nameWidth = (int) (mc.textRenderer.getWidth(moduleName) * textScale);

                    int backgroundY2 = yOffset + textHeight + moduleItemPadding;

                    if (isRight) {
                        int boxWidth = nameWidth + boxPadding + 4 + barWidth;
                        int fixedBarX = tempMX + (boxWidth - barWidth);
                        int boxX = tempMX;
                        moduleListMinX = Math.min(moduleListMinX, boxX);
                        moduleListMaxX = Math.max(moduleListMaxX, fixedBarX + barWidth);
                    } else if (isMiddle) {
                        int boxWidth = nameWidth + boxPadding * 2;
                        int baseX = tempMX - (boxWidth / 2);
                        moduleListMinX = Math.min(moduleListMinX, baseX);
                        moduleListMaxX = Math.max(moduleListMaxX, baseX + boxWidth);
                    } else {
                        int boxWidth = nameWidth + boxPadding + 4;
                        int boxX = tempMX;
                        int fixedBarX = boxX - barWidth;
                        moduleListMinX = Math.min(moduleListMinX, fixedBarX);
                        moduleListMaxX = Math.max(moduleListMaxX, boxX + boxWidth);
                    }
                    moduleListMaxY = Math.max(moduleListMaxY, backgroundY2);
                    yOffset += itemHeight + 4;
                }

                if (mouseX >= moduleListMinX && mouseX <= moduleListMaxX &&
                        mouseY >= moduleListMinY && mouseY <= moduleListMaxY) {
                    return "moduleList";
                }
            }
        }

        if (showTargetHUD.getValue()) {
            int x = tempTHX;
            int y = tempTHY;
            int boxHeight = 80;
            int boxWidth = 250;
            if (mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight) {
                return "targetHUD";
            }
        }

        if (showRegionMap.getValue()) {
            int x = tempRMX;
            int y = tempRMY;
            int cellSize = regionMapCellSize.getValue().intValue();
            int width = regionMapComponent.getWidth(cellSize);
            int height = regionMapComponent.getHeight(cellSize, regionMapShowCoords.getValue(),
                    regionMapShowLabels.getValue());
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                return "regionMap";
            }
        }

        return null;
    }

    public void drawEditOutline(DrawContext context, int x, int y, int width, int height, boolean isDragging) {
        // Removed as requested
    }

    public void render(DrawContext context, float tickDelta) {

        var mc = RadiumClient.mc;
        if (!isEnabled() || mc.player == null || mc.getDebugHud().shouldShowDebugHud())
            return;

        RenderUtils.unscaledProjection();
        var clickGUI = RadiumClient.moduleManager.getModule(ClickGUI.class);

        // Get theme from ThemeManager
        Theme theme = ThemeManager.getHudTheme();

        int currentY = 8;
        int leftMargin = 0;

        // Apply theme-specific settings
        int alphaValue = 80;
        float scale = hudScale.getValue().floatValue();
        int itemPadding = (int) (theme.getItemPadding() * scale);
        int boxPadding = (int) (theme.getBoxPadding() * scale);
        float textScale = theme.getTextScale() * scale;
        int textHeight = (int) (mc.textRenderer.fontHeight * textScale);
        int backgroundColor = theme.getBackgroundColor(alphaValue);
        int shadowColor = theme.getShadowColor();
        int borderColor = theme.getBorderColor();
        int textColor = theme.getTextColor(this);
        int radius = theme.getRadius(3);
        boolean useShadows = theme.useShadows();
        boolean useBorders = theme.useBorders();

        int barWidth = (int) (4 * scale);

        // Keep currentTheme enum for compatibility with existing code
        Themes themesModule = RadiumClient.moduleManager.getModule(Themes.class);
        Themes.HudTheme currentTheme = themesModule != null ? themesModule.hudTheme.getValue() : Themes.HudTheme.MIDNIGHT;

        boolean isModuleListOnLeft = false;

        int totalLeftHeight = 0;
        int startY = currentY;

        int watermarkHeight = 0;
        if (showWatermark.getValue()) {
            // Add extra padding to prevent text cutoff
            watermarkHeight = (textHeight + (boxPadding * 2) + 2) + 64;
            totalLeftHeight += watermarkHeight + 8;
        }

        List<String> infoLines = new ArrayList<>();
        if (showFPS.getValue())
            infoLines.add(fpsFormat.getValue().replace("{value}", String.valueOf(mc.getCurrentFps())));
        if (showPing.getValue())
            infoLines.add(getPing());
        if (showServerIP.getValue()) {
            String serverDisplay;
            if (fakeServer.getValue())
                serverDisplay = fakeServerIP.getValue();
            else {
                String spText = lowercaseServer.getValue() ? "singleplayer" : "Singleplayer";
                serverDisplay = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : (spText);
            }
            infoLines.add(serverDisplay);
        }
        int infoHeight = 0;
        if (!infoLines.isEmpty()) {
            // Add extra padding to prevent text cutoff
            infoHeight = (textHeight * infoLines.size()) + (itemPadding * (infoLines.size() - 1)) + (boxPadding * 2) + 2;
            totalLeftHeight += infoHeight + 8;
        }

        List<Module> modulesWithKeybinds = showKeybinds.getValue() ? RadiumClient.moduleManager.getModules().stream()
                .filter(m -> m.getKeyBind() != -1 && !m.getName().equals("RubyClient"))
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList()) : new ArrayList<>();
        int keybindsHeight = 0;
        if (!modulesWithKeybinds.isEmpty()) {
            keybindsHeight = (textHeight * (modulesWithKeybinds.size() + 1))
                    + (itemPadding * modulesWithKeybinds.size()) + (boxPadding * 2) + 4;
            totalLeftHeight += keybindsHeight + 8;
        }

        List<Module> enabledModules = new ArrayList<>();
        int moduleListHeight = 0;
        if (showModuleList.getValue()) {
            enabledModules = RadiumClient.moduleManager.getModules().stream()
                    .filter(Module::isEnabled)
                    .sorted(Comparator.comparingInt(m -> {
                        String name = m.getName();
                        if (lowercase.getValue()) {
                            name = name.toLowerCase();
                        } else {
                            name = toTitleCase(name);
                        }
                        return (int) (-mc.textRenderer.getWidth(name) * textScale);
                    }))
                    .collect(Collectors.toList());

            if (isModuleListOnLeft) {
                int moduleItemPadding = (int) (4 * scale);
                int itemHeight = textHeight + (moduleItemPadding * 2);
                int moduleCount = (int) enabledModules.stream()
                        .filter(m -> !(m instanceof ClickGUI
                                || m instanceof HUD))
                        .count();

                moduleListHeight = moduleCount * (itemHeight + 4);
                totalLeftHeight += moduleListHeight;
            }
        }

        if (totalLeftHeight > 0)
            totalLeftHeight -= 8;

        int currentPixelY = 0;

        String WatermarkText = "";
        switch (watermark.getValue()) {
            case WatermarkStyle.LOWERCASE:
                WatermarkText = "rubyclient";
                break;
            case WatermarkStyle.UPPERCASE:
                WatermarkText = "RUBYCLIENT";
                break;
            default:
                WatermarkText = "RubyClient";
                break;
        }

        if (showWatermark.getValue()) {
            int watermarkWidth = (int) (mc.textRenderer.getWidth(WatermarkText) * textScale);
            int boxHeight = textHeight + (boxPadding * 2);

            int watermarkXPos = useCustomPositions() ? watermarkX.getValue().intValue() : leftMargin;
            int watermarkYPos = useCustomPositions() ? watermarkY.getValue().intValue() : currentY;

            // SpotiPlay style: no side bars, just rounded rectangles
            int boxWidth = watermarkWidth + (boxPadding * 2);
            int boxX = watermarkXPos;

            // Use theme text color
            int textColorAtCenter = textColor;

            // SpotiPlay style for all themes: clean rounded rectangles
            if (useShadows) {
                RenderUtils.fillRoundRect(context, boxX + 1, watermarkYPos + 1, boxWidth, boxHeight, radius, radius, radius,
                        radius, shadowColor);
            }
            RenderUtils.fillRoundRect(context, boxX, watermarkYPos, boxWidth, boxHeight, radius, radius, radius,
                    radius, backgroundColor);
            if (useBorders) {
                RenderUtils.drawRoundRect(context, boxX, watermarkYPos, boxWidth, boxHeight, radius, borderColor);
            }

            int textX = boxX + boxPadding;
            int textY = watermarkYPos + boxPadding;

            context.getMatrices().push();
            context.getMatrices().scale(textScale, textScale, 1.0f);
            // Clean text rendering - ensure pixel-perfect alignment
            float textXPos = Math.round(textX / textScale);
            float textYPos = Math.round(textY / textScale) + 1.0f;
            context.drawText(mc.textRenderer, WatermarkText, (int)textXPos, (int)textYPos, textColorAtCenter, false);
            context.getMatrices().pop();

            if (!useCustomPositions()) {
                currentY += boxHeight + 8;
                currentPixelY += boxHeight + 8;
            }
        }

        if (!infoLines.isEmpty()) {
            int maxWidth = 0;
            for (String line : infoLines) {
                int lineWidth = (int) (mc.textRenderer.getWidth(line) * textScale);
                maxWidth = Math.max(maxWidth, lineWidth);
            }

            // Add extra padding to prevent text cutoff
            int boxHeight = (textHeight * infoLines.size()) + (itemPadding * (infoLines.size() - 1)) + (boxPadding * 2) + 2;

            int infoXPos = useCustomPositions() ? infoLinesX.getValue().intValue() : leftMargin;
            int infoYPos = useCustomPositions() ? infoLinesY.getValue().intValue() : currentY;

            int boxWidth = maxWidth + (boxPadding * 2);
            int boxX = infoXPos;

            // Enhanced shadows for depth
            if (useShadows) {
                // Multi-layer shadow for better depth
                int shadowAlpha1 = (shadowColor >> 24) & 0xFF;
                int shadowAlpha2 = Math.max(0, shadowAlpha1 - 40);
                int shadowAlpha3 = Math.max(0, shadowAlpha2 - 30);
                int shadowColor1 = (shadowAlpha1 << 24) | (shadowColor & 0x00FFFFFF);
                int shadowColor2 = (shadowAlpha2 << 24) | (shadowColor & 0x00FFFFFF);
                int shadowColor3 = (shadowAlpha3 << 24) | (shadowColor & 0x00FFFFFF);

                RenderUtils.fillRoundRect(context, boxX + 3, infoYPos + 3, boxWidth, boxHeight, radius, radius, radius, radius, shadowColor3);
                RenderUtils.fillRoundRect(context, boxX + 2, infoYPos + 2, boxWidth, boxHeight, radius, radius, radius, radius, shadowColor2);
                RenderUtils.fillRoundRect(context, boxX + 1, infoYPos + 1, boxWidth, boxHeight, radius, radius, radius, radius, shadowColor1);
            }
            RenderUtils.fillRoundRect(context, boxX, infoYPos, boxWidth, boxHeight, radius, radius, radius, radius,
                    backgroundColor);
            if (useBorders) {
                // Enhanced border with gradient effect
                RenderUtils.drawRoundRect(context, boxX, infoYPos, boxWidth, boxHeight, radius, borderColor);
                // Inner border highlight
                int highlightBorder = RadiumGuiTheme.applyAlpha(borderColor, 0.3f);
                RenderUtils.drawRoundRect(context, boxX + 1, infoYPos + 1, boxWidth - 2, boxHeight - 2, Math.max(0, radius - 1), highlightBorder);
            }

            int lineY = infoYPos + boxPadding;
            for (String line : infoLines) {
                int textX = boxX + boxPadding;

                context.getMatrices().push();
                context.getMatrices().scale(textScale, textScale, 1.0f);
                // Clean text rendering - ensure pixel-perfect alignment
                float textXPos = Math.round(textX / textScale);
                float textYPos = Math.round(lineY / textScale);
                context.drawText(mc.textRenderer, line, (int)textXPos, (int)textYPos, textColor, false);
                context.getMatrices().pop();

                lineY += textHeight + itemPadding;
            }

            if (!useCustomPositions()) {
                currentY += boxHeight + 10;
                currentPixelY += boxHeight + 10;
            }
        }

        if (showKeybinds.getValue() && !modulesWithKeybinds.isEmpty()) {
            String labelText = lowercaseKeybinds.getValue() ? "keybinds" : toTitleCase("keybinds");
            int labelWidth = (int) (mc.textRenderer.getWidth(labelText) * textScale);

            int maxWidth = labelWidth;
            for (Module module : modulesWithKeybinds) {
                String moduleName = module.getName();
                if (lowercase.getValue()) {
                    moduleName = moduleName.toLowerCase();
                } else if (!lowercaseKeybinds.getValue()) {
                    moduleName = toTitleCase(moduleName);
                }
                String keybindName = KeybindManager.getKeyName(module.getKeyBind());
                if (lowercaseKeybinds.getValue()) {
                    keybindName = keybindName.toLowerCase();
                } else {
                    keybindName = toTitleCase(keybindName);
                }
                int lineWidth = (int) (mc.textRenderer.getWidth(moduleName + " - " + keybindName) * textScale);
                maxWidth = Math.max(maxWidth, lineWidth);
            }

            int boxHeight = (textHeight * (modulesWithKeybinds.size() + 1)) + (itemPadding * modulesWithKeybinds.size())
                    + (boxPadding * 2) + 4;

            int keybindsXPos = useCustomPositions() ? keybindsX.getValue().intValue() : leftMargin;
            int keybindsYPos = useCustomPositions() ? keybindsY.getValue().intValue() : currentY;
            int keybindsY = keybindsYPos;

            int boxWidth;
            int boxX;

            if (theme.isModernStyle()) {
                // Midnight/Sci-Fi theme: no side bars
                boxWidth = maxWidth + (boxPadding * 2);
                boxX = keybindsXPos;

                if (useShadows) {
                    // Multi-layer shadow for better depth
                    int shadowAlpha1 = (shadowColor >> 24) & 0xFF;
                    int shadowAlpha2 = Math.max(0, shadowAlpha1 - 40);
                    int shadowAlpha3 = Math.max(0, shadowAlpha2 - 30);
                    int shadowColor1 = (shadowAlpha1 << 24) | (shadowColor & 0x00FFFFFF);
                    int shadowColor2 = (shadowAlpha2 << 24) | (shadowColor & 0x00FFFFFF);
                    int shadowColor3 = (shadowAlpha3 << 24) | (shadowColor & 0x00FFFFFF);

                    RenderUtils.fillRoundRect(context, boxX + 3, keybindsYPos + 3, boxWidth, boxHeight, radius, radius, radius, radius, shadowColor3);
                    RenderUtils.fillRoundRect(context, boxX + 2, keybindsYPos + 2, boxWidth, boxHeight, radius, radius, radius, radius, shadowColor2);
                    RenderUtils.fillRoundRect(context, boxX + 1, keybindsYPos + 1, boxWidth, boxHeight, radius, radius, radius, radius, shadowColor1);
                }
                RenderUtils.fillRoundRect(context, boxX, keybindsYPos, boxWidth, boxHeight, radius, radius, radius, radius,
                        backgroundColor);
                if (useBorders) {
                    // Enhanced border with gradient effect
                    RenderUtils.drawRoundRect(context, boxX, keybindsYPos, boxWidth, boxHeight, radius, borderColor);
                    // Inner border highlight
                    int highlightBorder = RadiumGuiTheme.applyAlpha(borderColor, 0.3f);
                    RenderUtils.drawRoundRect(context, boxX + 1, keybindsYPos + 1, boxWidth - 2, boxHeight - 2, Math.max(0, radius - 1), highlightBorder);
                }
            } else {
                // Default theme: original style with side bars
                BarPosition barPos = useCustomPositions() ? getBarPosition(keybindsXPos, mc.getWindow().getWidth())
                        : BarPosition.LEFT;
                boolean hasBar = barPos != BarPosition.MIDDLE;
                boxWidth = maxWidth + (boxPadding * 2) + (hasBar ? barWidth : 0);
                int accentColor = getColorAtHeight(currentPixelY + boxHeight / 2, totalLeftHeight);

                int fixedBarX;
                int originalBoxX = keybindsXPos;
                boxX = keybindsXPos;
                if (barPos == BarPosition.LEFT) {
                    fixedBarX = originalBoxX - barWidth;
                } else if (barPos == BarPosition.RIGHT) {
                    fixedBarX = originalBoxX + (boxWidth - barWidth);
                } else {
                    fixedBarX = 0;
                    boxX = keybindsXPos;
                    originalBoxX = boxX;
                }

                if (barPos == BarPosition.LEFT) {
                    RenderUtils.fillRoundRect(context, boxX, keybindsYPos, boxWidth - barWidth, boxHeight, 0, radius,
                            radius, 0, backgroundColor);
                    if (useGradient.getValue() && totalLeftHeight > 1) {
                        drawVerticalGradientBar(context, fixedBarX, keybindsYPos, barWidth, boxHeight, currentPixelY,
                                totalLeftHeight);
                    } else {
                        context.fill(fixedBarX, keybindsYPos, fixedBarX + barWidth, keybindsYPos + boxHeight, accentColor);
                    }
                } else if (barPos == BarPosition.RIGHT) {
                    RenderUtils.fillRoundRect(context, boxX, keybindsYPos, boxWidth - barWidth, boxHeight, radius, 0, 0,
                            radius, backgroundColor);
                    context.fill(fixedBarX, keybindsYPos, fixedBarX + barWidth, keybindsYPos + boxHeight, accentColor);
                } else {
                    RenderUtils.fillRoundRect(context, keybindsXPos, keybindsYPos, boxWidth, boxHeight, radius, radius,
                            radius, radius, backgroundColor);
                }
            }

            int textX;
            if (theme.isModernStyle()) {
                textX = boxX + boxPadding;
            } else {
                BarPosition barPos = useCustomPositions() ? getBarPosition(keybindsXPos, mc.getWindow().getWidth())
                        : BarPosition.LEFT;
                int keybindsBoxWidth = maxWidth + (boxPadding * 2) + (barPos != BarPosition.MIDDLE ? barWidth : 0);
                int fixedBarX = barPos == BarPosition.LEFT ? 0 : (barPos == BarPosition.RIGHT ? mc.getWindow().getWidth() - barWidth : 0);
                textX = getTextX(barPos, boxX, keybindsBoxWidth - (barPos != BarPosition.MIDDLE ? barWidth : 0), labelWidth,
                        boxPadding, fixedBarX);
            }
            int lineY = keybindsY + boxPadding;

            // Use theme header color
            int labelColor = theme.getHeaderColor(this);

            context.getMatrices().push();
            context.getMatrices().scale(textScale, textScale, 1.0f);
            // Clean text rendering - ensure pixel-perfect alignment
            float textXPos = Math.round(textX / textScale);
            float textYPos = Math.round(lineY / textScale);
            context.drawText(mc.textRenderer, labelText, (int)textXPos, (int)textYPos, labelColor, false);
            context.getMatrices().pop();

            lineY += textHeight + itemPadding;

            int separatorColor = theme.getSeparatorColor(alphaValue);
            int separatorX1 = boxX + (currentTheme != Themes.HudTheme.DEFAULT ? boxPadding + 1 : 4);
            int separatorX2;
            if (theme.isModernStyle()) {
                separatorX2 = boxX + boxWidth - boxPadding - 1;
                // Slightly thicker separator for more natural feel / sci-fi glow
                RenderUtils.fillRoundRect(context, separatorX1, lineY + 1, separatorX2 - separatorX1, 2, 1, 1, 1, 1, separatorColor);
            } else {
                BarPosition barPos = useCustomPositions() ? getBarPosition(keybindsXPos, mc.getWindow().getWidth())
                        : BarPosition.LEFT;
                separatorX2 = boxX + (boxWidth - (barPos != BarPosition.MIDDLE ? barWidth : 0)) - 4;
                context.fill(separatorX1, lineY, separatorX2, lineY + 2, separatorColor);
            }
            lineY += 4;

            for (Module module : modulesWithKeybinds) {
                String moduleName = module.getName();
                if (lowercase.getValue()) {
                    moduleName = moduleName.toLowerCase();
                } else if (!lowercaseKeybinds.getValue()) {
                    moduleName = toTitleCase(moduleName);
                }
                String keybindName = KeybindManager.getKeyName(module.getKeyBind());
                if (lowercaseKeybinds.getValue()) {
                    keybindName = keybindName.toLowerCase();
                } else {
                    keybindName = toTitleCase(keybindName);
                }
                String line = moduleName + " - " + keybindName;

                // Use theme secondary text color
                int lineColor = theme.getSecondaryTextColor(currentPixelY + (lineY - keybindsY) + textHeight / 2, totalLeftHeight, this);

                int lineWidth = (int) (mc.textRenderer.getWidth(line) * textScale);

                int lineTextX;
                if (theme.isModernStyle()) {
                    lineTextX = boxX + boxPadding;
                } else {
                    BarPosition barPos = useCustomPositions() ? getBarPosition(keybindsXPos, mc.getWindow().getWidth())
                            : BarPosition.LEFT;
                    int keybindsBoxWidth = maxWidth + (boxPadding * 2) + (barPos != BarPosition.MIDDLE ? barWidth : 0);
                    int fixedBarX = barPos == BarPosition.LEFT ? 0 : (barPos == BarPosition.RIGHT ? mc.getWindow().getWidth() - barWidth : 0);
                    lineTextX = getTextX(barPos, boxX, keybindsBoxWidth - (barPos != BarPosition.MIDDLE ? barWidth : 0),
                            lineWidth, boxPadding, fixedBarX);
                }

                context.getMatrices().push();
                context.getMatrices().scale(textScale, textScale, 1.0f);
                context.drawText(mc.textRenderer, line, (int)(lineTextX / textScale), (int)(lineY / textScale), lineColor, false); // No drop shadow for less bold look
                context.getMatrices().pop();

                lineY += textHeight + itemPadding;
            }

            if (!useCustomPositions()) {
                currentY += boxHeight + 10; // Better spacing between elements
                currentPixelY += boxHeight + 10;
            }
        }

        int centerY = 8;
        int centerSpacing = 8;

        if (showCoordinates.getValue()) {
            int x = mc.player.getBlockPos().getX();
            int y = mc.player.getBlockPos().getY();
            int z = mc.player.getBlockPos().getZ();
            String coordText = coordsFormat.getValue().replace("{x}", String.valueOf(x))
                    .replace("{y}", String.valueOf(y))
                    .replace("{z}", String.valueOf(z));

            int coordWidth = (int) (mc.textRenderer.getWidth(coordText) * textScale);
            int coordBoxHeight = textHeight + (boxPadding * 2);
            int coordXPos = useCustomPositions() ? coordinatesX.getValue().intValue()
                    : (mc.getWindow().getWidth() - coordWidth - boxPadding * 2 - barWidth) / 2;
            int coordYPos = useCustomPositions() ? coordinatesY.getValue().intValue() : centerY;

            int coordBoxWidth;
            int boxX;
            int coordColor;

            if (theme.isModernStyle()) {
                // Midnight/Sci-Fi theme: no side bars
                coordBoxWidth = coordWidth + (boxPadding * 2);
                boxX = coordXPos;
                coordColor = textColor;

                if (useShadows) {
                    // Shadow offset for depth
                    RenderUtils.fillRoundRect(context, boxX + 1, coordYPos + 1, coordBoxWidth, coordBoxHeight, radius, radius,
                            radius, radius, shadowColor);
                }
                RenderUtils.fillRoundRect(context, boxX, coordYPos, coordBoxWidth, coordBoxHeight, radius, radius,
                        radius, radius, backgroundColor);
                if (useBorders) {
                    RenderUtils.drawRoundRect(context, boxX, coordYPos, coordBoxWidth, coordBoxHeight, radius, borderColor);
                }
            } else {
                // Default theme: original style with side bars
                BarPosition barPos = useCustomPositions() ? getBarPosition(coordXPos, mc.getWindow().getWidth())
                        : BarPosition.MIDDLE;
                boolean hasBar = barPos != BarPosition.MIDDLE;
                coordBoxWidth = coordWidth + (boxPadding * 2) + (hasBar ? barWidth : 0);
                coordColor = getColorAtHeight(coordYPos + boxPadding, totalLeftHeight);

                int fixedBarX;
                int originalBoxX = coordXPos;
                boxX = coordXPos;
                if (barPos == BarPosition.LEFT) {
                    fixedBarX = originalBoxX - barWidth;
                } else if (barPos == BarPosition.RIGHT) {
                    fixedBarX = originalBoxX + (coordBoxWidth - barWidth);
                } else {
                    fixedBarX = 0;
                    boxX = coordXPos;
                    originalBoxX = boxX;
                }

                if (barPos == BarPosition.LEFT) {
                    RenderUtils.fillRoundRect(context, boxX, coordYPos, coordBoxWidth - barWidth, coordBoxHeight, 0, radius,
                            radius, 0, backgroundColor);
                    context.fill(fixedBarX, coordYPos, fixedBarX + barWidth, coordYPos + coordBoxHeight, coordColor);
                } else if (barPos == BarPosition.RIGHT) {
                    RenderUtils.fillRoundRect(context, boxX, coordYPos, coordBoxWidth - barWidth, coordBoxHeight, radius, 0,
                            0, radius, backgroundColor);
                    context.fill(fixedBarX, coordYPos, fixedBarX + barWidth, coordYPos + coordBoxHeight, coordColor);
                } else {
                    RenderUtils.fillRoundRect(context, boxX, coordYPos, coordBoxWidth, coordBoxHeight, radius, radius,
                            radius, radius, backgroundColor);
                }
            }

            int textX;
            if (theme.isModernStyle()) {
                textX = boxX + boxPadding;
            } else {
                BarPosition barPos = useCustomPositions() ? getBarPosition(coordXPos, mc.getWindow().getWidth())
                        : BarPosition.MIDDLE;
                int coordBoxWidthCalc = coordWidth + (boxPadding * 2) + (barPos != BarPosition.MIDDLE ? barWidth : 0);
                int fixedBarX = barPos == BarPosition.LEFT ? 0 : (barPos == BarPosition.RIGHT ? mc.getWindow().getWidth() - barWidth : 0);
                textX = getTextX(barPos, boxX, coordBoxWidthCalc - (barPos != BarPosition.MIDDLE ? barWidth : 0),
                        coordWidth, boxPadding, fixedBarX);
            }
            int textY = coordYPos + boxPadding;

            context.getMatrices().push();
            context.getMatrices().scale(textScale, textScale, 1.0f);
            // Clean text rendering - ensure pixel-perfect alignment
            float textXPos = Math.round(textX / textScale);
            float textYPos = Math.round(textY / textScale);
            context.drawText(mc.textRenderer, coordText, (int)textXPos, (int)textYPos, coordColor, false);
            context.getMatrices().pop();

            if (!useCustomPositions()) {
                centerY += coordBoxHeight + centerSpacing;
            }
        }

        if (showTotems.getValue()) {
            int totemCount = getTotemCount();
            String totemText = totemFormat.getValue().replace("{value}", String.valueOf(totemCount));
            int totemWidth = (int) (mc.textRenderer.getWidth(totemText) * textScale);
            int totemBoxHeight = textHeight + (boxPadding * 2);
            int totemXPos = useCustomPositions() ? totemsX.getValue().intValue()
                    : (mc.getWindow().getWidth() - totemWidth - boxPadding * 2 - barWidth) / 2;
            int totemYPos = useCustomPositions() ? totemsY.getValue().intValue() : centerY;

            int totemBoxWidth;
            int boxX;
            int totemColor;

            if (theme.isModernStyle()) {
                // Midnight theme: no side bars
                totemBoxWidth = totemWidth + (boxPadding * 2);
                boxX = totemXPos;
                totemColor = textColor;

                if (useShadows) {
                    // Shadow offset for depth
                    RenderUtils.fillRoundRect(context, boxX + 1, totemYPos + 1, totemBoxWidth, totemBoxHeight, radius, radius,
                            radius, radius, shadowColor);
                }
                RenderUtils.fillRoundRect(context, boxX, totemYPos, totemBoxWidth, totemBoxHeight, radius, radius,
                        radius, radius, backgroundColor);
                if (useBorders) {
                    RenderUtils.drawRoundRect(context, boxX, totemYPos, totemBoxWidth, totemBoxHeight, radius, borderColor);
                }
            } else {
                // Default theme: original style with side bars
                BarPosition barPos = useCustomPositions() ? getBarPosition(totemXPos, mc.getWindow().getWidth())
                        : BarPosition.MIDDLE;
                boolean hasBar = barPos != BarPosition.MIDDLE;
                totemBoxWidth = totemWidth + (boxPadding * 2) + (hasBar ? barWidth : 0);
                totemColor = getColorAtHeight(totemYPos + boxPadding, totalLeftHeight);

                int fixedBarX;
                int originalBoxX = totemXPos;
                boxX = totemXPos;
                if (barPos == BarPosition.LEFT) {
                    fixedBarX = originalBoxX - barWidth;
                } else if (barPos == BarPosition.RIGHT) {
                    fixedBarX = originalBoxX + (totemBoxWidth - barWidth);
                } else {
                    fixedBarX = 0;
                    boxX = totemXPos;
                    originalBoxX = boxX;
                }

                if (barPos == BarPosition.LEFT) {
                    RenderUtils.fillRoundRect(context, boxX, totemYPos, totemBoxWidth - barWidth, totemBoxHeight, 0, radius,
                            radius, 0, backgroundColor);
                    context.fill(fixedBarX, totemYPos, fixedBarX + barWidth, totemYPos + totemBoxHeight, totemColor);
                } else if (barPos == BarPosition.RIGHT) {
                    RenderUtils.fillRoundRect(context, boxX, totemYPos, totemBoxWidth - barWidth, totemBoxHeight, radius, 0,
                            0, radius, backgroundColor);
                    context.fill(fixedBarX, totemYPos, fixedBarX + barWidth, totemYPos + totemBoxHeight, totemColor);
                } else {
                    RenderUtils.fillRoundRect(context, boxX, totemYPos, totemBoxWidth, totemBoxHeight, radius, radius,
                            radius, radius, backgroundColor);
                }
            }

            int totemTextX;
            if (theme.isModernStyle()) {
                totemTextX = boxX + boxPadding;
            } else {
                BarPosition barPos = useCustomPositions() ? getBarPosition(totemXPos, mc.getWindow().getWidth())
                        : BarPosition.MIDDLE;
                int totemBoxWidthCalc = totemWidth + (boxPadding * 2) + (barPos != BarPosition.MIDDLE ? barWidth : 0);
                int fixedBarX = barPos == BarPosition.LEFT ? 0 : (barPos == BarPosition.RIGHT ? mc.getWindow().getWidth() - barWidth : 0);
                totemTextX = getTextX(barPos, boxX, totemBoxWidthCalc - (barPos != BarPosition.MIDDLE ? barWidth : 0),
                        totemWidth, boxPadding, fixedBarX);
            }
            int totemTextY = totemYPos + boxPadding;

            context.getMatrices().push();
            context.getMatrices().scale(textScale, textScale, 1.0f);
            // Clean text rendering - ensure pixel-perfect alignment
            float textXPos = Math.round(totemTextX / textScale);
            float textYPos = Math.round(totemTextY / textScale);
            context.drawText(mc.textRenderer, totemText, (int)textXPos, (int)textYPos, totemColor, false);
            context.getMatrices().pop();

            if (!useCustomPositions()) {
                centerY += totemBoxHeight + centerSpacing;
            }
        }

        if (showModuleList.getValue()) {
            int moduleItemPadding = (int) (4 * scale);
            int itemHeight = textHeight + (moduleItemPadding * 2);

            boolean isRight, isMiddle, isLeft;
            BarPosition barPos;
            if (useCustomPositions()) {
                barPos = getBarPosition(moduleListX.getValue().intValue(), mc.getWindow().getWidth());
                isLeft = barPos == BarPosition.LEFT;
                isMiddle = barPos == BarPosition.MIDDLE;
                isRight = barPos == BarPosition.RIGHT;
            } else {
                isRight = true;
                isMiddle = false;
                isLeft = false;
                barPos = BarPosition.RIGHT;
            }

            int moduleListYOffset = useCustomPositions() ? moduleListY.getValue().intValue()
                    : (isRight ? 16 : currentY + 4);
            int yOffset = moduleListYOffset;
            int moduleIndex = 0;
            int totalModules = (int) enabledModules.stream()
                    .filter(m -> !(m instanceof ClickGUI
                            || m instanceof HUD))
                    .count();

            for (Module module : enabledModules) {
                if (module instanceof ClickGUI)
                    continue;
                if (module instanceof HUD)
                    continue;
                String moduleName = module.getName();
                if (lowercase.getValue()) {
                    moduleName = moduleName.toLowerCase();
                } else {
                    moduleName = toTitleCase(moduleName);
                }

                if (moduleName == null || moduleName.isEmpty()) {
                    continue;
                }

                int nameWidth = (int) (mc.textRenderer.getWidth(moduleName) * textScale);

                if (nameWidth <= 0) {
                    continue;
                }

                int backgroundY1 = yOffset - moduleItemPadding;
                int backgroundY2 = yOffset + textHeight + moduleItemPadding;

                // Use theme text color
                int barColor;
                if (theme.isModernStyle()) {
                    barColor = textColor;
                } else {
                    barColor = isRight ? getGradientColor(moduleIndex, totalModules)
                            : getColorAtHeight(currentPixelY + (backgroundY2 - backgroundY1) / 2, totalLeftHeight);
                }

                if (isRight) {
                    int boxWidth;
                    int boxX;
                    int textX;

                    if (theme.isModernStyle()) {
                        // Midnight theme: no side bars
                        boxWidth = nameWidth + boxPadding * 2 + 2;
                        boxX = useCustomPositions() ? moduleListX.getValue().intValue()
                                : (mc.getWindow().getWidth() - boxWidth);
                        textX = boxX + (boxWidth - nameWidth) / 2;

                        if (boxWidth > 0) {
                            if (useShadows) {
                                // Multi-layer shadow for better depth
                                int shadowAlpha1 = (shadowColor >> 24) & 0xFF;
                                int shadowAlpha2 = Math.max(0, shadowAlpha1 - 40);
                                int shadowAlpha3 = Math.max(0, shadowAlpha2 - 30);
                                int shadowColor1 = (shadowAlpha1 << 24) | (shadowColor & 0x00FFFFFF);
                                int shadowColor2 = (shadowAlpha2 << 24) | (shadowColor & 0x00FFFFFF);
                                int shadowColor3 = (shadowAlpha3 << 24) | (shadowColor & 0x00FFFFFF);

                                RenderUtils.fillRoundRect(context, boxX + 3, backgroundY1 + 3, boxWidth,
                                        backgroundY2 - backgroundY1, radius, radius, radius, radius, shadowColor3);
                                RenderUtils.fillRoundRect(context, boxX + 2, backgroundY1 + 2, boxWidth,
                                        backgroundY2 - backgroundY1, radius, radius, radius, radius, shadowColor2);
                                RenderUtils.fillRoundRect(context, boxX + 1, backgroundY1 + 1, boxWidth,
                                        backgroundY2 - backgroundY1, radius, radius, radius, radius, shadowColor1);
                            }
                            RenderUtils.fillRoundRect(context, boxX, backgroundY1, boxWidth,
                                    backgroundY2 - backgroundY1, radius, radius, radius, radius, backgroundColor);
                            if (useBorders) {
                                // Enhanced border with gradient effect
                                RenderUtils.drawRoundRect(context, boxX, backgroundY1, boxWidth,
                                        backgroundY2 - backgroundY1, radius, borderColor);
                                // Inner border highlight
                                int highlightBorder = RadiumGuiTheme.applyAlpha(borderColor, 0.3f);
                                RenderUtils.drawRoundRect(context, boxX + 1, backgroundY1 + 1, boxWidth - 2,
                                        backgroundY2 - backgroundY1 - 2, Math.max(0, radius - 1), highlightBorder);
                            }
                        }
                    } else {
                        // Default theme: original style with side bars
                        boxWidth = nameWidth + boxPadding * 2 + barWidth + 2;
                        int fixedBarX = mc.getWindow().getWidth() - barWidth;
                        boxX = fixedBarX - (boxWidth - barWidth);
                        textX = getTextX(BarPosition.RIGHT, boxX, boxWidth - barWidth, nameWidth, boxPadding,
                                fixedBarX);

                        if (boxWidth > 0) {
                            RenderUtils.fillRoundRect(context, boxX, backgroundY1, boxWidth - barWidth,
                                    backgroundY2 - backgroundY1, radius, 0, 0, radius, backgroundColor);
                            context.fill(fixedBarX, backgroundY1, fixedBarX + barWidth, backgroundY2, barColor);
                        }
                    }

                    context.getMatrices().push();
                    context.getMatrices().scale(textScale, textScale, 1.0f);
                    // Clean text rendering - ensure pixel-perfect alignment
                    float textXPos = Math.round(textX / textScale);
                    float textYPos = Math.round(yOffset / textScale);
                    context.drawText(mc.textRenderer, moduleName, (int)textXPos, (int)textYPos, barColor, false);
                    context.getMatrices().pop();
                } else if (isMiddle) {
                    int boxWidth = nameWidth + boxPadding * 2;

                    int centerX = useCustomPositions() ? moduleListX.getValue().intValue()
                            : (mc.getWindow().getWidth() / 2);
                    int baseX = centerX - (boxWidth / 2);
                    int textX = baseX + boxPadding;

                    if (boxWidth > 0) {
                        // Add subtle shadow for depth
                        RenderUtils.fillRoundRect(context, baseX + 1, backgroundY1 + 1, boxWidth, backgroundY2 - backgroundY1,
                                radius, radius, radius, radius, shadowColor);

                        RenderUtils.fillRoundRect(context, baseX, backgroundY1, boxWidth, backgroundY2 - backgroundY1,
                                radius, radius, radius, radius, backgroundColor);

                        // Add subtle border
                        RenderUtils.drawRoundRect(context, baseX, backgroundY1, boxWidth, backgroundY2 - backgroundY1,
                                radius, borderColor);

                        context.getMatrices().push();
                        context.getMatrices().scale(textScale, textScale, 1.0f);
                        // Clean text rendering
                        context.drawText(mc.textRenderer, moduleName, (int)(textX / textScale), (int)(yOffset / textScale), barColor, false);
                        context.getMatrices().pop();
                    }
                } else {
                    int boxWidth;
                    int boxX;
                    int textX;
                    int moduleColor;

                    if (theme.isModernStyle()) {
                        // Midnight/Sci-Fi theme: no side bars
                        boxWidth = nameWidth + boxPadding * 2 + 2;
                        boxX = useCustomPositions() ? moduleListX.getValue().intValue() : 0;
                        textX = boxX + (boxWidth - nameWidth) / 2;
                        moduleColor = textColor;

                        if (boxWidth > 0) {
                            if (useShadows) {
                                // Multi-layer shadow for better depth
                                int shadowAlpha1 = (shadowColor >> 24) & 0xFF;
                                int shadowAlpha2 = Math.max(0, shadowAlpha1 - 40);
                                int shadowAlpha3 = Math.max(0, shadowAlpha2 - 30);
                                int shadowColor1 = (shadowAlpha1 << 24) | (shadowColor & 0x00FFFFFF);
                                int shadowColor2 = (shadowAlpha2 << 24) | (shadowColor & 0x00FFFFFF);
                                int shadowColor3 = (shadowAlpha3 << 24) | (shadowColor & 0x00FFFFFF);

                                RenderUtils.fillRoundRect(context, boxX + 3, backgroundY1 + 3, boxWidth, backgroundY2 - backgroundY1, radius,
                                        radius, radius, radius, shadowColor3);
                                RenderUtils.fillRoundRect(context, boxX + 2, backgroundY1 + 2, boxWidth, backgroundY2 - backgroundY1, radius,
                                        radius, radius, radius, shadowColor2);
                                RenderUtils.fillRoundRect(context, boxX + 1, backgroundY1 + 1, boxWidth, backgroundY2 - backgroundY1, radius,
                                        radius, radius, radius, shadowColor1);
                            }
                            RenderUtils.fillRoundRect(context, boxX, backgroundY1, boxWidth, backgroundY2 - backgroundY1, radius,
                                    radius, radius, radius, backgroundColor);
                            if (useBorders) {
                                // Enhanced border with gradient effect
                                RenderUtils.drawRoundRect(context, boxX, backgroundY1, boxWidth, backgroundY2 - backgroundY1, radius,
                                        borderColor);
                                // Inner border highlight
                                int highlightBorder = RadiumGuiTheme.applyAlpha(borderColor, 0.3f);
                                RenderUtils.drawRoundRect(context, boxX + 1, backgroundY1 + 1, boxWidth - 2, backgroundY2 - backgroundY1 - 2, Math.max(0, radius - 1), highlightBorder);
                            }
                        }
                    } else {
                        // Default theme: original style with side bars
                        int moduleListXPos = moduleListX.getValue().intValue();
                        boxWidth = nameWidth + boxPadding * 2 + 2;
                        boxX = moduleListXPos;

                        int fixedBarX = boxX - barWidth;
                        // Center text within the new wider box
                        textX = boxX + (boxWidth - nameWidth - barWidth) / 2;

                        int moduleBarHeight = backgroundY2 - backgroundY1;
                        moduleColor = getColorAtHeight(currentPixelY + moduleBarHeight / 2, totalLeftHeight);

                        if (boxWidth > 0) {
                            RenderUtils.fillRoundRect(context, boxX, backgroundY1, boxWidth, backgroundY2 - backgroundY1, 0,
                                    radius, radius, 0, backgroundColor);

                            if (useGradient.getValue() && totalLeftHeight > 1) {
                                drawVerticalGradientBar(context, fixedBarX, backgroundY1, barWidth, moduleBarHeight,
                                        currentPixelY, totalLeftHeight);
                            } else {
                                context.fill(fixedBarX, backgroundY1, fixedBarX + barWidth, backgroundY2, moduleColor);
                            }
                        }
                    }

                    context.getMatrices().push();
                    context.getMatrices().scale(textScale, textScale, 1.0f);
                    context.drawText(mc.textRenderer, moduleName, (int)(textX / textScale), (int)(yOffset / textScale), moduleColor, false);
                    context.getMatrices().pop();

                    if (!theme.isModernStyle()) {
                        int moduleBarHeight = backgroundY2 - backgroundY1;
                        currentPixelY += moduleBarHeight + 4;
                    }
                }
                yOffset += itemHeight + 4;
                moduleIndex++;
            }
        }

        if (showTargetHUD.getValue() && mc.player != null && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
            net.minecraft.entity.Entity targetEntity = ((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget)
                    .getEntity();

            if (targetEntity instanceof net.minecraft.entity.player.PlayerEntity targetPlayer
                    && targetPlayer != mc.player) {
                String targetName = targetPlayer.getName().getString();
                float health = targetPlayer.getHealth();
                float maxHealth = targetPlayer.getMaxHealth();
                float healthPercent = Math.max(0, Math.min(1, health / maxHealth));

                int headSize = (int) (32 * scale);
                int healthBarHeight = (int) (4 * scale);
                int healthBarWidth = (int) (100 * scale);
                int padding = (int) (10 * scale);

                int nameWidth = mc.textRenderer.getWidth(targetName);

                String healthText = String.format("%.1f/%.1f", health, maxHealth);
                int healthTextWidth = mc.textRenderer.getWidth(healthText);


                int contentWidth = Math.max(nameWidth, Math.max(healthTextWidth, healthBarWidth));
                int boxWidth = headSize + padding + contentWidth + (padding * 2);
                int boxHeight = headSize + (padding * 2);

                int targetXPos = useCustomPositions() ? targetHUDX.getValue().intValue()
                        : (mc.getWindow().getWidth() - boxWidth - 8);
                int targetYPos = useCustomPositions() ? targetHUDY.getValue().intValue() : 8;

                int boxX = targetXPos;

                int spotiplayBgColor = 0xFF131A2E;

                RenderUtils.fillRoundRect(context, boxX, targetYPos, boxWidth, boxHeight, 8, spotiplayBgColor);

                int headX = boxX + padding;
                int headY = targetYPos + padding;
                try {
                    net.minecraft.client.network.PlayerListEntry playerEntry = mc.getNetworkHandler() != null
                            ? mc.getNetworkHandler().getPlayerListEntry(targetPlayer.getUuid())
                            : null;
                    if (playerEntry != null) {
                        net.minecraft.client.util.SkinTextures skinTextures = playerEntry.getSkinTextures();
                        RenderUtils.drawRoundedPlayerHead(context, skinTextures, headX, headY, headSize, 4);
                    } else {
                        RenderUtils.fillRoundRect(context, headX, headY, headSize, headSize, 4, 0xFF1A2235);
                    }
                } catch (Exception e) {
                    RenderUtils.fillRoundRect(context, headX, headY, headSize, headSize, 4, 0xFF1A2235);
                }

                int textX = headX + headSize + padding;
                int nameY = headY + 2;
                int healthTextY = nameY + 14;
                int healthBarY = healthTextY + 12;

                // Clean text rendering - ensure pixel-perfect alignment
                float textXPos = Math.round(textX);
                float nameYPos = Math.round(nameY);
                float healthYPos = Math.round(healthTextY);
                context.drawText(mc.textRenderer, targetName, (int)textXPos, (int)nameYPos, 0xFFFFFFFF, false);

                context.drawText(mc.textRenderer, healthText, (int)textXPos, (int)healthYPos, 0xFFFFFFFF, false);

                int healthBarBgColor = 0xFF2A3548;
                RenderUtils.fillRoundRect(context, textX, healthBarY, healthBarWidth, healthBarHeight, 2, healthBarBgColor);

                int healthBarFillWidth = (int) (healthBarWidth * healthPercent);
                if (healthBarFillWidth > 0) {
                    int healthColor = 0xFF1DB954;
                    RenderUtils.fillRoundRect(context, textX, healthBarY, healthBarFillWidth, healthBarHeight, 2, healthColor);
                }
            }
        }

        if (showRegionMap.getValue()) {
            int cellSize = regionMapCellSize.getValue().intValue();
            int mapX = useCustomPositions() ? regionMapX.getValue().intValue() : 100;
            int mapY = useCustomPositions() ? regionMapY.getValue().intValue() : 100;

            regionMapComponent.render(context, mapX, mapY, cellSize,
                    regionMapTransparency.getValue(),
                    true,
                    regionMapShowLabels.getValue(),
                    regionMapShowCoords.getValue(),
                    regionMapShowPlayer.getValue(),
                    regionMapBgColor.getValue(),
                    regionMapGridColor.getValue(),
                    regionMapPlayerColor.getValue());
        }

        RenderUtils.scaledProjection();
    }

    private int getHealthColor(float healthPercent) {
        if (healthPercent > 0.6f) {
            float ratio = (healthPercent - 0.6f) / 0.4f;
            return interpolateColor(new Color(0, 255, 0), new Color(255, 255, 0), 1.0f - ratio);
        } else if (healthPercent > 0.2f) {
            float ratio = (healthPercent - 0.2f) / 0.4f;
            return interpolateColor(new Color(255, 255, 0), new Color(255, 0, 0), 1.0f - ratio);
        } else {
            return new Color(255, 0, 0).getRGB();
        }
    }

    private int getHudColor() {
        Color color = hudColor.getValue();
        return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    public int getHudColorInt() {
        return getHudColor();
    }

    private void drawVerticalGradientBar(DrawContext context, int x, int y, int width, int height, int startPixelY,
                                         int totalHeight) {
        Color primary = hudColor.getValue();
        Color secondary = secondaryColor.getValue();

        for (int i = 0; i < height; i++) {
            float ratio = totalHeight > 1 ? (float) (startPixelY + i) / (totalHeight - 1) : 0;
            ratio = Math.max(0, Math.min(1, ratio));
            int color = interpolateColor(primary, secondary, ratio);
            RenderUtils.fillRect(context, x, y + i, width, 1, color);
        }
    }

    public enum ModulePos {
        LEFT,
        MIDDLE,
        RIGHT
    }

    public enum WatermarkStyle {
        NORMAL,
        LOWERCASE,
        UPPERCASE
    }

    public enum BarPosition {
        LEFT,
        MIDDLE,
        RIGHT
    }

}
