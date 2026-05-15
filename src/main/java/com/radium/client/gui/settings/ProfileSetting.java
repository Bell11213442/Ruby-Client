package com.radium.client.gui.settings;
// radium client

import java.util.List;

public class ProfileSetting extends Setting<String> {
    private List<String> profiles;
    private boolean expanded = false;

    public ProfileSetting(String name, String defaultValue, List<String> profiles) {
        super(name, defaultValue);
        this.profiles = profiles;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
