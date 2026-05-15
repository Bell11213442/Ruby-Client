package com.radium.client.modules;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    public static MinecraftClient mc = RadiumClient.mc;
    public boolean enabled = false;
    protected String name;
    protected String description;
    protected Category category;
    protected List<Setting<?>> settings = new ArrayList<>();
    protected int keyBind = -1;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }


    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void toggle() {
        enabled = !enabled;

        if (enabled) {
            onEnable();
            // Show toast notification for module enabled
            if (RadiumClient.getModuleManager() != null && !(this instanceof com.radium.client.modules.client.ClickGUI)) {
                com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            getName(),
                            "Enabled",
                            com.radium.client.utils.ToastNotification.ToastType.MODULE_ENABLED
                    );
                }
            }
        } else {
            onDisable();
            // Show toast notification for module disabled
            if (RadiumClient.getModuleManager() != null && !(this instanceof com.radium.client.modules.client.ClickGUI)) {
                com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            getName(),
                            "Disabled",
                            com.radium.client.utils.ToastNotification.ToastType.MODULE_DISABLED
                    );
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        int oldKeyBind = this.keyBind;
        this.keyBind = keyBind;

        // Show toast notification for keybind change (only if actually changed and not during initialization)
        // Only show if both old and new keybinds are valid (not -1) to avoid toasts during initial config load
        if (oldKeyBind != keyBind && oldKeyBind > 0 && keyBind > 0 && RadiumClient.getModuleManager() != null && RadiumClient.getModuleManager().getModules().size() > 1 && !(this instanceof com.radium.client.modules.client.ClickGUI)) {
            com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
            if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                String keyName = com.radium.client.client.KeybindManager.getKeyName(keyBind);
                com.radium.client.utils.ToastNotificationManager.getInstance().show(
                        getName() + " Keybind",
                        "Set to " + keyName,
                        com.radium.client.utils.ToastNotification.ToastType.KEYBIND_CHANGE
                );
            }
        }
    }

    public void addSettings(Setting<?>... settingsToAdd) {
        Collections.addAll(settings, settingsToAdd);
    }

    public enum Category {
        COMBAT("Combat"),
        VISUAL("Visual"),
        MISC("Misc"),
        DONUT("Donut"),
        CLIENT("Client"),
        SEARCH("Search");


        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}

