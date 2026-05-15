package com.radium.client.modules.client;


import com.radium.client.client.RadiumClient;
import com.radium.client.gui.ClickGuiScreen;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ClickGUI extends Module {
    public final BooleanSetting icons = new BooleanSetting("Icons", false);
    public final BooleanSetting toastNotifications = new BooleanSetting("Notifier", true);
    public final NumberSetting guiOpacity = new NumberSetting("GUI Opacity", 35.0, 0.0, 100.0, 5.0);
    private final ColorSetting primaryColor = new ColorSetting("Primary Color", new Color(255, 68, 68));
    private final ColorSetting secondaryColor = new ColorSetting("Secondary Color", new Color(204, 34, 34));
    public Screen currentGuiScreen;
    private ClickGuiScreen defaultGuiScreen;

    public ClickGUI() {
        super("RubyClient", "The Gui", Category.CLIENT);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        addSettings(primaryColor, secondaryColor, icons, toastNotifications, guiOpacity);
        RadiumClient.sendKeepAliveIfAllowed();
    }

    public int getPrimaryColor() {
        Color color = primaryColor.getValue();
        return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    public int getSecondaryColor() {
        Color color = secondaryColor.getValue();
        return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    public int getHudColor(double offset) {
        return getPrimaryColor();
    }

    public boolean isRounded() {
        return true;
    }

    @Override
    public void onEnable() {
        RadiumClient.sendKeepAliveIfAllowed();

        if (!canOpenGUI()) {
            return;
        }

        updateGuiScreen();
        if (!(mc.currentScreen instanceof ClickGuiScreen)) {
            mc.execute(() -> mc.setScreen(currentGuiScreen));
        }
    }

    @Override
    public void onDisable() {
        mc.execute(() -> {
            Screen screen = mc.currentScreen;
            if (screen != null) {
                screen.close();
            }
        });
    }


    public void updateGuiScreen() {
        if (defaultGuiScreen == null) {
            defaultGuiScreen = new ClickGuiScreen();
        }
        currentGuiScreen = defaultGuiScreen;
    }

    public boolean canOpenGUI() {
        Screen currentScreen = mc.currentScreen;
        if (currentScreen == null) {
            return true;
        }
        if (currentScreen instanceof TitleScreen) {
            return true;
        }
        return currentScreen instanceof ClickGuiScreen;
    }

    public void openGUI() {
        if (!canOpenGUI()) {
            return;
        }

        updateGuiScreen();
        if (currentGuiScreen != null) {
            mc.execute(() -> mc.setScreen(currentGuiScreen));
        }
    }
}

