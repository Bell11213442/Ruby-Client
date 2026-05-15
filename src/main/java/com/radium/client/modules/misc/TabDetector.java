package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.StringListSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabDetector extends Module {
    private final StringListSetting targetPlayers = new StringListSetting("Target Players");
    private final ModeSetting<NotificationMode> notificationMode = new ModeSetting<>("Notification Mode", NotificationMode.Both, NotificationMode.class);
    private final BooleanSetting logOffline = new BooleanSetting("Log Offline", true);

    private final Set<String> currentTargetPlayers = new HashSet<>();
    private final Set<String> previousTargetPlayers = new HashSet<>();

    public TabDetector() {
        super("TabDetector", "Detects when specific players join or leave the server", Category.MISC);
        addSettings(targetPlayers, notificationMode, logOffline);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        currentTargetPlayers.clear();

        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();

        for (PlayerListEntry entry : playerList) {
            String playerName = entry.getProfile().getName();

            for (String targetName : targetPlayers.getList()) {
                if (targetName.equalsIgnoreCase(playerName)) {
                    currentTargetPlayers.add(playerName);
                    break;
                }
            }
        }


        Set<String> newPlayers = new HashSet<>(currentTargetPlayers);
        newPlayers.removeAll(previousTargetPlayers);

        if (!newPlayers.isEmpty()) {
            handlePlayerJoin(newPlayers);
        }


        if (logOffline.getValue()) {
            Set<String> leftPlayers = new HashSet<>(previousTargetPlayers);
            leftPlayers.removeAll(currentTargetPlayers);

            if (!leftPlayers.isEmpty()) {
                handlePlayerLeave(leftPlayers);
            }
        }

        previousTargetPlayers.clear();
        previousTargetPlayers.addAll(currentTargetPlayers);
    }

    private void handlePlayerJoin(Set<String> players) {
        String playerList = String.join(", ", players);
        String message = players.size() == 1 ?
                "Target player joined: " + playerList :
                "Target players joined: " + playerList;

        NotificationMode mode = notificationMode.getValue();

        if (mode == NotificationMode.Chat || mode == NotificationMode.Both) {
            ChatUtils.m(message);
        }

        if (mode == NotificationMode.Toast || mode == NotificationMode.Both) {
            String toastMessage = players.size() == 1 ? "Target Player Joined!" : "Target Players Joined!";
            com.radium.client.utils.ToastNotificationManager.getInstance().show(
                    "TabDetector",
                    toastMessage,
                    com.radium.client.utils.ToastNotification.ToastType.WARNING
            );
        }
    }

    private void handlePlayerLeave(Set<String> players) {
        String playerList = String.join(", ", players);
        String message = players.size() == 1 ?
                "Target player left: " + playerList :
                "Target players left: " + playerList;

        NotificationMode mode = notificationMode.getValue();

        if (mode == NotificationMode.Chat || mode == NotificationMode.Both) {
            ChatUtils.m(message);
        }

        if (mode == NotificationMode.Toast || mode == NotificationMode.Both) {
            String toastMessage = players.size() == 1 ? "Target Player Left!" : "Target Players Left!";
            com.radium.client.utils.ToastNotificationManager.getInstance().show(
                    "TabDetector",
                    toastMessage,
                    com.radium.client.utils.ToastNotification.ToastType.INFO
            );
        }
    }

    @Override
    public void onEnable() {
        currentTargetPlayers.clear();
        previousTargetPlayers.clear();


        if (mc.getNetworkHandler() != null) {
            Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
            for (PlayerListEntry entry : playerList) {
                String playerName = entry.getProfile().getName();
                for (String targetName : targetPlayers.getList()) {
                    if (targetName.equalsIgnoreCase(playerName)) {
                        previousTargetPlayers.add(playerName);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        currentTargetPlayers.clear();
        previousTargetPlayers.clear();
    }

    public List<String> getTargetPlayers() {
        return targetPlayers.getList();
    }

    public void addTargetPlayer(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            targetPlayers.add(playerName.trim());
        }
    }

    public void removeTargetPlayer(String playerName) {
        if (playerName != null) {
            targetPlayers.remove(playerName.trim());
        }
    }

    public int getOnlineTargetCount() {
        return currentTargetPlayers.size();
    }

    public enum NotificationMode {
        Chat,
        Toast,
        Both
    }
}
