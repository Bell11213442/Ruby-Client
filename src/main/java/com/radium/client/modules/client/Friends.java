package com.radium.client.modules.client;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.StringListSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Friends extends Module {
    public final BooleanSetting notifyOnJoin = new BooleanSetting("Notify On Join", true);
    public final BooleanSetting notifyOnLeave = new BooleanSetting("Notify On Leave", true);
    private final StringListSetting friendsList = new StringListSetting("Friends");
    private final Set<String> onlineFriends = new HashSet<>();
    private final Set<String> previousOnlineFriends = new HashSet<>();

    public Friends() {
        super("Friends", "Manage your friends list", Category.CLIENT);
        settings.add(friendsList);
        settings.add(notifyOnJoin);
        settings.add(notifyOnLeave);
    }

    public List<String> getFriendsList() {
        return friendsList.getList();
    }

    public boolean isFriend(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        List<String> friends = getFriendsList();
        if (friends == null || friends.isEmpty()) {
            return false;
        }

        String normalizedName = playerName.trim().toLowerCase();
        return friends.stream()
                .map(name -> name != null ? name.trim().toLowerCase() : "")
                .anyMatch(name -> name.equals(normalizedName));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (!notifyOnJoin.getValue() && !notifyOnLeave.getValue()) return;

        onlineFriends.clear();

        // Get current online friends
        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
        for (PlayerListEntry entry : playerList) {
            String playerName = entry.getProfile().getName();
            if (isFriend(playerName)) {
                onlineFriends.add(playerName);
            }
        }

        // Check for friends who joined
        if (notifyOnJoin.getValue() && !previousOnlineFriends.isEmpty()) {
            Set<String> joinedFriends = new HashSet<>(onlineFriends);
            joinedFriends.removeAll(previousOnlineFriends);

            for (String friendName : joinedFriends) {
                if (RadiumClient.getModuleManager() != null) {
                    com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                    if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                        com.radium.client.utils.ToastNotificationManager.getInstance().show(
                                "Friend Joined",
                                friendName + " joined the server",
                                com.radium.client.utils.ToastNotification.ToastType.FRIEND_JOIN
                        );
                    }
                }
            }
        }

        // Check for friends who left
        if (notifyOnLeave.getValue() && !previousOnlineFriends.isEmpty()) {
            Set<String> leftFriends = new HashSet<>(previousOnlineFriends);
            leftFriends.removeAll(onlineFriends);

            for (String friendName : leftFriends) {
                if (RadiumClient.getModuleManager() != null) {
                    com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                    if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                        com.radium.client.utils.ToastNotificationManager.getInstance().show(
                                "Friend Left",
                                friendName + " left the server",
                                com.radium.client.utils.ToastNotification.ToastType.FRIEND_LEAVE
                        );
                    }
                }
            }
        }

        previousOnlineFriends.clear();
        previousOnlineFriends.addAll(onlineFriends);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        onlineFriends.clear();
        previousOnlineFriends.clear();

        // Initialize previous online friends
        if (mc.getNetworkHandler() != null) {
            Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
            for (PlayerListEntry entry : playerList) {
                String playerName = entry.getProfile().getName();
                if (isFriend(playerName)) {
                    previousOnlineFriends.add(playerName);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        onlineFriends.clear();
        previousOnlineFriends.clear();
    }

    public void addFriend(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            String trimmedName = playerName.trim();
            friendsList.add(trimmedName);

            // Show toast notification for friend added
            if (RadiumClient.getModuleManager() != null) {
                com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            "Friend Added",
                            trimmedName + " added to friends",
                            com.radium.client.utils.ToastNotification.ToastType.FRIEND_JOIN
                    );
                }
            }
        }
    }

    public void removeFriend(String playerName) {
        if (playerName != null) {
            String trimmedName = playerName.trim();
            friendsList.remove(trimmedName);

            // Show toast notification for friend removed
            if (RadiumClient.getModuleManager() != null) {
                com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            "Friend Removed",
                            trimmedName + " removed from friends",
                            com.radium.client.utils.ToastNotification.ToastType.FRIEND_LEAVE
                    );
                }
            }
        }
    }
}


