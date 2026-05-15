package com.radium.client.gui.settings;
// radium client

import com.radium.client.client.KeybindManager;


public class KeybindSetting extends Setting<Integer> {
    private boolean listening = false;

    public KeybindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public String getDisplayText() {
        if (listening) {
            return "Press a key...";
        }
        return KeybindManager.getKeyName(getValue());
    }

    public void setKey(int keyCode) {
        setValue(keyCode);
        setListening(false);
    }

    public void clearKey() {
        setValue(-1);
        setListening(false);
    }
}
