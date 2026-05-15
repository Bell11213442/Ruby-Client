package com.radium.client.modules.visual;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.StorageEspRenderer;

import java.awt.*;

public class StorageESP extends Module {
    private final NumberSetting range = new NumberSetting("Range", 50.0, 10.0, 200.0, 10.0);
    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final BooleanSetting barrels = new BooleanSetting("Barrels", true);
    private final BooleanSetting dispensers = new BooleanSetting("Dispensers", false);
    private final BooleanSetting hoppers = new BooleanSetting("Hoppers", false);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);
    private final BooleanSetting furnaces = new BooleanSetting("Furnaces", true);


    private final ColorSetting chestColor = new ColorSetting("Chest Color", new Color(235, 198, 52, 77));
    private final ColorSetting enderChestColor = new ColorSetting("Ender Chest Color", new Color(255, 0, 255, 77));
    private final ColorSetting shulkerColor = new ColorSetting("Shulker Color", new Color(139, 92, 246, 77));
    private final ColorSetting barrelColor = new ColorSetting("Barrel Color", new Color(139, 69, 19, 77));
    private final ColorSetting dispenserColor = new ColorSetting("Dispenser Color", new Color(128, 128, 128, 77));
    private final ColorSetting hopperColor = new ColorSetting("Hopper Color", new Color(64, 64, 64, 77));
    private final ColorSetting spawnerColor = new ColorSetting("Spawner Color", new Color(255, 69, 0, 77));
    private final ColorSetting furnaceColor = new ColorSetting("Furnace Color", new Color(112, 128, 144, 77));

    private final BooleanSetting showLabels = new BooleanSetting("Show Labels", true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", true);
    private final BooleanSetting fill = new BooleanSetting("Fill", true);
    private final BooleanSetting outline = new BooleanSetting("Outline", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", false);
    private final NumberSetting outlineOpacity = new NumberSetting("Outline Opacity", 100, 0, 100, 5);

    public StorageESP() {
        super("StorageESP", "Highlights storage blocks", Category.VISUAL);
        settings.add(range);
        settings.add(chests);
        settings.add(enderChests);
        settings.add(shulkers);
        settings.add(barrels);
        settings.add(dispensers);
        settings.add(hoppers);
        settings.add(spawners);
        settings.add(furnaces);
        settings.add(chestColor);
        settings.add(enderChestColor);
        settings.add(shulkerColor);
        settings.add(barrelColor);
        settings.add(dispenserColor);
        settings.add(hopperColor);
        settings.add(spawnerColor);
        settings.add(furnaceColor);
        settings.add(showLabels);
        settings.add(showDistance);
        settings.add(fill);
        settings.add(outline);
        settings.add(tracers);
        settings.add(outlineOpacity);
        StorageEspRenderer.register();
    }

    public boolean shouldHighlight(String blockName) {
        if (!enabled) return false;

        String name = blockName.toLowerCase();

        if (chests.getValue() && (name.contains("chest") && !name.contains("ender"))) return true;
        if (enderChests.getValue() && name.contains("ender_chest")) return true;
        if (shulkers.getValue() && name.contains("shulker")) return true;
        if (barrels.getValue() && name.contains("barrel")) return true;
        if (dispensers.getValue() && (name.contains("dispenser") || name.contains("dropper"))) return true;
        if (hoppers.getValue() && name.contains("hopper")) return true;
        if (spawners.getValue() && name.contains("spawner")) return true;
        return furnaces.getValue() && name.contains("furnace");
    }

    public int getColor(String blockName) {
        String name = blockName.toLowerCase();

        if (name.contains("ender_chest")) return enderChestColor.getValue().getRGB() & 0xFFFFFF;
        if (name.contains("shulker")) return shulkerColor.getValue().getRGB() & 0xFFFFFF;
        if (name.contains("barrel")) return barrelColor.getValue().getRGB() & 0xFFFFFF;
        if (name.contains("dispenser") || name.contains("dropper"))
            return dispenserColor.getValue().getRGB() & 0xFFFFFF;
        if (name.contains("hopper")) return hopperColor.getValue().getRGB() & 0xFFFFFF;
        if (name.contains("spawner")) return spawnerColor.getValue().getRGB() & 0xFFFFFF;
        if (name.contains("furnace")) return furnaceColor.getValue().getRGB() & 0xFFFFFF;

        return chestColor.getValue().getRGB() & 0xFFFFFF;
    }

    public double getRenderRange() {
        return range.getValue();
    }

    public boolean shouldShowLabels() {
        return showLabels.getValue();
    }

    public boolean shouldShowDistance() {
        return showDistance.getValue();
    }

    public boolean shouldFill() {
        return fill.getValue();
    }

    public boolean shouldOutline() {
        return outline.getValue();
    }

    public boolean shouldTracers() {
        return tracers.getValue();
    }

    public int getFillColor(String blockName) {
        String name = blockName.toLowerCase();
        Color color;

        if (name.contains("ender_chest")) color = enderChestColor.getValue();
        else if (name.contains("shulker")) color = shulkerColor.getValue();
        else if (name.contains("barrel")) color = barrelColor.getValue();
        else if (name.contains("dispenser") || name.contains("dropper")) color = dispenserColor.getValue();
        else if (name.contains("hopper")) color = hopperColor.getValue();
        else if (name.contains("spawner")) color = spawnerColor.getValue();
        else if (name.contains("furnace")) color = furnaceColor.getValue();
        else color = chestColor.getValue();

        return color.getRGB();
    }

    public int getOutlineColor(String blockName) {
        int baseColor = getColor(blockName);
        int alpha = (int) (outlineOpacity.getValue() * 2.55);
        return (alpha << 24) | baseColor;
    }

    @Override
    public void onEnable() {

        RadiumClient.sendKeepAliveIfAllowed();
    }
}
