package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.*;
import com.radium.client.modules.Module;
import com.radium.client.utils.WebhookUtils;
import net.minecraft.block.entity.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class StashFinder extends Module {
    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting barrels = new BooleanSetting("Barrels", true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", true);
    private final BooleanSetting furnaces = new BooleanSetting("Furnaces", true);
    private final BooleanSetting dispensersDroppers = new BooleanSetting("Dispensers/Droppers", true);
    private final BooleanSetting hoppers = new BooleanSetting("Hoppers", true);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);

    private final NumberSetting minimumStorageCount = new NumberSetting("Min Storage Count", 4.0, 1.0, 100.0, 1.0);
    private final NumberSetting minimumDistance = new NumberSetting("Min Distance", 0.0, 0.0, 10000.0, 100.0);
    private final BooleanSetting criticalSpawner = new BooleanSetting("Critical Spawner", true);
    private final BooleanSetting disconnectOnFind = new BooleanSetting("Disconnect On Find", false);
    private final BooleanSetting sendNotifications = new BooleanSetting("Send Notifications", true);
    private final ModeSetting<NotificationMode> notificationMode = new ModeSetting<>("Notification Mode", NotificationMode.BOTH, NotificationMode.class);

    private final BooleanSetting enableWebhook = new BooleanSetting("Enable Webhook", false);
    private final StringSetting webhookUrl = new StringSetting("Webhook URL", "");
    private final BooleanSetting selfPing = new BooleanSetting("Self Ping", false);
    private final StringSetting discordId = new StringSetting("Discord ID", "");
    private final Set<ChunkPos> processedChunks = new HashSet<>();

    public StashFinder() {
        super("StashFinder", "Finds and tracks stashes with webhook support", Category.MISC);

        Setting<?>[] settings = new Setting<?>[]{
                chests, barrels, shulkers, enderChests, furnaces, dispensersDroppers, hoppers, spawners,
                minimumStorageCount, minimumDistance, criticalSpawner, disconnectOnFind,
                sendNotifications, notificationMode, enableWebhook, webhookUrl, selfPing, discordId
        };
        this.addSettings(settings);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        processedChunks.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        processedChunks.clear();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        ChunkPos playerChunk = mc.player.getChunkPos();
        int renderDistance = mc.options.getViewDistance().getValue();

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                ChunkPos chunkPos = new ChunkPos(playerChunk.x + x, playerChunk.z + z);

                if (processedChunks.contains(chunkPos)) continue;

                WorldChunk chunk = mc.world.getChunk(chunkPos.x, chunkPos.z);
                if (chunk != null) {
                    processChunk(chunk);
                    processedChunks.add(chunkPos);
                }
            }
        }
    }

    private void processChunk(WorldChunk worldChunk) {
        ChunkPos chunkPos = worldChunk.getPos();
        double chunkXAbs = Math.abs(chunkPos.x * 16);
        double chunkZAbs = Math.abs(chunkPos.z * 16);

        if (Math.sqrt(chunkXAbs * chunkXAbs + chunkZAbs * chunkZAbs) < minimumDistance.getValue()) return;

        int chestsCount = 0;
        int barrelsCount = 0;
        int shulkersCount = 0;
        int enderChestsCount = 0;
        int furnacesCount = 0;
        int dispensersDroppersCount = 0;
        int hoppersCount = 0;
        int spawnersCount = 0;

        for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
            if (spawners.getValue() && blockEntity instanceof MobSpawnerBlockEntity) {
                spawnersCount++;
                continue;
            }

            if (chests.getValue() && blockEntity instanceof ChestBlockEntity) chestsCount++;
            else if (barrels.getValue() && blockEntity instanceof BarrelBlockEntity) barrelsCount++;
            else if (shulkers.getValue() && blockEntity instanceof ShulkerBoxBlockEntity) shulkersCount++;
            else if (enderChests.getValue() && blockEntity instanceof EnderChestBlockEntity) enderChestsCount++;
            else if (furnaces.getValue() && blockEntity instanceof AbstractFurnaceBlockEntity) furnacesCount++;
            else if (dispensersDroppers.getValue() && blockEntity instanceof DispenserBlockEntity)
                dispensersDroppersCount++;
            else if (hoppers.getValue() && blockEntity instanceof HopperBlockEntity) hoppersCount++;
        }

        int totalStorage = chestsCount + barrelsCount + shulkersCount + enderChestsCount +
                furnacesCount + dispensersDroppersCount + hoppersCount + spawnersCount;

        boolean isStash = false;
        boolean isCriticalSpawner = false;
        String detectionReason = "";

        if (criticalSpawner.getValue() && spawnersCount > 0) {
            isStash = true;
            isCriticalSpawner = true;
            detectionReason = "Spawner(s) detected (Critical mode)";
        } else if (totalStorage >= minimumStorageCount.getValue().intValue()) {
            isStash = true;
            detectionReason = "Storage threshold reached (" + totalStorage + " blocks)";
        }

        if (isStash) {
            int x = chunkPos.x * 16 + 8;
            int z = chunkPos.z * 16 + 8;

            if (sendNotifications.getValue() && mc.player != null) {
                String stashType = isCriticalSpawner ? "spawner base" : "stash";
                String message = "Found " + stashType + " at " + x + ", " + z + ". " + detectionReason;

                if (notificationMode.isMode(NotificationMode.CHAT) || notificationMode.isMode(NotificationMode.BOTH)) {
                    mc.player.sendMessage(Text.of("§a[Stash Finder] §f" + message), false);
                }

                if (notificationMode.isMode(NotificationMode.TOAST) || notificationMode.isMode(NotificationMode.BOTH)) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            "Stash Finder",
                            "Found " + stashType,
                            com.radium.client.utils.ToastNotification.ToastType.SUCCESS
                    );
                }

                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW, 1.0f, 1.0f);
            }

            if (enableWebhook.getValue()) {
                sendWebhookNotification(x, z, chestsCount, barrelsCount, shulkersCount, enderChestsCount,
                        furnacesCount, dispensersDroppersCount, hoppersCount, spawnersCount,
                        isCriticalSpawner, detectionReason);
            }

            if (disconnectOnFind.getValue()) {
                toggle();
                if (mc.world != null) {
                    mc.world.disconnect();
                }
            }
        }
    }

    private void sendWebhookNotification(int x, int z, int chestsCount, int barrelsCount, int shulkersCount,
                                         int enderChestsCount, int furnacesCount, int dispensersDroppersCount,
                                         int hoppersCount, int spawnersCount, boolean isCriticalSpawner,
                                         String detectionReason) {
        String url = webhookUrl.getValue().trim();
        if (url.isEmpty()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.of("§c[Stash Finder] Webhook URL not configured!"), false);
            }
            return;
        }

        String selfPingId = "";
        if (selfPing.getValue() && !discordId.getValue().trim().isEmpty()) {
            selfPingId = discordId.getValue().trim();
        }

        String stashType = isCriticalSpawner ? "Spawner Base" : "Stash";
        String description = String.format("%s found at coordinates %d, %d!", stashType, x, z);

        StringBuilder itemsFound = new StringBuilder();
        int totalItems = 0;

        if (spawnersCount > 0) {
            itemsFound.append("Spawners: ").append(spawnersCount).append("\n");
            totalItems += spawnersCount;
        }
        if (chestsCount > 0) {
            itemsFound.append("Chests: ").append(chestsCount).append("\n");
            totalItems += chestsCount;
        }
        if (barrelsCount > 0) {
            itemsFound.append("Barrels: ").append(barrelsCount).append("\n");
            totalItems += barrelsCount;
        }
        if (shulkersCount > 0) {
            itemsFound.append("Shulker Boxes: ").append(shulkersCount).append("\n");
            totalItems += shulkersCount;
        }
        if (enderChestsCount > 0) {
            itemsFound.append("Ender Chests: ").append(enderChestsCount).append("\n");
            totalItems += enderChestsCount;
        }
        if (furnacesCount > 0) {
            itemsFound.append("Furnaces: ").append(furnacesCount).append("\n");
            totalItems += furnacesCount;
        }
        if (dispensersDroppersCount > 0) {
            itemsFound.append("Dispensers/Droppers: ").append(dispensersDroppersCount).append("\n");
            totalItems += dispensersDroppersCount;
        }
        if (hoppersCount > 0) {
            itemsFound.append("Hoppers: ").append(hoppersCount).append("\n");
            totalItems += hoppersCount;
        }

        new WebhookUtils(url)
                .setTitle("📦 Stashfinder Alert")
                .setDescription(description)
                .setSelfPing(selfPingId)
                .addField("Detection Reason", detectionReason, false)
                .addField("Total Items Found", String.valueOf(totalItems), false)
                .addField("Items Breakdown", itemsFound.toString(), false)
                .addField("Coordinates", x + ", " + z, true)
                .addServer()
                .addTime()
                .send();
    }

    public enum NotificationMode {
        CHAT("Chat", 0),
        TOAST("Toast", 1),
        BOTH("Both", 2);

        NotificationMode(String name, int ordinal) {
        }
    }
}
