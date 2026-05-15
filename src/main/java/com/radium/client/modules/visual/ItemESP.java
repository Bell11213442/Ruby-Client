package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.ItemEspRenderer;

public class ItemESP extends Module {
    public final NumberSetting range = new NumberSetting("Range", 128.0, 10.0, 512.0, 10.0);
    public final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.1, 5.0, 0.1);
    public final BooleanSetting showName = new BooleanSetting("Show Name", true);
    public final BooleanSetting showCount = new BooleanSetting("Show Count", true);

    public ItemESP() {
        super("ItemESP", "Shows dropped items with icons and labels", Category.VISUAL);
        addSettings(range, scale, showName, showCount);
        ItemEspRenderer.register();
    }
}

