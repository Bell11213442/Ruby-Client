package com.radium.client.gui;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.HUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;


public class HudEditorScreen extends Screen {
    private static final double SIDE_SNAP_THRESHOLD = 5.0;
    private static final double CENTER_SNAP_THRESHOLD = 5.0;
    private final HUD hudModule;
    private String draggingElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean snapToGrid = true;
    private int gridSize = 2;
    private boolean showVerticalSnapLine = false;
    private boolean showHorizontalSnapLine = false;
    private int verticalSnapX = 0;
    private final int horizontalSnapY = 0;
    private int tempWatermarkX, tempWatermarkY;
    private int tempInfoLinesX, tempInfoLinesY;
    private int tempKeybindsX, tempKeybindsY;
    private int tempCoordinatesX, tempCoordinatesY;
    private int tempTotemsX, tempTotemsY;
    private int tempModuleListX, tempModuleListY;
    private int tempTargetHUDX, tempTargetHUDY;
    private int tempRegionMapX, tempRegionMapY;
    private int tempMediaPlayerX, tempMediaPlayerY;
    private final com.radium.client.modules.client.MediaPlayer mediaPlayerModule;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
        this.hudModule = RadiumClient.moduleManager.getModule(HUD.class);

        if (hudModule != null) {
            tempWatermarkX = hudModule.getWatermarkX().getValue().intValue();
            tempWatermarkY = hudModule.getWatermarkY().getValue().intValue();
            tempInfoLinesX = hudModule.getInfoLinesX().getValue().intValue();
            tempInfoLinesY = hudModule.getInfoLinesY().getValue().intValue();
            tempKeybindsX = hudModule.getKeybindsX().getValue().intValue();
            tempKeybindsY = hudModule.getKeybindsY().getValue().intValue();
            tempCoordinatesX = hudModule.getCoordinatesX().getValue().intValue();
            tempCoordinatesY = hudModule.getCoordinatesY().getValue().intValue();
            tempTotemsX = hudModule.getTotemsX().getValue().intValue();
            tempTotemsY = hudModule.getTotemsY().getValue().intValue();
            tempModuleListX = hudModule.getModuleListX().getValue().intValue();
            tempModuleListY = hudModule.getModuleListY().getValue().intValue();
            tempTargetHUDX = hudModule.getTargetHUDX().getValue().intValue();
            tempTargetHUDY = hudModule.getTargetHUDY().getValue().intValue();
            tempRegionMapX = hudModule.getRegionMapX().getValue().intValue();
            tempRegionMapY = hudModule.getRegionMapY().getValue().intValue();
        }

        mediaPlayerModule = RadiumClient.moduleManager.getModule(com.radium.client.modules.client.MediaPlayer.class);
        if (mediaPlayerModule != null) {
            tempMediaPlayerX = mediaPlayerModule.getPosX().getValue().intValue();
            tempMediaPlayerY = mediaPlayerModule.getPosY().getValue().intValue();
        }
    }

    @Override
    protected void init() {
        this.width = client.getWindow().getFramebufferWidth() / 2;
        this.height = client.getWindow().getFramebufferHeight() / 2;
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonY = this.height - 30;
        int saveX = this.width / 2 - buttonWidth - 10;
        int cancelX = this.width / 2 + 10;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            savePositions();
            this.client.setScreen(null);
        }).dimensions(saveX, buttonY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            this.client.setScreen(null);
        }).dimensions(cancelX, buttonY, buttonWidth, buttonHeight).build());
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        int scaledWidth = client.getWindow().getFramebufferWidth() / 2;
        int scaledHeight = client.getWindow().getFramebufferHeight() / 2;
        super.resize(client, scaledWidth, scaledHeight);
    }

    private void savePositions() {
        if (hudModule == null)
            return;

        if (RadiumClient.getConfigManager() != null) {
            RadiumClient.getConfigManager().saveSettings();
            RadiumClient.getConfigManager().saveHudPositions();
        }
    }

    private void updatePositions() {
        if (hudModule == null)
            return;

        hudModule.getWatermarkX().setValue((double) tempWatermarkX);
        hudModule.getWatermarkY().setValue((double) tempWatermarkY);
        hudModule.getInfoLinesX().setValue((double) tempInfoLinesX);
        hudModule.getInfoLinesY().setValue((double) tempInfoLinesY);
        hudModule.getKeybindsX().setValue((double) tempKeybindsX);
        hudModule.getKeybindsY().setValue((double) tempKeybindsY);
        hudModule.getCoordinatesX().setValue((double) tempCoordinatesX);
        hudModule.getCoordinatesY().setValue((double) tempCoordinatesY);
        hudModule.getTotemsX().setValue((double) tempTotemsX);
        hudModule.getTotemsY().setValue((double) tempTotemsY);
        hudModule.getModuleListX().setValue((double) tempModuleListX);
        hudModule.getModuleListY().setValue((double) tempModuleListY);
        hudModule.getTargetHUDX().setValue((double) tempTargetHUDX);
        hudModule.getTargetHUDY().setValue((double) tempTargetHUDY);
        hudModule.getRegionMapX().setValue((double) tempRegionMapX);
        hudModule.getRegionMapY().setValue((double) tempRegionMapY);

        if (mediaPlayerModule != null) {
            mediaPlayerModule.getPosX().setValue((double) tempMediaPlayerX);
            mediaPlayerModule.getPosY().setValue((double) tempMediaPlayerY);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        com.radium.client.utils.render.RenderUtils.unscaledProjection();
        int unscaledWidth = this.client.getWindow().getFramebufferWidth();
        int unscaledHeight = this.client.getWindow().getFramebufferHeight();
        int bg = 0x22000000;
        context.fill(0, 0, unscaledWidth, unscaledHeight, bg);

        if (snapToGrid) {
            drawGrid(context);
        }

        com.radium.client.utils.render.RenderUtils.scaledProjection();

        super.render(context, mouseX, mouseY, delta);

        com.radium.client.utils.render.RenderUtils.unscaledProjection();

        updatePositions();

        if (hudModule != null && RadiumClient.mc.player != null) {
            hudModule.render(context, delta);

            if (hudModule.showTargetHUD.getValue()) {
                drawPreviewTargetHUD(context);
            }

            drawElementOutlines(context, draggingElement);
        }

        if (mediaPlayerModule != null && mediaPlayerModule.isEnabled()) {
            mediaPlayerModule.render(context, delta);
        }

        if (snapToGrid) {
            int crosshairCenterX = unscaledWidth / 2;
            int crosshairCenterY = unscaledHeight / 2;
            int crosshairColor = 0xFF00FF00;
            context.fill(crosshairCenterX - 1, 0, crosshairCenterX + 1, unscaledHeight, crosshairColor);
            context.fill(0, crosshairCenterY - 1, unscaledWidth, crosshairCenterY + 1, crosshairColor);
        }

        if (showVerticalSnapLine) {
            int snapColor = 0xFF00FF00;
            context.fill(verticalSnapX - 1, 0, verticalSnapX + 1, unscaledHeight, snapColor);
        }
        if (showHorizontalSnapLine) {
            int snapColor = 0xFF00FF00;
            context.fill(0, horizontalSnapY - 1, unscaledWidth, horizontalSnapY + 1, snapColor);
        }

        com.radium.client.utils.render.RenderUtils.scaledProjection();

        int scaledWidth = this.width;
        int scaledHeight = this.height;
        int scaledCenterX = scaledWidth / 2;
        int scaledCenterY = scaledHeight / 2;

        String scaledInstructionText = "Click and drag HUD elements";
        int scaledTextWidth = this.textRenderer.getWidth(scaledInstructionText);
        int scaledBoxWidth = scaledTextWidth + 12;
        int scaledBoxHeight = 16;
        int scaledBoxX = scaledCenterX - (scaledBoxWidth / 2);
        int scaledBoxY = scaledCenterY - 15;

        context.fill(scaledBoxX, scaledBoxY, scaledBoxX + scaledBoxWidth, scaledBoxY + scaledBoxHeight, 0xE6131A2E);
        context.drawCenteredTextWithShadow(this.textRenderer, scaledInstructionText, scaledCenterX, scaledBoxY + 4, 0xFFB0B8C8);

        String scaledGridText = "Grid(G): " + (snapToGrid ? "ON" : "OFF");
        int scaledGridWidth = this.textRenderer.getWidth(scaledGridText);
        int scaledGridBoxWidth = scaledGridWidth + 12;
        int scaledGridBoxX = scaledCenterX - (scaledGridBoxWidth / 2);
        int scaledGridBoxY = scaledCenterY + 5;

        context.fill(scaledGridBoxX, scaledGridBoxY, scaledGridBoxX + scaledGridBoxWidth, scaledGridBoxY + scaledBoxHeight, 0xE6131A2E);
        context.drawCenteredTextWithShadow(this.textRenderer, scaledGridText, scaledCenterX, scaledGridBoxY + 4, 0xFFB0B8C8);
    }

    private void drawGrid(DrawContext context) {
        int unscaledWidth = this.client.getWindow().getFramebufferWidth();
        int unscaledHeight = this.client.getWindow().getFramebufferHeight();
        int gridColor = 0x33000000;
        for (int gx = 0; gx < unscaledWidth; gx += gridSize) {
            com.radium.client.utils.render.RenderUtils.drawVerLine(context, gx, 0, unscaledHeight, gridColor);
        }
        for (int gy = 0; gy < unscaledHeight; gy += gridSize) {
            com.radium.client.utils.render.RenderUtils.drawHorLine(context, 0, gy, unscaledWidth, gridColor);
        }
    }

    private int snapToGrid(int coord) {
        if (!snapToGrid)
            return coord;
        return (coord / gridSize) * gridSize;
    }

    private int snapToEdgesOrCenter(String element, int x) {
        if (hudModule == null || this.client == null || this.client.getWindow() == null)
            return x;

        int screenWidth = this.client.getWindow().getFramebufferWidth();
        int centerX = screenWidth / 2;
        int elementWidth = getElementWidth(element);

        showVerticalSnapLine = false;
        showHorizontalSnapLine = false;

        if (x >= -SIDE_SNAP_THRESHOLD && x <= SIDE_SNAP_THRESHOLD) {
            setCenteredFlag(element, false);
            showVerticalSnapLine = true;
            verticalSnapX = 0;
            return 0;
        }

        int rightAlignedX = Math.max(0, screenWidth - elementWidth);
        if (x >= rightAlignedX - SIDE_SNAP_THRESHOLD && x <= screenWidth) {
            setCenteredFlag(element, false);
            showVerticalSnapLine = true;
            verticalSnapX = screenWidth;
            return rightAlignedX;
        }

        int elementCenterX = x + elementWidth / 2;
        if (Math.abs(elementCenterX - centerX) <= CENTER_SNAP_THRESHOLD) {
            setCenteredFlag(element, true);
            showVerticalSnapLine = true;
            verticalSnapX = centerX;
            return centerX - (elementWidth / 2);
        }

        setCenteredFlag(element, false);
        return x;
    }

    private void setCenteredFlag(String element, boolean centered) {
        if (hudModule == null)
            return;
        switch (element) {
            case "watermark":
                hudModule.getWatermarkCentered().setValue(centered);
                break;
            case "infoLines":
                hudModule.getInfoLinesCentered().setValue(centered);
                break;
            case "keybinds":
                hudModule.getKeybindsCentered().setValue(centered);
                break;
            case "coordinates":
                hudModule.getCoordinatesCentered().setValue(centered);
                break;
            case "totems":
                hudModule.getTotemsCentered().setValue(centered);
                break;
            case "moduleList":
                hudModule.getModuleListCentered().setValue(centered);
                break;
            case "targetHUD":
                hudModule.getTargetHUDCentered().setValue(centered);
                break;
        }
    }

    private int getElementWidth(String element) {
        if (hudModule == null)
            return 0;
        var mc = RadiumClient.mc;
        if (mc == null || mc.getWindow() == null)
            return 0;

        var clickGUI = RadiumClient.moduleManager.getModule(com.radium.client.modules.client.ClickGUI.class);
        
        int textHeight = mc.textRenderer.fontHeight * 2;
        int boxPadding = 8;
        int barWidth = 4;
        int itemPadding = 6;

        switch (element) {
            case "watermark": {
                String watermarkText;
                HUD.WatermarkStyle style = (HUD.WatermarkStyle) hudModule.watermark.getValue();
                if (style == HUD.WatermarkStyle.LOWERCASE) {
                    watermarkText = "radium";
                } else if (style == HUD.WatermarkStyle.UPPERCASE) {
                    watermarkText = "RADIUM";
                } else {
                    watermarkText = "Radium";
                }
                int width = mc.textRenderer.getWidth(watermarkText) * 2;
                return width + (boxPadding * 2);
            }
            case "infoLines": {
                java.util.List<String> infoLines = new java.util.ArrayList<>();
                if (hudModule.showFPS.getValue()) {
                    infoLines.add(
                            hudModule.getFpsFormat().getValue().replace("{value}", String.valueOf(mc.getCurrentFps())));
                }
                if (hudModule.showPing.getValue()) {
                    infoLines.add(hudModule.getPing());
                }
                if (hudModule.showServerIP.getValue()) {
                    String serverText = "Singleplayer";
                    if (mc.getCurrentServerEntry() != null) {
                        serverText = mc.getCurrentServerEntry().address;
                    }
                    infoLines.add(serverText);
                }

                int maxWidth = 0;
                for (String line : infoLines) {
                    int w = mc.textRenderer.getWidth(line) * 2;
                    maxWidth = Math.max(maxWidth, w);
                }
                return maxWidth + (boxPadding * 2);
            }
            case "keybinds": {
                var modulesWithKeybinds = com.radium.client.client.RadiumClient.moduleManager.getModules().stream()
                        .filter(m -> m.getKeyBind() != -1 && !m.getName().equals("Radium"))
                        .sorted(java.util.Comparator.comparing(com.radium.client.modules.Module::getName))
                        .collect(java.util.stream.Collectors.toList());
                if (modulesWithKeybinds.isEmpty())
                    return 0;

                String labelText = hudModule.lowercaseKeybinds.getValue() ? "keybinds" : "Keybinds";
                float labelWidth = mc.textRenderer.getWidth(labelText) * 2;
                int maxWidth = (int) labelWidth;
                for (var module : modulesWithKeybinds) {
                    String moduleName = hudModule.lowercase.getValue() ? module.getName().toLowerCase()
                            : module.getName();
                    if (hudModule.lowercaseKeybinds.getValue())
                        moduleName = moduleName.toLowerCase();
                    String keybindName = com.radium.client.client.KeybindManager.getKeyName(module.getKeyBind());
                    if (hudModule.lowercaseKeybinds.getValue())
                        keybindName = keybindName.toLowerCase();
                    float lineWidth = mc.textRenderer
                            .getWidth(moduleName + " - " + keybindName) * 2;
                    maxWidth = Math.max(maxWidth, (int) lineWidth);
                }
                return maxWidth + (boxPadding * 2);
            }
            case "coordinates": {
                if (mc.player == null)
                    return 0;
                int x = mc.player.getBlockPos().getX();
                int y = mc.player.getBlockPos().getY();
                int z = mc.player.getBlockPos().getZ();
                String coordText = hudModule.getCoordsFormat().getValue().replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y))
                        .replace("{z}", String.valueOf(z));
                int width = mc.textRenderer.getWidth(coordText) * 2;
                return width + (boxPadding * 2);
            }
            case "totems": {
                int totemCount = 0;
                if (mc.player != null) {
                    for (var stack : mc.player.getInventory().main) {
                        if (stack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING)
                            totemCount += stack.getCount();
                    }
                    var offhand = mc.player.getOffHandStack();
                    if (offhand.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING)
                        totemCount += offhand.getCount();
                }
                String totemText = hudModule.getTotemFormat().getValue().replace("{value}", String.valueOf(totemCount));
                int width = mc.textRenderer.getWidth(totemText) * 2;
                return width + (boxPadding * 2);
            }
            case "targetHUD": {
                return 250;
            }
            case "moduleList": {
                var enabledModules = com.radium.client.client.RadiumClient.moduleManager.getModules().stream()
                        .filter(com.radium.client.modules.Module::isEnabled)
                        .filter(m -> !(m instanceof com.radium.client.modules.client.ClickGUI
                                || m instanceof com.radium.client.modules.client.HUD))
                        .collect(java.util.stream.Collectors.toList());
                if (enabledModules.isEmpty())
                    return 0;

                int maxWidth = 0;
                for (var module : enabledModules) {
                    String moduleName = hudModule.lowercase.getValue() ? module.getName().toLowerCase()
                            : module.getName();
                    int nameWidth = mc.textRenderer.getWidth(moduleName) * 2;
                    maxWidth = Math.max(maxWidth, nameWidth);
                }

                int screenWidth = mc.getWindow().getWidth();
                BarPosition barPos;
                int threshold = (int) SIDE_SNAP_THRESHOLD;
                if (tempModuleListX <= threshold) {
                    barPos = BarPosition.LEFT;
                } else if (tempModuleListX >= screenWidth - threshold) {
                    barPos = BarPosition.RIGHT;
                } else {
                    barPos = BarPosition.MIDDLE;
                }

                if (barPos == BarPosition.MIDDLE) {
                    return maxWidth + (boxPadding * 2);
                } else {

                    return maxWidth + boxPadding + 4 + barWidth;
                }
            }
            case "mediaPlayer": {
                if (mediaPlayerModule == null) return 280;
                if (mediaPlayerModule.getSettings().stream().anyMatch(s -> s.getName().equals("Width"))) {
                    return ((com.radium.client.gui.settings.NumberSetting) mediaPlayerModule.getSettings().stream().filter(s -> s.getName().equals("Width")).findFirst().get()).getValue().intValue();
                }
                return 280;
            }
            default:
                return 0;
        }
    }

    private int getElementHeight(String element) {
        if (hudModule == null)
            return 0;
        var mc = RadiumClient.mc;
        if (mc == null || mc.getWindow() == null)
            return 0;

        var clickGUI = RadiumClient.moduleManager.getModule(com.radium.client.modules.client.ClickGUI.class);
        // Always use Roboto font for HUD editor
        boolean useCustomFont = true;
        int textHeight = mc.textRenderer.fontHeight * 2;
        int boxPadding = 8;
        int itemPadding = 6;

        switch (element) {
            case "watermark":
            case "coordinates":
            case "totems":
                return textHeight + (boxPadding * 2);
            case "infoLines": {
                int lines = 0;
                if (hudModule.showFPS.getValue()) lines++;
                if (hudModule.showPing.getValue()) lines++;
                if (hudModule.showServerIP.getValue()) lines++;
                if (lines == 0) return 0;
                return (textHeight * lines) + (itemPadding * (lines - 1)) + (boxPadding * 2);
            }
            case "keybinds": {
                int count = (int) com.radium.client.client.RadiumClient.moduleManager.getModules().stream()
                        .filter(m -> m.getKeyBind() != -1 && !m.getName().equals("Radium")).count();
                if (count == 0) return 0;
                return (textHeight * (count + 1)) + (itemPadding * count) + (boxPadding * 2) + 4;
            }
            case "targetHUD":
                return 80;
            case "moduleList": {
                int count = (int) com.radium.client.client.RadiumClient.moduleManager.getModules().stream()
                        .filter(com.radium.client.modules.Module::isEnabled)
                        .filter(m -> !(m instanceof com.radium.client.modules.client.ClickGUI || m instanceof com.radium.client.modules.client.HUD))
                        .count();
                if (count == 0) return 0;
                int moduleItemPadding = 4;
                int itemHeight = textHeight + (moduleItemPadding * 2);
                return (itemHeight + 4) * count;
            }
            case "regionMap": {
                int cellSize = hudModule.regionMapCellSize.getValue().intValue();
                return hudModule.getRegionMapComponent().getHeight(cellSize, hudModule.regionMapShowCoords.getValue(), hudModule.regionMapShowLabels.getValue());
            }
            case "mediaPlayer": {
                int artSize = 64;
                int padding = 12;
                return artSize + padding * 2 + 8;
            }
            default:
                return 0;
        }
    }

    private void drawPreviewTargetHUD(DrawContext context) {
        if (hudModule == null || RadiumClient.mc.player == null)
            return;

        var mc = RadiumClient.mc;
        int headSize = 32;
        int healthBarHeight = 4;
        int healthBarWidth = 100;
        int padding = 10;

        String targetName = "PreviewPlayer";
        float health = 15.5f;
        float maxHealth = 20.0f;
        float healthPercent = Math.max(0, Math.min(1, health / maxHealth));

        // com.radium.client.font.Fonts.loadFonts(); removed

        int nameWidth = RadiumClient.mc.textRenderer.getWidth(targetName);
        String healthText = String.format("%.1f/%.1f", health, maxHealth);
        int healthTextWidth = RadiumClient.mc.textRenderer.getWidth(healthText);

        int contentWidth = Math.max(nameWidth, Math.max(healthTextWidth, healthBarWidth));
        int boxWidth = headSize + padding + contentWidth + (padding * 2);
        int boxHeight = headSize + (padding * 2);

        int targetXPos = tempTargetHUDX;
        int targetYPos = tempTargetHUDY;

        int boxX = hudModule.getTargetHUDCentered().getValue() ?
                (mc.getWindow().getWidth() / 2) - (boxWidth / 2) : targetXPos;

        com.radium.client.utils.render.RenderUtils.unscaledProjection();

        int spotiplayBgColor = 0xFF131A2E;
        com.radium.client.utils.render.RenderUtils.fillRoundRect(context, boxX, targetYPos, boxWidth, boxHeight, 8, spotiplayBgColor);

        int headX = boxX + padding;
        int headY = targetYPos + padding;
        try {
            net.minecraft.client.util.SkinTextures skinTextures = mc.player.getSkinTextures();
            com.radium.client.utils.render.RenderUtils.drawRoundedPlayerHead(context, skinTextures, headX, headY, headSize, 4);
        } catch (Exception e) {
            com.radium.client.utils.render.RenderUtils.fillRoundRect(context, headX, headY, headSize, headSize, 4, 0xFF1A2235);
        }

        int textX = headX + headSize + padding;
        int nameY = headY + 2;
        int healthTextY = nameY + 14;
        int healthBarY = healthTextY + 12;

        context.drawText(RadiumClient.mc.textRenderer, targetName, textX, nameY, 0xFFFFFFFF, false);

        context.drawText(RadiumClient.mc.textRenderer, healthText, textX, healthTextY, 0xFFFFFFFF, false);

        int healthBarBgColor = 0xFF2A3548;
        com.radium.client.utils.render.RenderUtils.fillRoundRect(context, textX, healthBarY, healthBarWidth, healthBarHeight, 2, healthBarBgColor);

        int healthBarFillWidth = (int) (healthBarWidth * healthPercent);
        if (healthBarFillWidth > 0) {
            int healthColor = 0xFF1DB954;
            com.radium.client.utils.render.RenderUtils.fillRoundRect(context, textX, healthBarY, healthBarFillWidth, healthBarHeight, 2, healthColor);
        }

        com.radium.client.utils.render.RenderUtils.scaledProjection();
    }

    private int getTextX(BarPosition barPos, int boxX, int boxWidth, int textWidth, int boxPadding, int fixedBarX) {
        if (barPos == BarPosition.LEFT) {
            return boxX + boxPadding;
        } else if (barPos == BarPosition.RIGHT) {
            return boxX + boxWidth - boxPadding - textWidth;
        } else {
            return boxX + (boxWidth / 2) - (textWidth / 2);
        }
    }

    private int getHealthColor(float healthPercent) {
        if (healthPercent > 0.6f) {
            float ratio = (healthPercent - 0.6f) / 0.4f;
            return interpolateColor(new java.awt.Color(0, 255, 0), new java.awt.Color(255, 255, 0), 1.0f - ratio);
        } else if (healthPercent > 0.2f) {
            float ratio = (healthPercent - 0.2f) / 0.4f;
            return interpolateColor(new java.awt.Color(255, 255, 0), new java.awt.Color(255, 0, 0), 1.0f - ratio);
        } else {
            return new java.awt.Color(255, 0, 0).getRGB();
        }
    }

    private int interpolateColor(java.awt.Color color1, java.awt.Color color2, float ratio) {
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * ratio);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * ratio);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * ratio);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawElementOutlines(DrawContext context, String dragging) {
        if (hudModule == null)
            return;

        var mc = RadiumClient.mc;
        var clickGUI = RadiumClient.moduleManager.getModule(com.radium.client.modules.client.ClickGUI.class);
        // Always use Roboto font for HUD editor
        boolean useCustomFont = true;
        int textHeight = mc.textRenderer.fontHeight * 2;
        int boxPadding = 8;
        int barWidth = 4;
        int itemPadding = 6;

        com.radium.client.utils.render.RenderUtils.unscaledProjection();

        if (hudModule.showWatermark.getValue() && tempWatermarkX >= 0 && tempWatermarkY >= 0) {
            String watermarkText = "";
            HUD.WatermarkStyle style = (HUD.WatermarkStyle) hudModule.watermark.getValue();
            if (style == HUD.WatermarkStyle.LOWERCASE) {
                watermarkText = "radium";
            } else if (style == HUD.WatermarkStyle.UPPERCASE) {
                watermarkText = "RADIUM";
            } else {
                watermarkText = "Radium";
            }
            int width = mc.textRenderer.getWidth(watermarkText) * 2;
            int boxWidth = width + (boxPadding * 2) + barWidth;
            int boxHeight = textHeight + (boxPadding * 2);

            hudModule.drawEditOutline(context, tempWatermarkX, tempWatermarkY, boxWidth, boxHeight,
                    "watermark".equals(dragging));
        }

        if ((hudModule.showFPS.getValue() || hudModule.showPing.getValue() || hudModule.showServerIP.getValue())
                && tempInfoLinesX >= 0 && tempInfoLinesY >= 0) {
            java.util.List<String> infoLines = new java.util.ArrayList<>();
            if (hudModule.showFPS.getValue()) {
                infoLines.add(
                        hudModule.getFpsFormat().getValue().replace("{value}", String.valueOf(mc.getCurrentFps())));
            }
            if (hudModule.showPing.getValue()) {
                infoLines.add(hudModule.getPing());
            }
            if (hudModule.showServerIP.getValue()) {
                String serverText = "Singleplayer";
                if (mc.getCurrentServerEntry() != null) {
                    serverText = mc.getCurrentServerEntry().address;
                }
                infoLines.add(serverText);
            }

            int maxWidth = 0;
            for (String line : infoLines) {
                int w = mc.textRenderer.getWidth(line) * 2;
                maxWidth = Math.max(maxWidth, w);
            }
            int boxWidth = maxWidth + (boxPadding * 2) + barWidth;
            int boxHeight = (textHeight * infoLines.size()) + (itemPadding * (infoLines.size() - 1)) + (boxPadding * 2);
            hudModule.drawEditOutline(context, tempInfoLinesX, tempInfoLinesY, boxWidth, boxHeight,
                    "infoLines".equals(dragging));
        }

        if (hudModule.showKeybinds.getValue() && tempKeybindsX >= 0 && tempKeybindsY >= 0) {
            var modulesWithKeybinds = com.radium.client.client.RadiumClient.moduleManager.getModules().stream()
                    .filter(m -> m.getKeyBind() != -1 && !m.getName().equals("Radium"))
                    .sorted(java.util.Comparator.comparing(com.radium.client.modules.Module::getName))
                    .collect(java.util.stream.Collectors.toList());

            if (!modulesWithKeybinds.isEmpty()) {
                String labelText = hudModule.lowercaseKeybinds.getValue() ? "keybinds" : "Keybinds";
                int labelWidth = mc.textRenderer.getWidth(labelText) * 2;
                int maxWidth = labelWidth;
                for (var module : modulesWithKeybinds) {
                    String moduleName = hudModule.lowercase.getValue() ? module.getName().toLowerCase()
                            : module.getName();
                    if (hudModule.lowercaseKeybinds.getValue())
                        moduleName = moduleName.toLowerCase();
                    String keybindName = com.radium.client.client.KeybindManager.getKeyName(module.getKeyBind());
                    if (hudModule.lowercaseKeybinds.getValue())
                        keybindName = keybindName.toLowerCase();
                    int lineWidth = mc.textRenderer
                            .getWidth(moduleName + " - " + keybindName) * 2;
                    maxWidth = Math.max(maxWidth, lineWidth);
                }
                int boxWidth = maxWidth + (boxPadding * 2) + barWidth;
                int boxHeight = (textHeight * (modulesWithKeybinds.size() + 1))
                        + (itemPadding * modulesWithKeybinds.size()) + (boxPadding * 2) + 4;
                hudModule.drawEditOutline(context, tempKeybindsX, tempKeybindsY, boxWidth, boxHeight,
                        "keybinds".equals(dragging));
            }
        }

        if (hudModule.showCoordinates.getValue() && tempCoordinatesX >= 0 && tempCoordinatesY >= 0) {
            if (mc.player != null) {
                int x = mc.player.getBlockPos().getX();
                int y = mc.player.getBlockPos().getY();
                int z = mc.player.getBlockPos().getZ();
                String coordText = hudModule.getCoordsFormat().getValue().replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y))
                        .replace("{z}", String.valueOf(z));
                int width = mc.textRenderer.getWidth(coordText) * 2;
                int boxWidth = width + (boxPadding * 2) + barWidth;
                int boxHeight = textHeight + (boxPadding * 2);
                hudModule.drawEditOutline(context, tempCoordinatesX, tempCoordinatesY, boxWidth, boxHeight,
                        "coordinates".equals(dragging));
            }
        }

        if (hudModule.showTotems.getValue() && tempTotemsX >= 0 && tempTotemsY >= 0) {
            int totemCount = 0;
            if (mc.player != null) {
                for (var stack : mc.player.getInventory().main) {
                    if (stack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING)
                        totemCount += stack.getCount();
                }
                var offhand = mc.player.getOffHandStack();
                if (offhand.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING)
                    totemCount += offhand.getCount();
            }
            String totemText = hudModule.getTotemFormat().getValue().replace("{value}", String.valueOf(totemCount));
            int width = mc.textRenderer.getWidth(totemText) * 2;
            int boxWidth = width + (boxPadding * 2) + barWidth;
            int boxHeight = textHeight + (boxPadding * 2);
            hudModule.drawEditOutline(context, tempTotemsX, tempTotemsY, boxWidth, boxHeight,
                    "totems".equals(dragging));
        }

        if (hudModule.showTargetHUD.getValue() && tempTargetHUDX >= 0 && tempTargetHUDY >= 0) {
            int boxWidth = 250;
            int boxHeight = 80;
            hudModule.drawEditOutline(context, tempTargetHUDX, tempTargetHUDY, boxWidth, boxHeight,
                    "targetHUD".equals(dragging));
        }

        if (hudModule.showModuleList.getValue() && tempModuleListX >= 0 && tempModuleListY >= 0) {
            var enabledModules = com.radium.client.client.RadiumClient.moduleManager.getModules().stream()
                    .filter(com.radium.client.modules.Module::isEnabled)
                    .filter(m -> !(m instanceof com.radium.client.modules.client.ClickGUI
                            || m instanceof com.radium.client.modules.client.HUD))
                    .collect(java.util.stream.Collectors.toList());

            if (!enabledModules.isEmpty()) {
                int moduleItemPadding = 4;
                int itemHeight = textHeight + (moduleItemPadding * 2);

                int screenWidth = mc.getWindow().getWidth();
                BarPosition barPos;
                int threshold = (int) SIDE_SNAP_THRESHOLD;
                if (tempModuleListX <= threshold) {
                    barPos = BarPosition.LEFT;
                } else if (tempModuleListX >= screenWidth - threshold) {
                    barPos = BarPosition.RIGHT;
                } else {
                    barPos = BarPosition.MIDDLE;
                }

                boolean isRight = barPos == BarPosition.RIGHT;
                boolean isMiddle = barPos == BarPosition.MIDDLE;
                boolean isLeft = barPos == BarPosition.LEFT;

                int maxWidth = 0;
                for (var module : enabledModules) {
                    String moduleName = hudModule.lowercase.getValue() ? module.getName().toLowerCase()
                            : module.getName();
                    int nameWidth = mc.textRenderer.getWidth(moduleName) * 2;
                    maxWidth = Math.max(maxWidth, nameWidth);
                }

                int moduleListMinX = Integer.MAX_VALUE;
                int moduleListMaxX = 0;
                int moduleListMinY = tempModuleListY;
                int moduleListMaxY = tempModuleListY;

                int yOffset = tempModuleListY;
                for (var module : enabledModules) {
                    String moduleName = hudModule.lowercase.getValue() ? module.getName().toLowerCase()
                            : module.getName();
                    int nameWidth = mc.textRenderer.getWidth(moduleName) * 2;

                    int backgroundY1 = yOffset - moduleItemPadding;
                    int backgroundY2 = yOffset + textHeight + moduleItemPadding;

                    if (isRight) {
                        int boxWidth = nameWidth + boxPadding + 4 + barWidth;
                        int fixedBarX = screenWidth - barWidth;
                        int boxX = fixedBarX - (boxWidth - barWidth);
                        moduleListMinX = Math.min(moduleListMinX, boxX);
                        moduleListMaxX = Math.max(moduleListMaxX, fixedBarX + barWidth);
                    } else if (isMiddle) {
                        int baseX = tempModuleListX;
                        int boxWidth = nameWidth + boxPadding * 2;
                        moduleListMinX = Math.min(moduleListMinX, baseX);
                        moduleListMaxX = Math.max(moduleListMaxX, baseX + boxWidth);
                    } else {
                        int fixedBarX = 0;
                        int boxWidth = nameWidth + boxPadding + 4;
                        int boxX = fixedBarX + barWidth;
                        moduleListMinX = Math.min(moduleListMinX, fixedBarX);
                        moduleListMaxX = Math.max(moduleListMaxX, boxX + boxWidth);
                    }
                    moduleListMaxY = Math.max(moduleListMaxY, backgroundY2);
                    yOffset += itemHeight + 4;
                }

                int outlineX = moduleListMinX;
                int outlineWidth = moduleListMaxX - moduleListMinX;
                int outlineHeight = moduleListMaxY - moduleListMinY;

                hudModule.drawEditOutline(context, outlineX, moduleListMinY, outlineWidth, outlineHeight,
                        "moduleList".equals(dragging));
            }
        }


        if (hudModule.showRegionMap.getValue()) {
            int cellSize = hudModule.regionMapCellSize.getValue().intValue();
            int width = hudModule.getRegionMapComponent().getWidth(cellSize);
            int height = hudModule.getRegionMapComponent().getHeight(cellSize, hudModule.regionMapShowCoords.getValue(),
                    hudModule.regionMapShowLabels.getValue());
            hudModule.drawEditOutline(context, tempRegionMapX, tempRegionMapY, width, height,
                    "regionMap".equals(dragging));
        }

        if (mediaPlayerModule != null && mediaPlayerModule.isEnabled() && tempMediaPlayerX >= 0 && tempMediaPlayerY >= 0) {
            int mpWidth = 280;
            if (mediaPlayerModule.getSettings().stream().anyMatch(s -> s.getName().equals("Width"))) {
                mpWidth = ((com.radium.client.gui.settings.NumberSetting) mediaPlayerModule.getSettings().stream().filter(s -> s.getName().equals("Width")).findFirst().get()).getValue().intValue();
            }
            int artSize = 64;
            int padding = 12;
            int mpHeight = artSize + padding * 2 + 8;
            hudModule.drawEditOutline(context, tempMediaPlayerX, tempMediaPlayerY, mpWidth, mpHeight,
                    "mediaPlayer".equals(dragging));
        }

        com.radium.client.utils.render.RenderUtils.scaledProjection();
    }

    private double toUnscaledX(double scaledX) {
        return scaledX * this.client.getWindow().getScaleFactor();
    }

    private double toUnscaledY(double scaledY) {
        return scaledY * this.client.getWindow().getScaleFactor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hudModule != null) {

            double unscaledMouseX = toUnscaledX(mouseX);
            double unscaledMouseY = toUnscaledY(mouseY);

            draggingElement = hudModule.getElementAtPosition((int) unscaledMouseX, (int) unscaledMouseY,
                    tempWatermarkX, tempWatermarkY,
                    tempInfoLinesX, tempInfoLinesY,
                    tempKeybindsX, tempKeybindsY,
                    tempCoordinatesX, tempCoordinatesY,
                    tempTotemsX, tempTotemsY,
                    tempModuleListX, tempModuleListY,
                    tempTargetHUDX, tempTargetHUDY,
                    tempRegionMapX, tempRegionMapY);

            if (draggingElement == null && mediaPlayerModule != null && mediaPlayerModule.isEnabled()) {
                int mpWidth = 280;
                if (mediaPlayerModule.getSettings().stream().anyMatch(s -> s.getName().equals("Width"))) {
                    mpWidth = ((com.radium.client.gui.settings.NumberSetting) mediaPlayerModule.getSettings().stream().filter(s -> s.getName().equals("Width")).findFirst().get()).getValue().intValue();
                }
                int artSize = 64;
                int padding = 12;
                int mpHeight = artSize + padding * 2 + 8;

                if ((int) unscaledMouseX >= tempMediaPlayerX && (int) unscaledMouseX <= tempMediaPlayerX + mpWidth &&
                        (int) unscaledMouseY >= tempMediaPlayerY && (int) unscaledMouseY <= tempMediaPlayerY + mpHeight) {
                    draggingElement = "mediaPlayer";
                }
            }

            if (draggingElement != null) {
                dragOffsetX = (int) unscaledMouseX - getElementX(draggingElement);
                dragOffsetY = (int) unscaledMouseY - getElementY(draggingElement);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingElement != null && hudModule != null) {

            double unscaledMouseX = toUnscaledX(mouseX);
            double unscaledMouseY = toUnscaledY(mouseY);

            int newX = (int) unscaledMouseX - dragOffsetX;
            int newY = (int) unscaledMouseY - dragOffsetY;

            newX = snapToGrid(newX);
            newY = snapToGrid(newY);

            newX = snapToEdgesOrCenter(draggingElement, newX);

            int width = getElementWidth(draggingElement);
            int height = getElementHeight(draggingElement);
            int screenWidth = this.client.getWindow().getFramebufferWidth();
            int screenHeight = this.client.getWindow().getFramebufferHeight();

            newX = Math.max(0, Math.min(newX, screenWidth - width));
            newY = Math.max(0, Math.min(newY, screenHeight - height));

            setElementPosition(draggingElement, newX, newY);
            updatePositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingElement = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
//commit
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(null);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_G) {
            snapToGrid = !snapToGrid;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            gridSize = Math.min(50, gridSize + 5);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            gridSize = Math.max(5, gridSize - 5);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int getElementX(String element) {
        switch (element) {
            case "watermark":
                return tempWatermarkX;
            case "infoLines":
                return tempInfoLinesX;
            case "keybinds":
                return tempKeybindsX;
            case "coordinates":
                return tempCoordinatesX;
            case "totems":
                return tempTotemsX;
            case "moduleList":
                return tempModuleListX;
            case "targetHUD":
                return tempTargetHUDX;
            case "regionMap":
                return tempRegionMapX;
            case "mediaPlayer":
                return tempMediaPlayerX;
            default:
                return 0;
        }
    }

    private int getElementY(String element) {
        switch (element) {
            case "watermark":
                return tempWatermarkY;
            case "infoLines":
                return tempInfoLinesY;
            case "keybinds":
                return tempKeybindsY;
            case "coordinates":
                return tempCoordinatesY;
            case "totems":
                return tempTotemsY;
            case "moduleList":
                return tempModuleListY;
            case "targetHUD":
                return tempTargetHUDY;
            case "regionMap":
                return tempRegionMapY;
            case "mediaPlayer":
                return tempMediaPlayerY;
            default:
                return 0;
        }
    }

    private void setElementPosition(String element, int x, int y) {
        switch (element) {
            case "watermark":
                tempWatermarkX = x;
                tempWatermarkY = y;
                break;
            case "infoLines":
                tempInfoLinesX = x;
                tempInfoLinesY = y;
                break;
            case "keybinds":
                tempKeybindsX = x;
                tempKeybindsY = y;
                break;
            case "coordinates":
                tempCoordinatesX = x;
                tempCoordinatesY = y;
                break;
            case "totems":
                tempTotemsX = x;
                tempTotemsY = y;
                break;
            case "moduleList":
                tempModuleListX = x;
                tempModuleListY = y;
                break;
            case "targetHUD":
                tempTargetHUDX = x;
                tempTargetHUDY = y;
                break;
            case "regionMap":
                tempRegionMapX = x;
                tempRegionMapY = y;
                break;
            case "mediaPlayer":
                tempMediaPlayerX = x;
                tempMediaPlayerY = y;
                break;
        }
    }

    private enum BarPosition {
        LEFT, MIDDLE, RIGHT
    }
}

