package com.radium.client.gui.utils;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.Module;
import org.lwjgl.glfw.GLFW;

public class KeybindListener {
    private boolean listeningForKeybind = false;
    private Module keybindModule = null;

    public void startListening(Module module) {
        this.listeningForKeybind = true;
        this.keybindModule = module;
    }

    public void stopListening() {
        this.listeningForKeybind = false;
        this.keybindModule = null;
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!listeningForKeybind || keybindModule == null) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            keybindModule.setKeyBind(-1);
        } else {
            keybindModule.setKeyBind(keyCode);
        }

        stopListening();


        if (RadiumClient.getConfigManager() != null) {
            RadiumClient.getConfigManager().saveKeybinds();
        }

        return true;
    }

    public boolean isListening() {
        return listeningForKeybind;
    }

    public Module getListeningModule() {
        return keybindModule;
    }

    public boolean isListeningFor(Module module) {
        return listeningForKeybind && keybindModule == module;
    }
}


