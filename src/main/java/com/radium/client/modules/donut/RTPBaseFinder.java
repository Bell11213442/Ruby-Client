package com.radium.client.modules.donut;
// radium client

import com.radium.client.events.event.GameRenderListener;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.BlockUtil;
import com.radium.client.utils.Character.RotateCharacter;
import net.minecraft.block.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static com.radium.client.client.RadiumClient.eventManager;

public class RTPBaseFinder extends Module implements GameRenderListener {

    private final ModeSetting<RTP> RTPLocation = new ModeSetting<>("Region", RTP.random, RTP.class);

    int chests = 0;
    int hoppers = 0;
    int dispensers = 0;
    int enderChests = 0;
    int shulkers = 0;
    boolean foundSpawner;

    boolean rotateDown = false;

    RotateCharacter rotator;
    boolean sentRTP = false;
    boolean isRotateDown = false;
    private long worldNotNullSince = -1;
    private boolean wasWorldNull = true;
    private boolean pendingRotation = false;
    private boolean firstRotationAfterEnable = true;

    public RTPBaseFinder() {
        super("RTPBaseFinder", "Finds bases by digging down", Category.DONUT);
        addSettings(RTPLocation);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        mc.execute(() -> mc.currentScreen.close());
        eventManager.add(GameRenderListener.class, this);
        worldNotNullSince = -1;
        wasWorldNull = true;
        pendingRotation = false;
        firstRotationAfterEnable = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(GameRenderListener.class, this);
    }

    @Override
    public void onGameRender(GameRenderListener.GameRenderEvent event) {
        if (isRotateDown && !rotator.isActive()) {
            rotator.rotate(mc.player.getYaw(), 85.453567575667456465234f, () -> {
                isRotateDown = false;
            });
        }

        if (isRotateDown && rotator.isActive()) {
            rotator.update(true, false);
        }
    }

    void sendRTP() {
        switch (RTPLocation.getValue()) {
            case naeast -> {
                mc.getNetworkHandler().sendChatCommand("rtp east");
            }
            case nawest -> {
                mc.getNetworkHandler().sendChatCommand("rtp west");
            }
            case euwest -> {
                mc.getNetworkHandler().sendChatCommand("rtp eu west");
            }
            case eucentral -> {
                mc.getNetworkHandler().sendChatCommand("rtp eu central");
            }
            case asia -> {
                mc.getNetworkHandler().sendChatCommand("rtp asia");
            }
            case oceania -> {
                mc.getNetworkHandler().sendChatCommand("rtp oceania");
            }
            case random -> {
                int r = (int) (Math.random() * 6);
                switch (r) {
                    case 0 -> mc.getNetworkHandler().sendChatCommand("rtp east");
                    case 1 -> mc.getNetworkHandler().sendChatCommand("rtp west");
                    case 2 -> mc.getNetworkHandler().sendChatCommand("rtp eu west");
                    case 3 -> mc.getNetworkHandler().sendChatCommand("rtp eu central");
                    case 4 -> mc.getNetworkHandler().sendChatCommand("rtp asia");
                    case 5 -> mc.getNetworkHandler().sendChatCommand("rtp oceania");
                }
            }

        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (rotator == null) {
            rotator = new RotateCharacter(mc);
        }
        boolean isWorldNull = mc.world == null;
        if (isWorldNull) {
            worldNotNullSince = -1;
            wasWorldNull = true;
            pendingRotation = false;
        } else if (wasWorldNull) {
            worldNotNullSince = System.currentTimeMillis();
            wasWorldNull = false;
        }
        if (pendingRotation && worldNotNullSince != -1) {
            long timeSinceWorldNotNull = System.currentTimeMillis() - worldNotNullSince;
            if (timeSinceWorldNotNull >= 2500) {
                pendingRotation = false;
                isRotateDown = true;
                firstRotationAfterEnable = false;
            }
        }

        if (!hasTotemInOffhand()) {
            disconnect("Totem Popped");
        }

        scanForBase();

        if ((int) mc.player.getPitch() == 85) {
            isRotateDown = false;
            if (mc.player.getY() > 0) {
                mc.options.attackKey.setPressed(true);
                mc.options.sneakKey.setPressed(true);
            } else {
                mc.options.attackKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
                if (!sentRTP) {
                    sendRTP();
                    sentRTP = true;
                }
            }

        } else {
            sentRTP = false;
            if (!isRotateDown) {
                if (firstRotationAfterEnable) {
                    isRotateDown = true;
                    firstRotationAfterEnable = false;
                    pendingRotation = false;
                } else if (mc.world == null || worldNotNullSince == -1) {
                    pendingRotation = true;
                } else {
                    long timeSinceWorldNotNull = System.currentTimeMillis() - worldNotNullSince;
                    if (timeSinceWorldNotNull >= 2500) {
                        isRotateDown = true;
                        pendingRotation = false;
                    } else {
                        pendingRotation = true;
                    }
                }
            }
        }
    }

    private void scanForBase() {
        if (mc.player == null || mc.world == null) return;
        chests = 0;
        hoppers = 0;
        dispensers = 0;
        enderChests = 0;
        shulkers = 0;
        foundSpawner = false;
        BlockUtil.getLoadedChunks().forEach(chunk -> {
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                BlockEntity be = mc.world.getBlockEntity(pos);
                if (be == null) continue;

                if (be instanceof MobSpawnerBlockEntity) {
                    foundSpawner = true;
                }

                if (be.getPos().getY() > 0) continue;

                if (be instanceof ChestBlockEntity) {
                    chests++;
                } else if (be instanceof HopperBlockEntity) {
                    hoppers++;
                } else if (be instanceof DispenserBlockEntity) {
                    dispensers++;
                } else if (be instanceof EnderChestBlockEntity) {
                    enderChests++;
                } else if (be instanceof ShulkerBoxBlockEntity) {
                    shulkers++;
                }
            }
        });

        boolean foundBase = false;
        String reason = "";
        if (chests >= 20) {
            foundBase = true;
            reason = "Chest threshold reached";
        } else if (shulkers >= 20) {
            foundBase = true;
            reason = "Shulker threshold reached";
        } else if (foundSpawner) {
            foundBase = true;
            reason = "Spawner found";
        }

        if (foundBase) {
            disconnect(reason);
        }
    }

    private boolean hasTotemInOffhand() {
        if (mc.player == null) return false;
        ItemStack offhandStack = mc.player.getOffHandStack();
        return offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private void disconnect(final String text) {
        if (mc.world == null || worldNotNullSince == -1) {
            return;
        }

        long timeSinceWorldNotNull = System.currentTimeMillis() - worldNotNullSince;
        if (timeSinceWorldNotNull < 2500) {
            return;
        }
        toggle();
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("RTPBaseFinder | " + text)));
        }
    }

    enum RTP {
        random,
        naeast,
        nawest,
        euwest,
        eucentral,
        asia,
        oceania
    }
}

