package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;

public final class PlacementOptimizer extends Module implements TickListener {
    private final BooleanSetting excludeAnchors = new BooleanSetting("Exclude Anchors/Glowstone", true);
    private final NumberSetting blockDelay = new NumberSetting("Block delay", 3.0, 0.0, 5.0, 0.1);
    private final NumberSetting crystalDelay = new NumberSetting("Crystal delay", 0.0, 0.0, 2.0, 1.0);

    public PlacementOptimizer() {
        super("Placement Optimizer", "Adjusts block/crystal placement delays", Category.COMBAT);
        this.addSettings(this.excludeAnchors, this.blockDelay, this.crystalDelay);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick2() {

    }

    public boolean shouldExcludeAnchors() {
        return this.excludeAnchors.getValue();
    }

    public int getBlockDelay() {
        return this.blockDelay.getValue().intValue();
    }

    public int getCrystalDelay() {
        return this.crystalDelay.getValue().intValue();
    }
}

