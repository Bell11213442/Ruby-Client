package com.radium.client.modules.misc;
// radium client

import com.mojang.authlib.GameProfile;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer extends Module {

    public final NumberSetting maxFakePlayers = new NumberSetting("Max Players", 5.0, 1.0, 20.0, 1.0);
    public final NumberSetting spawnDistance = new NumberSetting("Spawn Distance", 3.0, 1.0, 10.0, 0.5);
    public final StringSetting playerName = new StringSetting("Player Name", "Steve");
    public final BooleanSetting copyRotation = new BooleanSetting("Copy Rotation", true);
    public final BooleanSetting copyPose = new BooleanSetting("Copy Pose", true);

    private final List<OtherClientPlayerEntity> fakePlayers = new ArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final String[] spawnModes = {"Front", "Behind", "Left", "Right", "Above"};
    private int spawnModeIndex = 0;

    public FakePlayer() {
        super("FakePlayer", "Spawns fake players for testing ESP and other visual modules", Category.MISC);
        addSettings(
                maxFakePlayers,
                spawnDistance,
                playerName,
                copyRotation,
                copyPose
        );
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        spawnFakePlayer();
    }

    @Override
    public void onDisable() {
        clearFakePlayers();
    }

    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        fakePlayers.removeIf(fakePlayer -> mc.world.getEntityById(fakePlayer.getId()) == null);
    }

    public void spawnFakePlayer() {
        if (mc.player == null || mc.world == null) return;
        if (fakePlayers.size() >= maxFakePlayers.getValue().intValue()) return;

        double dist = spawnDistance.getValue();
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();

        double radYaw = Math.toRadians(yaw);
        double sinYaw = Math.sin(radYaw);
        double cosYaw = Math.cos(radYaw);

        String mode = spawnModes[spawnModeIndex];
        spawnModeIndex = (spawnModeIndex + 1) % spawnModes.length;

        if (mode.equals("Front")) {
            x += -sinYaw * dist;
            z += cosYaw * dist;
        } else if (mode.equals("Behind")) {
            x += sinYaw * dist;
            z += -cosYaw * dist;
        } else if (mode.equals("Left")) {
            x += cosYaw * dist;
            z += sinYaw * dist;
        } else if (mode.equals("Right")) {
            x += -cosYaw * dist;
            z += -sinYaw * dist;
        } else if (mode.equals("Above")) {
            y += dist;
        }

        String name = playerName.getValue();
        if (name == null || name.trim().isEmpty()) {
            name = "Steve";
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        OtherClientPlayerEntity fakePlayer = new OtherClientPlayerEntity(mc.world, profile);

        fakePlayer.refreshPositionAndAngles(x, y, z, 0, 0);
        fakePlayer.setVelocity(0, 0, 0);
        fakePlayer.velocityModified = true;

        if (copyRotation.getValue()) {
            float playerYaw = mc.player.getYaw();
            float playerPitch = mc.player.getPitch();
            fakePlayer.setYaw(playerYaw);
            fakePlayer.setPitch(playerPitch);
            fakePlayer.headYaw = mc.player.headYaw;
            fakePlayer.bodyYaw = mc.player.bodyYaw;
            fakePlayer.prevYaw = playerYaw;
            fakePlayer.prevPitch = playerPitch;
            fakePlayer.prevHeadYaw = mc.player.headYaw;
            fakePlayer.prevBodyYaw = mc.player.bodyYaw;
        }

        if (copyPose.getValue()) {
            fakePlayer.setPose(mc.player.getPose());
        }

        fakePlayer.setHealth(20.0f);
        fakePlayer.getInventory().clone(mc.player.getInventory());

        fakePlayer.setInvisible(false);
        fakePlayer.noClip = false;

        mc.world.addEntity(fakePlayer);

        fakePlayers.add(fakePlayer);
    }

    public void clearFakePlayers() {
        if (mc.world == null) return;

        for (OtherClientPlayerEntity fakePlayer : new ArrayList<>(fakePlayers)) {
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayers.clear();
    }
}
