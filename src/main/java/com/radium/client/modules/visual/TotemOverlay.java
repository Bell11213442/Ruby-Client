package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.ClickGuiScreen;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;

import java.awt.*;

public final class TotemOverlay extends Module {

    private final ColorSetting overlayColor = new ColorSetting("Overlay Color", new Color(255, 0, 0, 76));

    public TotemOverlay() {
        super("TotemOverlay",
                "Shows a semi-transparent screen overlay when you don't have a totem in your offhand",
                Category.VISUAL);
        addSettings(overlayColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void render(DrawContext dc) {
        if (!this.enabled) return;

        if (mc.player == null || mc.currentScreen instanceof ClickGuiScreen) return;


        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {


            RenderUtils.unscaledProjection();
            RenderUtils.fillRect(dc, 0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                    overlayColor.getValue().getRGB());
            RenderUtils.scaledProjection();
        }
    }
}

