package com.radium.client.modules.client;

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.CapeSelectionScreen;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.modules.Module;
import net.minecraft.util.Identifier;

public class Cape extends Module {

    private final BooleanSetting openMenu = new BooleanSetting("Open Menu", false);

    public Cape() {
        super("Cape", "Displays your custom cape.", Category.CLIENT);
        addSettings(openMenu);
    }

    @Override
    public void onEnable() {
        if (mc.player != null || mc.currentScreen instanceof com.radium.client.gui.ClickGuiScreen) {
            if (!(mc.currentScreen instanceof CapeSelectionScreen)) {
                mc.setScreen(new CapeSelectionScreen(mc.currentScreen));
            }
        }
    }

    @Override
    public void onTick() {
        if (openMenu.getValue()) {
            openMenu.setValue(false);
            mc.setScreen(new CapeSelectionScreen(mc.currentScreen));
        }
    }

    public boolean shouldShowCape() {
        return this.isEnabled();
    }

    public Identifier getCapeTexture() {
        if (RadiumClient.getCapeManager() != null) {
            return RadiumClient.getCapeManager().getSelectedCape();
        }
        return null;
    }
}
