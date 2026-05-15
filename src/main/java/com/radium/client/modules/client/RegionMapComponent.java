package com.radium.client.modules.client;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RegionMapComponent {
    private final MinecraftClient mc = RadiumClient.mc;
    private final MapDataManager mapData;
    private Color currentBgColor = new Color(0, 0, 0);

    public RegionMapComponent() {
        this.mapData = new MapDataManager();
    }

    public void render(DrawContext context, int x, int y, int cellSize, double transparency,
                       boolean enableGrid, boolean enableLabels, boolean enableCoordinates,
                       boolean enablePlayerIndicator,
                       Color backgroundColor, Color gridColor, Color playerColor) {

        this.currentBgColor = backgroundColor;

        if (mc.player == null || mc.world == null) return;

        int mapSize = mapData.getMapSize();
        int mapWidth = mapSize * cellSize;
        int mapHeight = mapSize * cellSize;
        int cornerRadius = 6;
        int padding = 2;


        HUD hud = RadiumClient.moduleManager.getModule(HUD.class);
        int hudColor = hud != null ? hud.getHudColorInt() : 0xFFFF0000;
        int borderAlpha = (int) (transparency * 0.6f * 255);
        int borderColor = (borderAlpha << 24) | (hudColor & 0xFFFFFF);

        int bgAlpha = (int) (transparency * 255);
        int bgRgb = (bgAlpha << 24) | (backgroundColor.getRGB() & 0xFFFFFF);


        RenderUtils.fillRoundRect(context, x, y, mapWidth, mapHeight, cornerRadius, bgRgb);


        RenderUtils.drawRoundRect(context, x, y, mapWidth, mapHeight, cornerRadius, borderColor);


        for (int row = 0; row < mapSize; row++) {
            for (int col = 0; col < mapSize; col++) {
                int index = row * mapSize + col;
                RegionInfo regionInfo = mapData.getRegionInfo(index);

                if (regionInfo != null) {
                    int cellX = x + col * cellSize + padding;
                    int cellY = y + row * cellSize + padding;
                    int cellW = cellSize - padding * 2;
                    int cellH = cellSize - padding * 2;

                    Color regionColor = mapData.getRegionColor(regionInfo.regionType);
                    int regionRgb = (bgAlpha << 24) | (regionColor.getRGB() & 0xFFFFFF);


                    RenderUtils.fillRoundRect(context, cellX, cellY, cellW, cellH, 2, regionRgb);
                }
            }
        }


        if (enableGrid) {
            int gridAlpha = (int) (transparency * 0.3f * 255);
            int gridRgb = (gridAlpha << 24) | (gridColor.getRGB() & 0xFFFFFF);

            for (int i = 0; i <= mapSize; i++) {
                int lineX = x + i * cellSize;
                RenderUtils.fillRect(context, lineX, y, 1, mapHeight, gridRgb);
            }

            for (int i = 0; i <= mapSize; i++) {
                int lineY = y + i * cellSize;
                RenderUtils.fillRect(context, x, lineY, mapWidth, 1, gridRgb);
            }
        }

        float textScale = 1.0f;

        context.getMatrices().push();

        if (enableLabels) {
            for (int row = 0; row < mapSize; row++) {
                for (int col = 0; col < mapSize; col++) {
                    int index = row * mapSize + col;
                    RegionInfo regionInfo = mapData.getRegionInfo(index);

                    if (regionInfo != null) {
                        int cellX = x + col * cellSize;
                        int cellY = y + row * cellSize;

                        String numberText = String.valueOf(regionInfo.regionId);

                        float textWidth;
                        float textHeight;

                        textWidth = mc.textRenderer.getWidth(numberText);
                        textHeight = mc.textRenderer.fontHeight;

                        float centeredX = cellX + (cellSize - textWidth) / 2.0f;
                        float centeredY = cellY + (cellSize - textHeight) / 2.0f;

                        context.drawTextWithShadow(mc.textRenderer, numberText, (int) centeredX, (int) centeredY, Color.WHITE.getRGB());
                    }
                }
            }
        }
        context.getMatrices().pop();

        if (enablePlayerIndicator) {
            renderPlayerPosition(context, x, y, cellSize, playerColor);
        }

        if (enableCoordinates || enableLabels) {
            renderPlayerInfoAndLegend(context, x, y, mapHeight, enableCoordinates, enableLabels);
        }
    }

    private void renderPlayerPosition(DrawContext context, int mapX, int mapY, int cellSize, Color color) {
        Vec3d playerPos = mc.player.getPos();
        int[] gridPos = mapData.worldToGrid(playerPos.x, playerPos.z);

        if (gridPos[0] >= 0 && gridPos[0] < mapData.getMapSize() &&
                gridPos[1] >= 0 && gridPos[1] < mapData.getMapSize()) {

            double[] cellPos = mapData.worldToCellPosition(playerPos.x, playerPos.z);

            double indicatorX = mapX + gridPos[0] * cellSize + cellPos[0] * cellSize;
            double indicatorY = mapY + gridPos[1] * cellSize + cellPos[1] * cellSize;


            int dotSize = 4;
            int indicatorColor = color.getRGB();
            int outlineColor = 0xFFFFFFFF;


            RenderUtils.fillRadialGradient(context, (int) indicatorX, (int) indicatorY, dotSize + 2,
                    (0x40 << 24) | (indicatorColor & 0xFFFFFF),
                    (0x00 << 24) | (indicatorColor & 0xFFFFFF));


            RenderUtils.fillRoundRect(context, (int) indicatorX - dotSize, (int) indicatorY - dotSize,
                    dotSize * 2, dotSize * 2, dotSize, indicatorColor);


            RenderUtils.drawRoundRect(context, (int) indicatorX - dotSize, (int) indicatorY - dotSize,
                    dotSize * 2, dotSize * 2, dotSize, outlineColor);


            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(-yaw + 90);
            double dirX = Math.cos(rad) * 7;
            double dirY = Math.sin(rad) * 7;

            RenderUtils.fillRoundRect(context, (int) (indicatorX + dirX) - 2, (int) (indicatorY + dirY) - 2,
                    4, 4, 2, outlineColor);
        }
    }

    private void renderPlayerInfoAndLegend(DrawContext context, int mapX, int mapY, int mapHeight, boolean enableCoordinates, boolean enableLabels) {
        int infoY = mapY + mapHeight + 8;


        HUD hud = RadiumClient.moduleManager.getModule(HUD.class);
        int hudColor = hud != null ? hud.getHudColorInt() : 0xFFFF0000;
        int bgAlpha = 0xC0;
        int bgColor = (bgAlpha << 24) | (currentBgColor.getRed() << 16) | (currentBgColor.getGreen() << 8) | currentBgColor.getBlue();
        int borderColor = (0xFF << 24) | (hudColor & 0xFFFFFF);

        int textHeight = mc.textRenderer.fontHeight;
        int boxPaddingHorizontal = 40;
        int boxPaddingVertical = 12;
        int textLeftMargin = 12;
        int lineSpacing = 6;
        int sectionSpacing = 12;
        int cornerRadius = 6;


        int maxWidth = 0;
        String coordsText = null;
        String regionInfo = null;

        if (enableCoordinates) {
            Vec3d pos = mc.player.getPos();
            coordsText = String.format("Pos: %d, %d", (int) pos.x, (int) pos.z);

            int coordsTextWidth = mc.textRenderer.getWidth(coordsText);
            maxWidth = Math.max(maxWidth, coordsTextWidth);

            int currentRegionId = mapData.getRegionAt(pos.x, pos.z);
            if (currentRegionId != -1) {
                regionInfo = String.format("Region: %d (%s)", currentRegionId, mapData.getRegionTypeName(pos.x, pos.z));
                int regionTextWidth = mc.textRenderer.getWidth(regionInfo);
                maxWidth = Math.max(maxWidth, regionTextWidth);
            }
        }


        String[] regionTypes = enableLabels ? mapData.getRegionTypeNames() : new String[0];
        Color[] regionTypeColors = enableLabels ? mapData.getRegionTypeColors() : new Color[0];
        int lineHeight = 20;
        int itemSpacing = 4;
        int colorBoxSize = 20;
        int colorTextSpacing = 16;

        int maxLegendTextWidth = 0;
        for (String regionType : regionTypes) {

            int width = mc.textRenderer.getWidth(regionType);
            maxLegendTextWidth = Math.max(maxLegendTextWidth, width);
        }

        int legendContentWidth = maxLegendTextWidth + colorBoxSize + colorTextSpacing;
        maxWidth = Math.max(maxWidth, legendContentWidth);


        int boxWidth = maxWidth + boxPaddingHorizontal * 2 + 40;
        int infoSectionHeight = 0;
        if (enableCoordinates) {
            infoSectionHeight = textHeight * (regionInfo != null ? 2 : 1) + lineSpacing * (regionInfo != null ? 1 : 0);
        }
        int legendSectionHeight = enableLabels ? regionTypes.length * lineHeight + itemSpacing * (regionTypes.length - 1) : 0;
        int totalHeight = infoSectionHeight + (enableCoordinates && enableLabels ? sectionSpacing : 0) + legendSectionHeight + boxPaddingVertical * 2;


        RenderUtils.fillRoundRect(context, mapX, infoY, boxWidth, totalHeight, cornerRadius, bgColor);
        RenderUtils.drawRoundRect(context, mapX, infoY, boxWidth, totalHeight, cornerRadius, borderColor);

        int currentY = infoY + boxPaddingVertical;


        if (enableCoordinates && coordsText != null) {
            int textColor = hudColor;
            int textY = currentY;


            context.getMatrices().push();
            context.getMatrices().scale(2.0f, 2.0f, 1.0f);
            context.drawTextWithShadow(mc.textRenderer, coordsText, (mapX + textLeftMargin) / 2, textY / 2, textColor);
            context.getMatrices().pop();

            currentY += textHeight;


            if (regionInfo != null) {
                int separatorY = currentY + lineSpacing / 2;
                int separatorAlpha = 0x40000000;
                int separatorColor = (separatorAlpha << 24) | (hudColor & 0xFFFFFF);
                RenderUtils.fillRect(context, mapX, separatorY, boxWidth, 1, separatorColor);
                currentY += lineSpacing;
            }


            if (regionInfo != null) {
                textY = currentY;
                context.getMatrices().push();
                context.getMatrices().scale(2.0f, 2.0f, 1.0f);
                context.drawTextWithShadow(mc.textRenderer, regionInfo, (mapX + textLeftMargin) / 2, textY / 2, textColor);
                context.getMatrices().pop();
                currentY += textHeight;
            }


            if (enableLabels) {
                currentY += sectionSpacing;
            }
        }


        if (enableLabels) {
            int legendTextColor = 0xFFFFFFFF;
            int contentY = currentY;

            for (int i = 0; i < regionTypes.length && i < regionTypeColors.length; i++) {
                int legendY = contentY + i * (lineHeight + itemSpacing);
                int colorBoxX = mapX + textLeftMargin;
                int colorBoxY = legendY + (lineHeight - colorBoxSize) / 2;


                int colorBoxColor = regionTypeColors[i].getRGB();
                RenderUtils.fillRoundRect(context, colorBoxX, colorBoxY, colorBoxSize, colorBoxSize, 3, colorBoxColor);
                int colorBorderAlpha = 0x60000000;
                int colorBorder = (colorBorderAlpha << 24) | 0xFFFFFF;
                RenderUtils.drawRoundRect(context, colorBoxX, colorBoxY, colorBoxSize, colorBoxSize, 3, colorBorder);


                int textX = colorBoxX + colorBoxSize + colorTextSpacing;
                int textY = legendY + (lineHeight - textHeight) / 2;
                context.getMatrices().push();
                context.getMatrices().scale(2.0f, 2.0f, 1.0f);
                context.drawTextWithShadow(mc.textRenderer, regionTypes[i], textX / 2, textY / 2, legendTextColor);
                context.getMatrices().pop();
            }
        }
    }

    public int getWidth(int cellSize) {
        return mapData.getMapSize() * cellSize;
    }

    public int getHeight(int cellSize, boolean showCoords, boolean showLegend) {
        int h = mapData.getMapSize() * cellSize;
        if (showCoords || showLegend) {
            int textHeight = 9;
            int boxPaddingVertical = 12;
            int lineSpacing = 6;
            int sectionSpacing = 12;
            int lineHeight = 20;
            int itemSpacing = 4;

            int infoSectionHeight = showCoords ? textHeight * 2 + lineSpacing : 0;
            int legendSectionHeight = showLegend ? mapData.getRegionTypeNames().length * lineHeight + itemSpacing * (mapData.getRegionTypeNames().length - 1) : 0;
            int spacing = (showCoords && showLegend) ? sectionSpacing : 0;

            h += infoSectionHeight + spacing + legendSectionHeight + boxPaddingVertical * 2 + 8;
        }
        return h;
    }

    private static class MapDataManager {
        private static final int MAP_SIZE = 9;
        private static final double REGION_SIZE = 50000.0;
        private static final double MAP_OFFSET = 225000.0;

        private final Map<Integer, RegionInfo> regionMap;
        private final String[] regionTypeNames;
        private final Color[] regionTypeColors;

        public MapDataManager() {
            this.regionMap = new HashMap<>();
            this.regionTypeNames = new String[]{
                    "EU Central", "EU West", "NA East", "NA West", "Asia", "Oceania"
            };
            this.regionTypeColors = new Color[]{
                    new Color(159, 206, 99, 255),
                    new Color(0, 166, 99, 255),
                    new Color(79, 173, 234, 255),
                    new Color(47, 110, 186, 255),
                    new Color(245, 194, 66, 255),
                    new Color(252, 136, 3, 255)
            };

            initializeRegionData();
        }

        private void initializeRegionData() {
            int[][] regionLayout = {
                    {82, 5}, {100, 3}, {101, 3}, {102, 3}, {103, 2}, {104, 2}, {105, 2}, {106, 2}, {91, 2},
                    {83, 5}, {44, 3}, {75, 3}, {42, 3}, {41, 2}, {40, 2}, {39, 2}, {38, 2}, {92, 2},
                    {84, 5}, {45, 3}, {14, 3}, {13, 3}, {12, 2}, {11, 2}, {10, 2}, {37, 2}, {93, 2},
                    {85, 5}, {46, 5}, {74, 5}, {3, 3}, {2, 2}, {1, 2}, {25, 2}, {36, 2}, {94, 2},
                    {86, 4}, {47, 4}, {72, 4}, {71, 4}, {5, 2}, {4, 2}, {24, 2}, {35, 2}, {95, 2},
                    {87, 4}, {51, 1}, {17, 1}, {9, 0}, {8, 0}, {7, 0}, {23, 0}, {34, 0}, {96, 2},
                    {88, 4}, {54, 1}, {18, 1}, {61, 0}, {62, 0}, {21, 0}, {22, 0}, {33, 0}, {97, 0},
                    {89, 0}, {26, 1}, {27, 0}, {28, 0}, {29, 0}, {30, 0}, {59, 0}, {32, 0}, {98, 0},
                    {90, 0}, {107, 1}, {108, 1}, {109, 1}, {110, 1}, {111, 1}, {112, 1}, {113, 1}, {99, 0}
            };

            for (int i = 0; i < regionLayout.length; i++) {
                int row = i / MAP_SIZE;
                int col = i % MAP_SIZE;

                if (regionLayout[i].length >= 2) {
                    int regionId = regionLayout[i][0];
                    int regionType = Math.min(regionLayout[i][1], regionTypeNames.length - 1);

                    regionMap.put(i, new RegionInfo(regionId, regionType, row, col));
                }
            }
        }

        public RegionInfo getRegionInfo(int gridIndex) {
            return regionMap.get(gridIndex);
        }

        public int getRegionAt(double worldX, double worldZ) {
            try {
                int[] gridPos = worldToGrid(worldX, worldZ);
                if (isValidGridPosition(gridPos[0], gridPos[1])) {
                    int index = gridPos[1] * MAP_SIZE + gridPos[0];
                    RegionInfo info = regionMap.get(index);
                    return info != null ? info.regionId : -1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
        }

        public String getRegionTypeName(double worldX, double worldZ) {
            try {
                int[] gridPos = worldToGrid(worldX, worldZ);
                if (isValidGridPosition(gridPos[0], gridPos[1])) {
                    int index = gridPos[1] * MAP_SIZE + gridPos[0];
                    RegionInfo info = regionMap.get(index);
                    if (info != null && info.regionType >= 0 && info.regionType < regionTypeNames.length) {
                        return regionTypeNames[info.regionType];
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Unknown";
        }

        public Color getRegionColor(int regionType) {
            if (regionType >= 0 && regionType < regionTypeColors.length) {
                return regionTypeColors[regionType];
            }
            return Color.WHITE;
        }

        public String[] getRegionTypeNames() {
            return regionTypeNames.clone();
        }

        public Color[] getRegionTypeColors() {
            return regionTypeColors.clone();
        }

        public int[] worldToGrid(double worldX, double worldZ) {
            if (REGION_SIZE == 0) return new int[]{0, 0};

            int gridX = (int) ((worldX + MAP_OFFSET) / REGION_SIZE);
            int gridZ = (int) ((worldZ + MAP_OFFSET) / REGION_SIZE);
            return new int[]{gridX, gridZ};
        }

        public double[] worldToCellPosition(double worldX, double worldZ) {
            if (REGION_SIZE == 0) return new double[]{0.0, 0.0};

            double cellX = ((worldX + MAP_OFFSET) % REGION_SIZE) / REGION_SIZE;
            double cellZ = ((worldZ + MAP_OFFSET) % REGION_SIZE) / REGION_SIZE;

            cellX = Math.max(0.0, Math.min(1.0, cellX));
            cellZ = Math.max(0.0, Math.min(1.0, cellZ));

            return new double[]{cellX, cellZ};
        }

        private boolean isValidGridPosition(int gridX, int gridZ) {
            return gridX >= 0 && gridX < MAP_SIZE && gridZ >= 0 && gridZ < MAP_SIZE;
        }

        public int getMapSize() {
            return MAP_SIZE;
        }
    }

    private record RegionInfo(int regionId, int regionType, int gridRow, int gridCol) {
    }
}
