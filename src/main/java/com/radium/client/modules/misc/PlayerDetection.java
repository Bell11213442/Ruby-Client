package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import com.radium.client.utils.FriendsManager;
import com.radium.client.utils.WebhookUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class PlayerDetection extends Module {

    private final BooleanSetting disconnect = new BooleanSetting("Disconnect", false);
    private final ModeSetting<NotificationMode> notificationMode = new ModeSetting<>("Notification Mode", NotificationMode.BOTH, NotificationMode.class);
    private final BooleanSetting toggleWhenDetected = new BooleanSetting("Toggle when a player is detected", false);

    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", false);

    private final BooleanSetting webhook = new BooleanSetting("Webhook", false);
    private final StringSetting webhookUrl = new StringSetting("Webhook URL", "");
    private final BooleanSetting selfPing = new BooleanSetting("Self Ping", false);
    private final StringSetting discordId = new StringSetting("Discord ID", "");

    private final BooleanSetting enablePanicPay = new BooleanSetting("Enable Panic Pay", false);
    private final StringSetting targetPlayer = new StringSetting("Target Player", "Denniball");
    private final StringSetting amount = new StringSetting("Amount", "1m");

    private boolean triggered = false;

    public PlayerDetection() {
        super("PlayerDetect", "Detects when players are in the world", Category.MISC);
        addSettings(
                disconnect, notificationMode, toggleWhenDetected,
                ignoreFriends,
                webhook, webhookUrl, selfPing, discordId,
                enablePanicPay, targetPlayer, amount
        );
    }

    @Override
    public void onEnable() {
        triggered = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (triggered && !toggleWhenDetected.getValue()) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.equals(mc.player)) {
                continue;
            }

            String playerName = player.getName().getString();

            if (ignoreFriends.getValue() && FriendsManager.isFriend(playerName)) {
                continue;
            }

            if (!triggered) {
                trigger(player);
                break;
            }
        }
    }

    private void trigger(PlayerEntity detectedPlayer) {
        triggered = true;
        String playerName = detectedPlayer.getName().getString();

        sendNotifications(playerName, detectedPlayer);

        if (webhook.getValue()) {
            sendWebhookNotification(detectedPlayer);
        }

        if (disconnect.getValue() && mc.player.networkHandler != null) {
            mc.player.networkHandler.getConnection().disconnect(Text.literal("[Radium] Player detected nearby. Disconnecting for safety."));
        }

        if (enablePanicPay.getValue() && !targetPlayer.getValue().trim().isEmpty() && !amount.getValue().trim().isEmpty()) {
            sendPanicPay();
        }

        if (toggleWhenDetected.getValue()) {
            this.toggle();
        }
    }

    private void sendNotifications(String playerName, PlayerEntity detectedPlayer) {
        if (mc.player == null) return;

        String message = "Player detected: " + playerName + " (" + String.format("%.1f", mc.player.distanceTo(detectedPlayer)) + " blocks away)";

        if (notificationMode.isMode(NotificationMode.CHAT) || notificationMode.isMode(NotificationMode.BOTH)) {
            ChatUtils.m(message);
        }

        if (notificationMode.isMode(NotificationMode.TOAST) || notificationMode.isMode(NotificationMode.BOTH)) {
            com.radium.client.utils.ToastNotificationManager.getInstance().show(
                    "Player Detection",
                    playerName + " detected",
                    com.radium.client.utils.ToastNotification.ToastType.WARNING
            );
        }

        mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW, 1.0f, 1.0f);
    }

    private void sendWebhookNotification(PlayerEntity detectedPlayer) {
        String url = webhookUrl.getValue().trim();
        if (url.isEmpty() || !url.startsWith("https://discord.com/api/webhooks/")) {
            return;
        }

        String selfPingId = "";
        if (selfPing.getValue() && !discordId.getValue().trim().isEmpty()) {
            selfPingId = discordId.getValue().trim();
        }

        double distance = mc.player != null ? mc.player.distanceTo(detectedPlayer) : 0.0;

        String yourCoords = mc.player != null
                ? String.format("%.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ())
                : "Unknown";

        String playerCoords = String.format("%.1f, %.1f, %.1f",
                detectedPlayer.getX(), detectedPlayer.getY(), detectedPlayer.getZ());

        new WebhookUtils(url)
                .setTitle("Player Detected!")
                .setDescription("")
                .setSelfPing(selfPingId)
                .addField("Detected Player", detectedPlayer.getName().getString(), true)
                .addField("At Distance", String.format("%.2f blocks", distance), true)
                .addField("Your Coords", yourCoords, false)
                .addField("Player Coords", playerCoords, false)
                .addServer()
                .send();
    }

    private void sendPanicPay() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        String player = targetPlayer.getValue().trim();
        String amountStr = amount.getValue().trim();

        if (player.isEmpty() || amountStr.isEmpty()) {
            return;
        }

        mc.player.networkHandler.sendChatCommand("pay " + player + " " + amountStr);
    }

    public enum NotificationMode {
        CHAT("Chat", 0),
        TOAST("Toast", 1),
        BOTH("Both", 2);

        NotificationMode(String name, int ordinal) {
        }
    }
}
