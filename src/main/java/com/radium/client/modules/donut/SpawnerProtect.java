package com.radium.client.modules.donut; // radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.client.Friends;
import com.radium.client.modules.misc.AutoReconnect;
import com.radium.client.utils.Character.RotateCharacter;
import com.radium.client.utils.ChatUtils;
import com.radium.client.utils.WebhookUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static com.radium.client.client.RadiumClient.eventManager;

public class SpawnerProtect extends Module implements TickListener, GameRenderListener {

    private final BooleanSetting fastMode = new BooleanSetting("Fast Mode", true);
    private final NumberSetting emergencyDistance = new NumberSetting("Emergency Distance", 5.0, 1.0, 50.0, 0.5);
    private final BooleanSetting webhookEnabled = new BooleanSetting("Webhook", false);
    private final StringSetting webhookUrl = new StringSetting("Webhook URL", "");
    private final BooleanSetting selfPing = new BooleanSetting("Self Ping", false);
    private final StringSetting discordId = new StringSetting("Discord ID", "");

    ProtectState state = ProtectState.CHECKING;
    boolean foundPlayer = false;
    List<BlockPos> spawnerPositions = new ArrayList<>();
    int searchRadius = 10;
    int currentSpawnerIndex = 0;
    int miningTicks = 0;
    boolean isMining = false;
    int previousSlot = -1;
    BlockPos enderChestPos = null;
    int dumpSlot = 0;
    int dumpDelay = 0;
    boolean hasOpenedChest = false;
    int verificationTicks = 0;
    boolean isVerifying = false;
    RotateCharacter rotateChar;

    private BlockPos lastPosition = null;
    private boolean isCoordChangeProtected = false;
    private int coordChangeCooldown = 0;

    public SpawnerProtect() {
        super("SpawnerProtect", "Breaks all spawners around you when players are nearby and dumps your inventory in an e-chest", Category.DONUT);
        addSettings(fastMode, emergencyDistance, webhookEnabled, webhookUrl, selfPing, discordId);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            return;
        }

        if (findPickaxeSlot() == -1) {
            ChatUtils.e("Please Get A Silk Touch Pickaxe!");
            toggle();
            return;
        }

        if (mc.currentScreen != null) {
            mc.execute(() -> {
                mc.currentScreen.close();
            });
        }

        int pickaxeSlot = findPickaxeSlot();
        if (pickaxeSlot != -1) {
            previousSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = pickaxeSlot;
        }

        foundPlayer = false;
        spawnerPositions.clear();
        eventManager.add(TickListener.class, this);
        eventManager.add(GameRenderListener.class, this);
        state = ProtectState.CHECKING;
        rotateChar = new RotateCharacter(RadiumClient.mc);
        currentSpawnerIndex = 0;
        miningTicks = 0;
        isMining = false;
        enderChestPos = null;
        dumpSlot = 0;
        dumpDelay = 0;
        hasOpenedChest = false;
        verificationTicks = 0;
        isVerifying = false;

        if (mc.player != null) {
            lastPosition = mc.player.getBlockPos();
        }
        isCoordChangeProtected = false;
        coordChangeCooldown = 0;
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(GameRenderListener.class, this);

        if (previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
        }

        mc.player.setSneaking(false);
        mc.options.sneakKey.setPressed(false);
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (state == ProtectState.MINING || state == ProtectState.OPENENDERCHEST) {
            rotateChar.update(true, fastMode.getValue());
        }
    }

    void checkForCoordinateChange() {
        if (mc.player == null) return;

        BlockPos currentPosition = mc.player.getBlockPos();

        if (lastPosition != null && !currentPosition.equals(lastPosition)) {
            isCoordChangeProtected = !isCoordChangeProtected;

            if (isCoordChangeProtected) {
                ChatUtils.w("Restart Detected");
                state = ProtectState.CHECKING;
                foundPlayer = false;
                spawnerPositions.clear();
                currentSpawnerIndex = 0;
                isMining = false;

                if (previousSlot != -1) {
                    mc.player.getInventory().selectedSlot = previousSlot;
                    previousSlot = -1;
                }

                mc.options.attackKey.setPressed(false);
            } else {
                ChatUtils.m("Resuming");
            }

            coordChangeCooldown = 40;
        }

        lastPosition = currentPosition;

        if (coordChangeCooldown > 0) {
            coordChangeCooldown--;
        }
    }

    void checkPlayer() {
        if (isCoordChangeProtected) {
            return;
        }

        String selfName = mc.player.getGameProfile().getName();
        Friends friends = RadiumClient.moduleManager.getModule(Friends.class);
        AutoReconnect autoReconnect = RadiumClient.moduleManager.getModule(AutoReconnect.class);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            String name = player.getGameProfile().getName();
            if (name.equalsIgnoreCase(selfName) || name.equalsIgnoreCase("venom")) continue;
            if (player.isSpectator()) continue;
            if (player == null) continue;
            if (friends != null && friends.isFriend(name)) continue;

            if (autoReconnect != null && autoReconnect.isEnabled()) {
                autoReconnect.toggle();
            }

            double distance = mc.player.distanceTo(player);

            if (emergencyDistance.getValue() > 0 && distance <= emergencyDistance.getValue()) {
                sendWebhook(name, distance);
                disconnect("Emergency Distance Triggered! Player: " + name + " (" + String.format("%.1f", distance) + " blocks)");
                return;
            }

            ChatUtils.m("Player Detected: " + name);
            sendWebhook(name, distance);
            foundPlayer = true;
            state = ProtectState.FINDSPAWNER;
            return;
        }
    }

    void findSpawners() {
        spawnerPositions.clear();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.SPAWNER) {
                        spawnerPositions.add(pos);
                    }
                }
            }
        }

        if (spawnerPositions.isEmpty()) {
            ChatUtils.w("No Spawners Found");
            disconnect("No Spawners Found");
            state = ProtectState.CHECKING;
        } else {
            ChatUtils.m("Found " + spawnerPositions.size() + " Spawners");
            currentSpawnerIndex = 0;
            state = ProtectState.MINING;
        }
    }

    int findPickaxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof PickaxeItem) {
                RegistryEntry<Enchantment> silkTouch = mc.world.getRegistryManager()
                        .get(RegistryKeys.ENCHANTMENT)
                        .getEntry(Identifier.of("minecraft", "silk_touch"))
                        .orElse(null);

                if (silkTouch != null && stack.getEnchantments().getLevel(silkTouch) > 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    void mineSpawners() {
        if (currentSpawnerIndex >= spawnerPositions.size()) {
            ChatUtils.m("Mined Spawners");

            if (previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
                previousSlot = -1;
            }

            mc.options.attackKey.setPressed(false);
            state = ProtectState.FINDENDERCHEST;
            spawnerPositions.clear();
            currentSpawnerIndex = 0;
            return;
        }

        BlockPos targetPos = spawnerPositions.get(currentSpawnerIndex);
        Block block = mc.world.getBlockState(targetPos).getBlock();

        if (block != Blocks.SPAWNER && !isVerifying) {
            isVerifying = true;
            verificationTicks = 0;
            mc.options.attackKey.setPressed(false);
            return;
        }

        if (isVerifying) {
            verificationTicks++;
            if (verificationTicks < 10) {
                return;
            } else {
                Block verifyBlock = mc.world.getBlockState(targetPos).getBlock();
                if (verifyBlock != Blocks.SPAWNER) {
                    ChatUtils.m("Spawner " + (currentSpawnerIndex + 1) + " Already Broken, Moving To Next");
                    currentSpawnerIndex++;
                    isMining = false;
                    miningTicks = 0;
                    isVerifying = false;
                    verificationTicks = 0;
                    return;
                } else {
                    ChatUtils.m("Spawner " + (currentSpawnerIndex + 1) + " Detected After Verification");
                    isVerifying = false;
                    verificationTicks = 0;
                    isMining = false;
                }
            }
        }

        if (!isMining && previousSlot == -1) {
            int pickaxeSlot = findPickaxeSlot();
            if (pickaxeSlot == -1) {
                ChatUtils.e("No Silk Touch Pickaxe Found In Hotbar!");
                disconnect("No Silk Touch Pickaxe Found In Hotbar!");
                state = ProtectState.CHECKING;
                return;
            }
            previousSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = pickaxeSlot;
        }

        Vec3d targetVec = Vec3d.ofCenter(targetPos);
        Vec3d playerVec = mc.player.getEyePos();
        Vec3d direction = targetVec.subtract(playerVec).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.asin(direction.y));

        if (!isMining) {
            mc.options.attackKey.setPressed(false);
            if (!rotateChar.isActive()) {
                rotateChar.rotate(yaw, pitch, () -> {
                    isMining = true;
                    miningTicks = 0;
                });
            }
        } else {
            miningTicks++;
            mc.options.attackKey.setPressed(true);

            if (miningTicks % 5 == 0) {
                Block currentBlock = mc.world.getBlockState(targetPos).getBlock();
                if (currentBlock != Blocks.SPAWNER) {
                    ChatUtils.m("Mined Spawner " + (currentSpawnerIndex + 1) + "/" + spawnerPositions.size());
                    mc.options.attackKey.setPressed(false);
                    currentSpawnerIndex++;
                    isMining = false;
                    miningTicks = 0;
                }
            }
        }
    }

    void findEnderChest() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.ENDER_CHEST) {
                        double dist = playerPos.getSquaredDistance(pos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }

        if (nearest == null) {
            disconnect("No E-Chest Found!");
            state = ProtectState.CHECKING;
        } else {
            enderChestPos = nearest;
            ChatUtils.m("Found E-Chest at " + nearest.toShortString());
            mc.player.setSneaking(false);
            mc.options.sneakKey.setPressed(false);
            state = ProtectState.OPENENDERCHEST;
        }
    }

    void openEnderChest() {
        if (enderChestPos == null) {
            state = ProtectState.CHECKING;
            return;
        }

        if (mc.world.getBlockState(enderChestPos).getBlock() != Blocks.ENDER_CHEST) {
            disconnect("E-Chest vanished!");
            state = ProtectState.CHECKING;
            return;
        }

        Vec3d targetVec = Vec3d.ofCenter(enderChestPos);
        Vec3d playerVec = mc.player.getEyePos();
        Vec3d direction = targetVec.subtract(playerVec).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.asin(direction.y));

        if (!rotateChar.isActive() && !hasOpenedChest) {
            rotateChar.rotate(yaw, pitch, () -> {
                BlockHitResult hitResult = new BlockHitResult(
                        Vec3d.ofCenter(enderChestPos),
                        Direction.UP,
                        enderChestPos,
                        false
                );
                mc.interactionManager.interactBlock(
                        mc.player,
                        Hand.MAIN_HAND,
                        hitResult
                );
                hasOpenedChest = true;
                dumpDelay = 10;
                ChatUtils.m("Opening E-Chest");
            });
        }

        if (hasOpenedChest && dumpDelay > 0) {
            dumpDelay--;
        } else if (hasOpenedChest && dumpDelay == 0) {
            state = ProtectState.DUMPINVENTORY;
            dumpSlot = 0;
        }
    }

    void dumpInventory() {
        ScreenHandler handler = mc.player.currentScreenHandler;

        if (handler == null || handler == mc.player.playerScreenHandler) {
            ChatUtils.e("E-Chest Not Opened");
            hasOpenedChest = false;
            state = ProtectState.OPENENDERCHEST;
            return;
        }

        int containerSlots = handler.slots.size() - 36;
        int playerInvStart = containerSlots;

        if (dumpSlot < 36) {
            int screenSlot = playerInvStart + dumpSlot;
            mc.interactionManager.clickSlot(
                    handler.syncId,
                    screenSlot,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mc.player
            );
            dumpSlot++;
        } else {
            ChatUtils.m("Dumped Inventory");
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.closeHandledScreen();
                }
            });
            enderChestPos = null;
            hasOpenedChest = false;
            dumpSlot = 0;
            state = ProtectState.CHECKING;
            disconnect("Spawners Saved!");
        }
    }

    private void sendWebhook(String playerName, double distance) {
        if (!webhookEnabled.getValue() || webhookUrl.getValue().trim().isEmpty()) {
            return;
        }

        String selfPingId = "";
        if (selfPing.getValue() && !discordId.getValue().trim().isEmpty()) {
            selfPingId = discordId.getValue().trim();
        }

        BlockPos pos = mc.player.getBlockPos();
        new WebhookUtils(webhookUrl.getValue())
                .setTitle("Player Detected!")
                .setDescription("A player was detected near spawners")
                .setSelfPing(selfPingId)
                .addUsername(playerName)
                .addCoords(pos)
                .addField("Distance", String.format("%.2f blocks", distance), true)
                .addServer()
                .addTime()
                .send();
    }

    private void disconnect(final String text) {
        this.toggle();
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("SpawnerProtect | " + text)));
    }

    @Override
    public void onTick2() {
        if (mc.world == null || mc.player == null) return;

        checkForCoordinateChange();

        if (state == ProtectState.CHECKING || state == ProtectState.FINDSPAWNER || state == ProtectState.MINING) {
            mc.player.setSneaking(true);
            mc.options.sneakKey.setPressed(true);
        }

        switch (state) {
            case CHECKING -> checkPlayer();
            case FINDSPAWNER -> findSpawners();
            case MINING -> mineSpawners();
            case FINDENDERCHEST -> findEnderChest();
            case OPENENDERCHEST -> openEnderChest();
            case DUMPINVENTORY -> dumpInventory();
        }
    }

    enum ProtectState {
        CHECKING,
        FINDSPAWNER,
        MINING,
        FINDENDERCHEST,
        OPENENDERCHEST,
        DUMPINVENTORY
    }
}