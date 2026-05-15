package com.radium.client.utils;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.Friends;

import java.util.List;

public class FriendsManager {

    public static boolean isFriend(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        Friends friendsModule = RadiumClient.moduleManager.getModule(Friends.class);
        if (friendsModule == null) {
            return false;
        }

        List<String> friendsList = friendsModule.getFriendsList();
        if (friendsList == null || friendsList.isEmpty()) {
            return false;
        }

        String normalizedName = playerName.trim().toLowerCase();
        return friendsList.stream()
                .map(name -> name != null ? name.trim().toLowerCase() : "")
                .anyMatch(name -> name.equals(normalizedName));
    }

    public static boolean isFriendIgnoreCase(String playerName) {
        return isFriend(playerName);
    }

    public static List<String> getFriends() {
        Friends friendsModule = RadiumClient.moduleManager.getModule(Friends.class);
        if (friendsModule == null) {
            return List.of();
        }

        List<String> friendsList = friendsModule.getFriendsList();
        return friendsList != null ? List.copyOf(friendsList) : List.of();
    }
}


