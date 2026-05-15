package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.HoleTunnelStairsRenderer;

import java.awt.*;

public class HoleTunnelStairsESP extends Module {

    private final ModeSetting<DetectionMode> detectionMode = new ModeSetting<>("Detection Mode", DetectionMode.ALL, DetectionMode.class);
    private final NumberSetting maxChunks = new NumberSetting("Chunks/Tick", 10.0, 1.0, 100.0, 1.0);
    private final BooleanSetting airBlocks = new BooleanSetting("Only Air Blocks", false);
    private final NumberSetting minY = new NumberSetting("Min Y Offset", 0.0, 0.0, 319.0, 1.0);
    private final NumberSetting maxY = new NumberSetting("Max Y Offset", 0.0, 0.0, 319.0, 1.0);
    private final NumberSetting minHoleDepth = new NumberSetting("Min Hole Depth", 4.0, 1.0, 20.0, 1.0);
    private final NumberSetting minTunnelLength = new NumberSetting("Min Tunnel Length", 3.0, 1.0, 20.0, 1.0);
    private final NumberSetting minTunnelHeight = new NumberSetting("Min Tunnel Height", 2.0, 1.0, 10.0, 1.0);
    private final NumberSetting maxTunnelHeight = new NumberSetting("Max Tunnel Height", 3.0, 2.0, 10.0, 1.0);
    private final BooleanSetting diagonals = new BooleanSetting("Detect Diagonals", true);
    private final NumberSetting minDiagonalLength = new NumberSetting("Min Diagonal Length", 3.0, 1.0, 20.0, 1.0);
    private final NumberSetting minDiagonalWidth = new NumberSetting("Min Diagonal Width", 2.0, 2.0, 10.0, 1.0);
    private final NumberSetting maxDiagonalWidth = new NumberSetting("Max Diagonal Width", 4.0, 2.0, 10.0, 1.0);
    private final NumberSetting minStaircaseLength = new NumberSetting("Min Staircase Length", 3.0, 1.0, 20.0, 1.0);
    private final NumberSetting minStaircaseHeight = new NumberSetting("Min Staircase Height", 3.0, 2.0, 10.0, 1.0);
    private final NumberSetting maxStaircaseHeight = new NumberSetting("Max Staircase Height", 5.0, 2.0, 10.0, 1.0);
    private final BooleanSetting fill = new BooleanSetting("Fill", true);
    private final BooleanSetting outline = new BooleanSetting("Outline", true);
    private final ColorSetting holeLineColor = new ColorSetting("1x1 Hole Line", new Color(255, 0, 0, 95));
    private final ColorSetting holeSideColor = new ColorSetting("1x1 Hole Side", new Color(255, 0, 0, 30));
    private final ColorSetting hole3x1LineColor = new ColorSetting("3x1 Hole Line", new Color(255, 165, 0, 95));
    private final ColorSetting hole3x1SideColor = new ColorSetting("3x1 Hole Side", new Color(255, 165, 0, 30));
    private final ColorSetting tunnelLineColor = new ColorSetting("Tunnel Line", new Color(0, 0, 255, 95));
    private final ColorSetting tunnelSideColor = new ColorSetting("Tunnel Side", new Color(0, 0, 255, 30));
    private final ColorSetting staircaseLineColor = new ColorSetting("Staircase Line", new Color(255, 0, 255, 95));
    private final ColorSetting staircaseSideColor = new ColorSetting("Staircase Side", new Color(255, 0, 255, 30));

    public HoleTunnelStairsESP() {
        super("HoleESP", "Detects and highlights holes, tunnels, and staircases", Category.VISUAL);


        settings.add(detectionMode);
        settings.add(maxChunks);
        settings.add(airBlocks);
        settings.add(minY);
        settings.add(maxY);


        settings.add(minHoleDepth);


        settings.add(minTunnelLength);
        settings.add(minTunnelHeight);
        settings.add(maxTunnelHeight);
        settings.add(diagonals);
        settings.add(minDiagonalLength);
        settings.add(minDiagonalWidth);
        settings.add(maxDiagonalWidth);


        settings.add(minStaircaseLength);
        settings.add(minStaircaseHeight);
        settings.add(maxStaircaseHeight);


        settings.add(fill);
        settings.add(outline);
        settings.add(holeLineColor);
        settings.add(holeSideColor);
        settings.add(hole3x1LineColor);
        settings.add(hole3x1SideColor);
        settings.add(tunnelLineColor);
        settings.add(tunnelSideColor);
        settings.add(staircaseLineColor);
        settings.add(staircaseSideColor);

        HoleTunnelStairsRenderer.register();
    }

    public DetectionMode getDetectionMode() {
        return detectionMode.getValue();
    }

    public int getMaxChunks() {
        return maxChunks.getValue().intValue();
    }

    public boolean isAirBlocksOnly() {
        return airBlocks.getValue();
    }

    public int getMinY() {
        return minY.getValue().intValue();
    }

    public int getMaxY() {
        return maxY.getValue().intValue();
    }

    public int getMinHoleDepth() {
        return minHoleDepth.getValue().intValue();
    }

    public int getMinTunnelLength() {
        return minTunnelLength.getValue().intValue();
    }

    public int getMinTunnelHeight() {
        return minTunnelHeight.getValue().intValue();
    }

    public int getMaxTunnelHeight() {
        return maxTunnelHeight.getValue().intValue();
    }

    public boolean shouldDetectDiagonals() {
        return diagonals.getValue();
    }

    public int getMinDiagonalLength() {
        return minDiagonalLength.getValue().intValue();
    }

    public int getMinDiagonalWidth() {
        return minDiagonalWidth.getValue().intValue();
    }

    public int getMaxDiagonalWidth() {
        return maxDiagonalWidth.getValue().intValue();
    }

    public int getMinStaircaseLength() {
        return minStaircaseLength.getValue().intValue();
    }

    public int getMinStaircaseHeight() {
        return minStaircaseHeight.getValue().intValue();
    }

    public int getMaxStaircaseHeight() {
        return maxStaircaseHeight.getValue().intValue();
    }

    public boolean shouldFill() {
        return fill.getValue();
    }

    public boolean shouldOutline() {
        return outline.getValue();
    }

    public int getHoleLineColor() {
        return holeLineColor.getValue().getRGB();
    }

    public int getHoleSideColor() {
        return holeSideColor.getValue().getRGB();
    }

    public int getHole3x1LineColor() {
        return hole3x1LineColor.getValue().getRGB();
    }

    public int getHole3x1SideColor() {
        return hole3x1SideColor.getValue().getRGB();
    }

    public int getTunnelLineColor() {
        return tunnelLineColor.getValue().getRGB();
    }

    public int getTunnelSideColor() {
        return tunnelSideColor.getValue().getRGB();
    }

    public int getStaircaseLineColor() {
        return staircaseLineColor.getValue().getRGB();
    }

    public int getStaircaseSideColor() {
        return staircaseSideColor.getValue().getRGB();
    }

    public enum DetectionMode {
        ALL,
        HOLES_AND_TUNNELS,
        HOLES_AND_STAIRCASES,
        TUNNELS_AND_STAIRCASES,
        HOLES,
        TUNNELS,
        STAIRCASES,
        HOLES_3X1_AND_TUNNELS
    }
}
