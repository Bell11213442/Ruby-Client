package com.radium.client.modules.misc;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.client.Friends;

import java.util.List;

public class NameProtect extends Module {
    private final StringSetting fakeName = new StringSetting("Fake Name", "DrDonutt");
    private final BooleanSetting protectFriends = new BooleanSetting("Protect Friends", false);

    public NameProtect() {
        super(("NameProtect"), ("Replaces your name with given one."), Category.MISC);
        this.addSettings(this.fakeName, this.protectFriends);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public String getFakeName() {
        return this.fakeName.getValue();
    }

    public boolean shouldProtectFriends() {
        return this.protectFriends.getValue();
    }

    public String getFakeNameFor(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return playerName;
        }

        String username = RadiumClient.mc.getSession().getUsername();
        if (playerName.equals(username)) {
            return getFakeName();
        }

        if (!shouldProtectFriends()) {
            return playerName;
        }

        Friends friendsModule = RadiumClient.moduleManager.getModule(Friends.class);
        if (friendsModule != null && friendsModule.isFriend(playerName)) {
            List<String> friendsList = friendsModule.getFriendsList();
            if (friendsList != null) {
                int index = -1;
                for (int i = 0; i < friendsList.size(); i++) {
                    if (friendsList.get(i).trim().equalsIgnoreCase(playerName.trim())) {
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    return "Friend" + (index + 1);
                }
            }
        }

        return playerName;
    }
}


