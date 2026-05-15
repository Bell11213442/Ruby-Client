package com.radium.client.modules.donut;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.BlockUtil;
import com.radium.client.utils.Character.CenterCharacter;
import com.radium.client.utils.Character.RotateCharacter;
import com.radium.client.utils.TunnelBaseFinder.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

import static com.radium.client.client.RadiumClient.eventManager;

public class TunnelBaseFinder extends Module implements GameRenderListener {

    private static final int MIN_Y_LEVEL = -59;
    private static final long PHASE_TIME_MS = 5000;
    private static final Item[] JUNK_ITEMS = {

            Items.STONE,
            Items.COBBLESTONE,
            Items.DEEPSLATE,
            Items.COBBLED_DEEPSLATE,
            Items.ANDESITE,
            Items.DIORITE,
            Items.GRANITE,
            Items.TUFF,
            Items.CALCITE,
            Items.DRIPSTONE_BLOCK,
            Items.POINTED_DRIPSTONE,
            Items.GRAVEL,
            Items.FLINT,
            Items.DIRT,
            Items.GRASS_BLOCK,
            Items.COARSE_DIRT,
            Items.ROOTED_DIRT,
            Items.NETHERRACK,
            Items.BLACKSTONE,
            Items.BASALT,
            Items.SMOOTH_BASALT,
            Items.END_STONE,
            Items.COAL,
            Items.RAW_IRON,
            Items.RAW_COPPER,
            Items.COBWEB,
            Items.STRING,
            Items.CLAY,
            Items.TOTEM_OF_UNDYING
    };
    public final ModeSetting<Mode> mode = new ModeSetting<>(("Mining Style"), Mode.AMETHYST, Mode.class);
    public final BooleanSetting spawnercritical = new BooleanSetting(("Spawner Critical"), false);
    private final NumberSetting BlockSlot = new NumberSetting(("Obsidian Slot"), 2, 1, 9, 1);
    private final NumberSetting PearlSlot = new NumberSetting(("Pearl Slot"), 3, 1, 9, 1);
    private final NumberSetting XPSlot = new NumberSetting(("Bottle Slot"), 4, 1, 9, 1);
    private final NumberSetting CarrotSlot = new NumberSetting(("GoldenCarrot Slot"), 5, 1, 9, 1);
    private final BooleanSetting humanize = new BooleanSetting("Humanize", true);
    private final NumberSetting delayRandomness = new NumberSetting("Delay Randomness", 3, 0, 10, 1);
    public CenterCharacter centerCharacter;
    public RotateCharacter rotateCharacter;
    public Direction currentDirection;
    public State state = State.NONE;
    public State backup = State.NONE;
    boolean isRotating = false;
    // for xp
    XpBuyState xpbuystate = XpBuyState.NONE;
    int xpwaitcounter = 0;
    // for pearl
    PearlBuyState pearlbuystate = PearlBuyState.NONE;
    int pearlwaitcounter = 0;
    // forr obi
    ObiBuyState obibuystate = ObiBuyState.NONE;
    int obiwaitcounter = 0;
    // for carrot
    CarrotBuyState carrotbuystate = CarrotBuyState.NONE;
    int carrotwaitcounter = 0;
    int waitTarget = 0;
    // for stuck
    int stuckTicks = 0;
    BlockPos lastCoords;
    int chests = 0;
    int hoppers = 0;
    int dispensers = 0;
    int enderChests = 0;
    int shulkers = 0;
    int movingPiston;
    boolean foundSpawner;
    boolean goddamnihateniggers = true;
    boolean jumped;
    private boolean yRecoveryRotationDone = false;
    private BlockPos yRecoveryBasePos = null;
    private boolean shouldCloseInventory = false;
    private final int swingTick = 0;
    private int resetMiningTick = 0;
    private int resetUseTick = 0;
    private boolean wasScreenOpen = false;
    // for clearing, i have no idea if this will worrk but nigger yes
    private boolean isBackup = false;
    private Direction backupDirection;
    private int mendingGraceTicks = 0;
    private MendStage mendStage = MendStage.ENSURE;
    private Phase phase = Phase.DIG;
    private long phaseStartTime = 0;
    private boolean towerRotationDone = false;
    private BlockPos towerBasePos = null;

    public TunnelBaseFinder() {
        super("TunnelBaseFinder", "Digs in tunnels until you find a base", Category.DONUT);
        addSettings(mode, spawnercritical, BlockSlot, PearlSlot, XPSlot, CarrotSlot, humanize, delayRandomness);
    }

    private static int convertSlotIndex(final int slotIndex) {
        if (slotIndex < 9) {
            return 36 + slotIndex;
        }
        return slotIndex;
    }

    public static TunnelBaseFinder get() {
        return RadiumClient.moduleManager.getModule(TunnelBaseFinder.class);
    }

    private int getRandomDelay(int baseDelay) {
        if (!humanize.getValue()) return baseDelay;
        int randomPart = (int) (Math.random() * delayRandomness.getValue() * 2) - delayRandomness.getValue().intValue();
        return Math.max(1, baseDelay + randomPart);
    }

    private boolean isHoldingPickaxe() {
        if (mc.player == null)
            return false;
        return mc.player.getMainHandStack().getItem() instanceof PickaxeItem;
    }

    private void scanForBase() {
        if (mc.player == null || mc.world == null)
            return;
        chests = 0;
        hoppers = 0;
        dispensers = 0;
        enderChests = 0;
        shulkers = 0;
        movingPiston = 0;
        foundSpawner = false;
        BlockUtil.getLoadedChunks().forEach(chunk -> {
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                BlockEntity be = mc.world.getBlockEntity(pos);
                if (be == null)
                    continue;

                if (be instanceof MobSpawnerBlockEntity) {
                    foundSpawner = true;
                }

                if (be.getPos().getY() > 0)
                    continue;

                switch (be) {
                    case ChestBlockEntity chestBlockEntity -> chests++;
                    case ShulkerBoxBlockEntity shulkerBoxBlockEntity -> shulkers++;
                    case PistonBlockEntity pistonBlockEntity -> movingPiston++;
                    default -> {
                    }
                }
            }
        });

        boolean foundBase = false;
        String reason = "";
        if (chests >= 35) {
            foundBase = true;
            reason = "BASE";
        } else if (shulkers >= 35) {
            foundBase = true;
            reason = "BASE";
        } else if (movingPiston >= 10) {
            reason = "BASE";
        } else if (foundSpawner && spawnercritical.getValue()) {
            foundBase = true;
            reason = "SPAWNER";
        }

        if (foundBase) {
            disconnect("YOU FOUND A " + reason);
        }
    }

    private int findPickaxeInHotbar() {
        if (mc.player == null)
            return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof PickaxeItem) {
                return i;
            }
        }
        return -1;
    }

    public boolean ensureXpInHotbarSlot() {
        if (mc.player == null || mc.interactionManager == null)
            return false;

        if (hasXPInOffhand()) {
            return true;
        }

        if (!isPickaxeLowDurability()) {
            return false;
        }

        int targetHotbarSlot = XPSlot.getValue().intValue() - 1;
        ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);

        if (hotbarStack.getItem() == Items.EXPERIENCE_BOTTLE) {
            return true;
        }

        int xpSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                xpSlot = i;
                break;
            }
        }

        if (xpSlot == -1) {
            state = State.BUYXP;
            return false;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
            return false;
        }

        var handler = mc.player.playerScreenHandler;
        int syncId = handler.syncId;

        int fromSlot = convertSlotIndex(xpSlot);
        int toSlot = convertSlotIndex(targetHotbarSlot);
        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        shouldCloseInventory = true;
        return false;
    }

    public boolean ensurePearlInHotbarSlot() {
        if (mc.player == null || mc.interactionManager == null)
            return false;

        int targetHotbarSlot = PearlSlot.getValue().intValue() - 1;
        ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);

        if (hotbarStack.getItem() == Items.ENDER_PEARL)
            return true;

        int pearlSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot == -1) {
            state = State.BUYPEARL;
            return false;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
            return false;
        }

        var handler = mc.player.playerScreenHandler;
        int syncId = handler.syncId;

        int fromSlot = convertSlotIndex(pearlSlot);
        int toSlot = convertSlotIndex(targetHotbarSlot);

        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        shouldCloseInventory = true;
        return false;
    }

    public boolean ensureGoldenCarrotInHotbarSlot() {
        if (mc.player == null || mc.interactionManager == null)
            return false;

        int targetHotbarSlot = CarrotSlot.getValue().intValue() - 1;
        ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);

        if (hotbarStack.getItem() == Items.GOLDEN_CARROT)
            return true;

        int carrotSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.GOLDEN_CARROT) {
                carrotSlot = i;
                break;
            }
        }

        if (carrotSlot == -1) {
            state = State.BUYCARROT;
            return false;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
            return false;
        }

        var handler = mc.player.playerScreenHandler;
        int syncId = handler.syncId;

        int fromSlot = convertSlotIndex(carrotSlot);
        int toSlot = convertSlotIndex(targetHotbarSlot);

        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        shouldCloseInventory = true;
        return false;
    }

    public boolean ensureObsidianInHotbarSlot() {
        if (mc.player == null || mc.interactionManager == null)
            return false;

        int targetHotbarSlot = BlockSlot.getValue().intValue() - 1;
        ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);

        if (hotbarStack.getItem() == Items.OBSIDIAN)
            return true;

        int obsidianSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.OBSIDIAN) {
                obsidianSlot = i;
                break;
            }
        }

        if (obsidianSlot == -1) {
            state = State.BUYOBI;
            return false;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
            return false;
        }

        var handler = mc.player.playerScreenHandler;
        int syncId = handler.syncId;

        int fromSlot = convertSlotIndex(obsidianSlot);
        int toSlot = convertSlotIndex(targetHotbarSlot);

        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
        shouldCloseInventory = true;
        return false;
    }

    public void handleMend() {
        switch (mendStage) {

            case ENSURE -> {
                if (!ensureXpInHotbarSlot())
                    return;

                mendStage = MendStage.ROTATE_DOWN;
            }

            case ROTATE_DOWN -> {
                if (Math.abs(mc.player.getPitch() - 90) > 0.05) {
                    if (!isRotating) {
                        rotateCharacter.rotate(mc.player.getYaw(), 90, () -> {
                            mc.player.getInventory().selectedSlot = XPSlot.getValue().intValue() - 1;
                            mendStage = MendStage.OFFHAND_XP;
                        });
                    }
                } else {
                    mendStage = MendStage.OFFHAND_XP;
                }
            }

            case OFFHAND_XP -> {
                if (hasXPInOffhand()) {
                    mendStage = MendStage.THROW_XP;
                    return;
                }

                var handler = mc.player.playerScreenHandler;
                int sync = handler.syncId;
                int offHandSlot = 45;

                mendingGraceTicks = 40;

                mc.interactionManager.clickSlot(
                        sync,
                        convertSlotIndex(XPSlot.getValue().intValue() - 1),
                        0,
                        SlotActionType.PICKUP,
                        mc.player);

                mc.interactionManager.clickSlot(
                        sync,
                        offHandSlot,
                        0,
                        SlotActionType.PICKUP,
                        mc.player);

                if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.TOTEM_OF_UNDYING) {
                    mc.interactionManager.clickSlot(
                            sync,
                            convertSlotIndex(XPSlot.getValue().intValue() - 1),
                            0,
                            SlotActionType.PICKUP,
                            mc.player);
                }

                mendStage = MendStage.THROW_XP;
            }

            case THROW_XP -> {
                if (!hasXPInOffhand()) {
                    mendStage = MendStage.REOFFHAND_TOTEM;
                    return;
                }

                if (isOffhandEmpty()) {
                    mendStage = MendStage.REOFFHAND_TOTEM;
                    return;
                }

                mendingGraceTicks = 40;
                updateUsage(true);
                mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
            }

            case REOFFHAND_TOTEM -> {
                updateUsage(false);

                if (!hasTotemInOffhand()) {
                    offhandTotem(findTotemInInventory());
                    mendingGraceTicks = 40;
                    return;
                }

                mendStage = MendStage.ROTATE_BACK;
            }

            case ROTATE_BACK -> {
                Vec2f values = getValues(currentDirection);

                if (!isRotating) {
                    rotateCharacter.rotate(values.x, values.y, () -> {
                        mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
                        mendStage = MendStage.RESET;
                    });
                }
            }

            case RESET -> {
                mendStage = MendStage.ENSURE;
                state = backup;
                backup = State.NONE;
            }
        }
    }

    public void handleXPBuy() {
        switch (xpbuystate) {
            case NONE -> xpbuystate = XpBuyState.OPENSHOP;
            case OPENSHOP -> {
                mc.getNetworkHandler().sendChatCommand("shop");
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
                xpbuystate = XpBuyState.WAIT1;
            }
            case WAIT1 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.CLICKGEAR;
                }
            }
            case CLICKGEAR -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(11).getStack().isOf(Items.END_STONE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        xpbuystate = XpBuyState.NONE;
                        return;
                    }
                } else {
                    xpbuystate = XpBuyState.NONE;
                    return;
                }
                xpbuystate = XpBuyState.WAIT2;
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT2 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.CLICKXP;
                }
            }
            case CLICKXP -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 16, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        xpbuystate = XpBuyState.NONE;
                        return;
                    }
                } else {
                    xpbuystate = XpBuyState.NONE;
                    return;
                }
                xpbuystate = XpBuyState.WAIT3;
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT3 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.CLICKSTACK;
                }
            }
            case CLICKSTACK -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 17, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        xpbuystate = XpBuyState.NONE;
                        return;
                    }
                } else {
                    xpbuystate = XpBuyState.NONE;
                    return;
                }
                xpbuystate = XpBuyState.WAIT4;
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT4 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.DROPITEMS;
                }
            }
            case DROPITEMS -> {
                if (isInventoryFull()) {
                    dropJunkStack();
                }
                xpbuystate = XpBuyState.WAIT5;
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT5 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.BUY;
                }
            }
            case BUY -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(23).hasStack()) {
                        mc.interactionManager.clickSlot(handler.syncId, 23, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        xpbuystate = XpBuyState.NONE;
                        return;
                    }
                } else {
                    xpbuystate = XpBuyState.NONE;
                    return;
                }
                xpbuystate = XpBuyState.WAIT6;
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT6 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.CLOSE;
                }
            }
            case CLOSE -> {
                if (mc.currentScreen != null) {
                    mc.execute(() -> mc.currentScreen.close());
                }
                xpbuystate = XpBuyState.WAIT7;
                xpwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT7 -> {
                if (xpwaitcounter < waitTarget) {
                    xpwaitcounter++;
                } else {
                    xpbuystate = XpBuyState.RESET;
                }
            }
            case RESET -> {
                xpbuystate = XpBuyState.NONE;
                xpwaitcounter = 0;
                state = State.AUTOMEND;
            }
        }
    }

    public void handlePearlBuy() {
        switch (pearlbuystate) {
            case NONE -> pearlbuystate = PearlBuyState.OPENSHOP;
            case OPENSHOP -> {
                mc.getNetworkHandler().sendChatCommand("shop");
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
                pearlbuystate = PearlBuyState.WAIT1;
            }
            case WAIT1 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.CLICKGEAR;
                }
            }
            case CLICKGEAR -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(11).getStack().isOf(Items.END_STONE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        pearlbuystate = PearlBuyState.NONE;
                        return;
                    }
                } else {
                    pearlbuystate = PearlBuyState.NONE;
                    return;
                }
                pearlbuystate = PearlBuyState.WAIT2;
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT2 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.CLICKPEARL;
                }
            }
            case CLICKPEARL -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 14, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        pearlbuystate = PearlBuyState.NONE;
                        return;
                    }
                } else {
                    pearlbuystate = PearlBuyState.NONE;
                    return;
                }
                pearlbuystate = PearlBuyState.WAIT3;
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT3 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.CLICKSTACK;
                }
            }
            case CLICKSTACK -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 17, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        pearlbuystate = PearlBuyState.NONE;
                        return;
                    }
                } else {
                    pearlbuystate = PearlBuyState.NONE;
                    return;
                }
                pearlbuystate = PearlBuyState.WAIT4;
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT4 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.DROPITEMS;
                }
            }
            case DROPITEMS -> {
                if (isInventoryFull()) {
                    dropJunkStack();
                }
                pearlbuystate = PearlBuyState.WAIT5;
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT5 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.BUY;
                }
            }
            case BUY -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(23).hasStack()) {
                        mc.interactionManager.clickSlot(handler.syncId, 23, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        pearlbuystate = PearlBuyState.NONE;
                        return;
                    }
                } else {
                    pearlbuystate = PearlBuyState.NONE;
                    return;
                }
                pearlbuystate = PearlBuyState.WAIT6;
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT6 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.CLOSE;
                }
            }
            case CLOSE -> {
                if (mc.currentScreen != null) {
                    mc.execute(() -> mc.currentScreen.close());
                }
                pearlbuystate = PearlBuyState.WAIT7;
                pearlwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT7 -> {
                if (pearlwaitcounter < waitTarget) {
                    pearlwaitcounter++;
                } else {
                    pearlbuystate = PearlBuyState.RESET;
                }
            }
            case RESET -> {
                pearlbuystate = PearlBuyState.NONE;
                pearlwaitcounter = 0;
                state = State.PEARL;
            }
        }
    }

    public void handleObiBuy() {
        switch (obibuystate) {
            case NONE -> obibuystate = ObiBuyState.OPENSHOP;
            case OPENSHOP -> {
                mc.getNetworkHandler().sendChatCommand("shop");
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
                obibuystate = ObiBuyState.WAIT1;
            }
            case WAIT1 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.CLICKGEAR;
                }
            }
            case CLICKGEAR -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(11).getStack().isOf(Items.END_STONE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        obibuystate = ObiBuyState.NONE;
                        return;
                    }
                } else {
                    obibuystate = ObiBuyState.NONE;
                    return;
                }
                obibuystate = ObiBuyState.WAIT2;
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT2 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.CLICKOBI;
                }
            }
            case CLICKOBI -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 9, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        obibuystate = ObiBuyState.NONE;
                        return;
                    }
                } else {
                    obibuystate = ObiBuyState.NONE;
                    return;
                }
                obibuystate = ObiBuyState.WAIT3;
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT3 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.CLICKSTACK;
                }
            }
            case CLICKSTACK -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 17, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        obibuystate = ObiBuyState.NONE;
                        return;
                    }
                } else {
                    obibuystate = ObiBuyState.NONE;
                    return;
                }
                obibuystate = ObiBuyState.WAIT4;
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT4 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.DROPITEMS;
                }
            }
            case DROPITEMS -> {
                if (isInventoryFull()) {
                    dropJunkStack();
                }
                obibuystate = ObiBuyState.WAIT5;
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT5 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.BUY;
                }
            }
            case BUY -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(23).hasStack()) {
                        mc.interactionManager.clickSlot(handler.syncId, 23, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        obibuystate = ObiBuyState.NONE;
                        return;
                    }
                } else {
                    obibuystate = ObiBuyState.NONE;
                    return;
                }
                obibuystate = ObiBuyState.WAIT6;
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT6 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.CLOSE;
                }
            }
            case CLOSE -> {
                if (mc.currentScreen != null) {
                    mc.execute(() -> mc.currentScreen.close());
                }
                obibuystate = ObiBuyState.WAIT7;
                obiwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT7 -> {
                if (obiwaitcounter < waitTarget) {
                    obiwaitcounter++;
                } else {
                    obibuystate = ObiBuyState.RESET;
                }
            }
            case RESET -> {
                obibuystate = ObiBuyState.NONE;
                obiwaitcounter = 0;
                state = State.MINING;
            }
        }
    }

    public void handleCarrotBuy() {
        switch (carrotbuystate) {
            case NONE -> carrotbuystate = CarrotBuyState.OPENSHOP;
            case OPENSHOP -> {
                mc.getNetworkHandler().sendChatCommand("shop");
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
                carrotbuystate = CarrotBuyState.WAIT1;
            }
            case WAIT1 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.CLICKFOOD;
                }
            }
            case CLICKFOOD -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(11).getStack().isOf(Items.END_STONE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 14, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        carrotbuystate = CarrotBuyState.NONE;
                        return;
                    }
                } else {
                    carrotbuystate = CarrotBuyState.NONE;
                    return;
                }
                carrotbuystate = CarrotBuyState.WAIT2;
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT2 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.CLICKCARROT;
                }
            }
            case CLICKCARROT -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(16).getStack().isOf(Items.GOLDEN_CARROT)) {
                        mc.interactionManager.clickSlot(handler.syncId, 16, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        carrotbuystate = CarrotBuyState.NONE;
                        return;
                    }
                } else {
                    carrotbuystate = CarrotBuyState.NONE;
                    return;
                }
                carrotbuystate = CarrotBuyState.WAIT3;
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT3 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.CLICKSTACK;
                }
            }
            case CLICKSTACK -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        mc.interactionManager.clickSlot(handler.syncId, 17, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        carrotbuystate = CarrotBuyState.NONE;
                        return;
                    }
                } else {
                    carrotbuystate = CarrotBuyState.NONE;
                    return;
                }
                carrotbuystate = CarrotBuyState.WAIT4;
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT4 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.DROPITEMS;
                }
            }
            case DROPITEMS -> {
                if (isInventoryFull()) {
                    dropJunkStack();
                }
                carrotbuystate = CarrotBuyState.WAIT5;
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT5 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.BUY;
                }
            }
            case BUY -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    if (handler.getSlot(23).hasStack()) {
                        mc.interactionManager.clickSlot(handler.syncId, 23, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        carrotbuystate = CarrotBuyState.NONE;
                        return;
                    }
                } else {
                    carrotbuystate = CarrotBuyState.NONE;
                    return;
                }
                carrotbuystate = CarrotBuyState.WAIT6;
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT6 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.CLOSE;
                }
            }
            case CLOSE -> {
                if (mc.currentScreen != null) {
                    mc.execute(() -> mc.currentScreen.close());
                }
                carrotbuystate = CarrotBuyState.WAIT7;
                carrotwaitcounter = 0;
                waitTarget = getRandomDelay(7);
            }
            case WAIT7 -> {
                if (carrotwaitcounter < waitTarget) {
                    carrotwaitcounter++;
                } else {
                    carrotbuystate = CarrotBuyState.RESET;
                }
            }
            case RESET -> {
                carrotbuystate = CarrotBuyState.NONE;
                carrotwaitcounter = 0;
                state = State.AUTOEAT;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        eventManager.add(GameRenderListener.class, this);
        state = State.NONE;
        backup = State.NONE;
        isRotating = false;
        phase = Phase.DIG;
        phaseStartTime = 0;
        towerRotationDone = false;
        mendingGraceTicks = 0;
        stuckTicks = 0;
        lastCoords = null;
        towerBasePos = null;
        yRecoveryRotationDone = false;
        yRecoveryBasePos = null;
        mendStage = MendStage.ENSURE;
        xpbuystate = XpBuyState.NONE;
        xpwaitcounter = 0;
        pearlbuystate = PearlBuyState.NONE;
        pearlwaitcounter = 0;
        obibuystate = ObiBuyState.NONE;
        obiwaitcounter = 0;
        carrotbuystate = CarrotBuyState.NONE;
        carrotwaitcounter = 0;
        isBackup = false;

        goddamnihateniggers = true;

        if (mc != null) {
            rotateCharacter = new RotateCharacter(mc);
            centerCharacter = new CenterCharacter(mc);
        }

        if (!isHoldingPickaxe()) {
            int pickaxeSlot = findPickaxeInHotbar();
            if (pickaxeSlot != -1) {
                mc.player.getInventory().selectedSlot = pickaxeSlot;
            } else {
                disconnect("YOU DON'T HAVE PICKAXE");
                return;
            }
        }

        if (!hasTotemInOffhand()) {
            if (findTotemInInventory() != -1) {
                offhandTotem(findTotemInInventory());
            } else {
                disconnect("YOU DON'T HAVE TOTEM");
                return;
            }
        }

        currentDirection = getDir();
        Vec2f firstTurn = getValues(currentDirection);
        if (!isRotating) {
            rotateCharacter.rotate(firstTurn.x, firstTurn.y, () -> {
                centerCharacter.initiate();
                state = State.MINING;
            });
        }
    }

    private void disconnect(final String text) {
        if (mc.player != null) {
            this.toggle();
            mc.player.networkHandler
                    .onDisconnect(new DisconnectS2CPacket(Text.literal("TunnelBaseFinder | " + text)));
        }
    }

    private boolean checkHazardDirection(Direction facing, int extraDistance) {
        if (mc.player == null || mc.world == null || facing == null)
            return false;

        BlockPos playerPos = mc.player.getBlockPos();
        int playerY = playerPos.getY();

        int minY = playerY - 1;
        int maxY;
        switch (mode.getValue()) {
            case STANDING -> maxY = playerY + 2;
            case CRAWL -> maxY = playerY + 1;
            default -> maxY = playerY + 3;
        }

        int scanDistance = 5 + extraDistance;
        int expand = (mode.getValue() == Mode.AMETHYST) ? 2 : 1;

        for (int yCoord = minY; yCoord <= maxY; yCoord++) {
            int currentExpand = (yCoord >= playerY) ? expand : 0;
            for (int x = -currentExpand; x <= currentExpand; x++) {
                for (int i = 1; i <= scanDistance; i++) {
                    BlockPos forwardPos = playerPos.offset(facing, i);
                    BlockPos pos;
                    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                        pos = new BlockPos(forwardPos.getX() + x, yCoord, forwardPos.getZ());
                    } else {
                        pos = new BlockPos(forwardPos.getX(), yCoord, forwardPos.getZ() + x);
                    }

                    BlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (isLava(block)) {
                        return true;
                    }

                    if (yCoord < playerY && block == Blocks.AIR && x == 0) {
                        BlockPos checkDown = pos.down();
                        while (mc.world.getBlockState(checkDown).getBlock() == Blocks.AIR) {
                            checkDown = checkDown.down();
                            if (checkDown.getY() < -64)
                                break;
                        }
                        if (isLava(mc.world.getBlockState(checkDown).getBlock())) {
                            return true;
                        }
                    }

                    if (isGravel(block) && yCoord > playerY && x == 0) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    private boolean checkHazardAbove() {
        if (mc.player == null || mc.world == null)
            return false;

        BlockPos playerPos = mc.player.getBlockPos();
        for (int i = 1; i <= 4; i++) {
            BlockPos checkPos = playerPos.up(i);
            Block block = mc.world.getBlockState(checkPos).getBlock();
            if (isLava(block)) {
                return true;
            }
        }
        return false;
    }

    public Direction getDirLeft(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            default -> dir;
        };
    }

    public Direction getDirRight(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> dir;
        };
    }

    public void handlePearl() {
        if (!ensurePearlInHotbarSlot())
            return;

        if (goddamnihateniggers) {
            updateUsage(false);
            goddamnihateniggers = false;
            mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
            rotateCharacter.rotate(mc.player.getYaw(), 0, () -> state = State.MINING);
        }
        BlockPos playerPos = mc.player.getBlockPos();
        Direction facing = currentDirection;
        BlockPos frontPos = playerPos.offset(facing, 1);
        BlockPos aboveOne = frontPos.up().offset(facing);
        BlockPos aboveTwo = frontPos.up(3);

        if (mc.world.getBlockState(frontPos).getBlock() != Blocks.AIR &&
                mc.world.getBlockState(aboveTwo).getBlock() != Blocks.AIR) {
            mc.options.forwardKey.setPressed(false);
            updateMining(true);
            if (mc.world.getBlockState(aboveOne).getBlock() == Blocks.AIR) {
                if (!isRotating) {
                    rotateCharacter.rotate(mc.player.getYaw(), 11, () -> {
                        mc.player.getInventory().selectedSlot = PearlSlot.getValue().intValue() - 1;
                        updateUsage(true);
                        goddamnihateniggers = true;
                    });
                }
            }
        } else {
            if (mc.options != null) {
                mc.options.forwardKey.setPressed(true);
                updateMining(true);
            }
        }

        if (checkHazardDirection(currentDirection, 0)) {
            if (!checkHazardDirection(getDirLeft(currentDirection), 5)) {
                currentDirection = getDirLeft(currentDirection);
                Vec2f dir = getValues(currentDirection);
                if (!isRotating) {
                    rotateCharacter.rotate(dir.x, dir.y, () -> centerCharacter.initiate());
                }

            } else if (!checkHazardDirection(getDirRight(currentDirection), 5)) {
                currentDirection = getDirRight(currentDirection);
                Vec2f dir = getValues(currentDirection);
                if (!isRotating) {
                    rotateCharacter.rotate(dir.x, dir.y, () -> centerCharacter.initiate());
                }
            } else {
                centerCharacter.initiate();
                state = State.GOABOVEHAZARD;
            }
        }

        BlockPos down = mc.player.getBlockPos().offset(currentDirection);
        BlockPos up = down.up();
        BlockPos up2 = up.up();
        BlockPos up3 = mc.player.getBlockPos().up(2);
        if (mc.world.getBlockState(down).getBlock() != Blocks.AIR) {
            if (mc.world.getBlockState(up).getBlock() == Blocks.AIR) {
                if (mc.world.getBlockState(up2).getBlock() == Blocks.AIR) {
                    if (mc.world.getBlockState(up3).getBlock() == Blocks.AIR) {
                        mc.options.jumpKey.setPressed(true);
                        jumped = true;
                    }
                }
            }
        }

    }

    public void handleMining() {
        if (isRotating) {
            stopMovement();
            return;
        }

        if (mc.player != null && mc.player.getBlockPos().getY() < MIN_Y_LEVEL) {
            centerCharacter.initiate();
            state = State.YRECOVERY;
            yRecoveryBasePos = null;
            yRecoveryRotationDone = false;
            phase = Phase.DIG;
            phaseStartTime = System.currentTimeMillis();
            return;
        }

        mc.player.getInventory().selectedSlot = findPickaxeInHotbar();

        if (mc.player != null) {
            if (mode.getValue() == Mode.CRAWL && mc.player.getPose() != EntityPose.SWIMMING) {
                state = State.PEARL;
            }
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            BlockPos currentPos = mc.player.getBlockPos();
            if (currentPos.equals(lastCoords)) {
                if (stuckTicks < 20) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0;
                    if (!checkHazardDirection(getDirLeft(currentDirection), 5)) {
                        backupDirection = currentDirection;
                        currentDirection = getDirLeft(currentDirection);
                        Vec2f dir = getValues(currentDirection);
                        if (!isRotating) {
                            rotateCharacter.rotate(dir.x, dir.y, () -> centerCharacter.initiate());
                        }
                    } else if (!checkHazardDirection(getDirRight(currentDirection), 5)) {
                        backupDirection = currentDirection;
                        currentDirection = getDirRight(currentDirection);
                        Vec2f dir = getValues(currentDirection);
                        if (!isRotating) {
                            rotateCharacter.rotate(dir.x, dir.y, () -> {
                                centerCharacter.initiate();
                            });
                        }
                    } else {
                        centerCharacter.initiate();
                        state = State.GOABOVEHAZARD;
                    }
                }
            } else {
                stuckTicks = 0;
                lastCoords = currentPos;
            }
        } else {
            stuckTicks = 0;
            lastCoords = mc.player.getBlockPos();
        }

        BlockPos down = mc.player.getBlockPos().offset(currentDirection);
        BlockPos up = down.up();
        BlockPos up2 = up.up();
        BlockPos up3 = mc.player.getBlockPos().up(2);
        if (mc.world.getBlockState(down).getBlock() != Blocks.AIR) {
            if (mc.world.getBlockState(up).getBlock() == Blocks.AIR) {
                if (mc.world.getBlockState(up2).getBlock() == Blocks.AIR) {
                    if (mc.world.getBlockState(up3).getBlock() == Blocks.AIR) {
                        mc.options.jumpKey.setPressed(true);
                        jumped = true;
                    }
                }
            }
        }

        if (isBackup) {
            if (!checkHazardDirection(backupDirection, 0)) {
                currentDirection = backupDirection;
                Vec2f dir = getValues(currentDirection);
                isBackup = false;
                if (!isRotating) {
                    rotateCharacter.rotate(dir.x, dir.y, () -> centerCharacter.initiate());
                }
            }
        }

        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);
            if (mode.getValue() == Mode.STANDING) {
                if (mc.crosshairTarget instanceof BlockHitResult hit) {
                    BlockPos blockPos = hit.getBlockPos();
                    updateMining(blockPos.getY() >= mc.player.getBlockPos().getY());
                }
            } else {
                updateMining(true);
            }
        }
        if (checkHazardDirection(currentDirection, 0)) {
            if (!checkHazardDirection(getDirLeft(currentDirection), 5)) {
                isBackup = true;
                backupDirection = currentDirection;
                currentDirection = getDirLeft(currentDirection);
                Vec2f dir = getValues(currentDirection);
                if (!isRotating) {
                    rotateCharacter.rotate(dir.x, dir.y, () -> centerCharacter.initiate());
                }

            } else if (!checkHazardDirection(getDirRight(currentDirection), 5)) {
                isBackup = true;
                backupDirection = currentDirection;
                currentDirection = getDirRight(currentDirection);
                Vec2f dir = getValues(currentDirection);
                if (!isRotating) {
                    rotateCharacter.rotate(dir.x, dir.y, () -> centerCharacter.initiate());
                }
            } else {
                centerCharacter.initiate();
                state = State.GOABOVEHAZARD;
            }
        }
    }

    private boolean phaseTimePassed() {
        return System.currentTimeMillis() - phaseStartTime >= PHASE_TIME_MS;
    }

    private void switchPhase(Phase newPhase) {
        phase = newPhase;
        phaseStartTime = System.currentTimeMillis();
        towerRotationDone = false;

        updateMining(false);
        updateUsage(false);
        mc.options.jumpKey.setPressed(false);
    }

    private void captureAboveBlocks(BlockPos pos) {
        for (int i = 1; i <= 3; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = mc.world.getBlockState(checkPos);
        }
    }

    private void resetTowerState() {
        towerBasePos = null;
        towerRotationDone = false;
        phase = Phase.DIG;
        phaseStartTime = System.currentTimeMillis();
    }

    public void handleGoAbove() {
        if (!ensureObsidianInHotbarSlot())
            return;

        if (mc.player == null || mc.world == null)
            return;
        if (checkHazardAbove()) {
            disconnect("NO SAFE PATH - HAZARD ABOVE");
            return;
        }

        if (towerBasePos == null) {
            towerBasePos = mc.player.getBlockPos();
            captureAboveBlocks(towerBasePos);
            phase = Phase.DIG;
            phaseStartTime = System.currentTimeMillis();
        }

        BlockPos currentPos = mc.player.getBlockPos();
        BlockPos belowPos = currentPos.down();

        if (mc.world.getBlockState(belowPos).getBlock() != Blocks.AIR && !checkHazardDirection(currentDirection, 0)) {
            Vec2f dir = getValues(currentDirection);
            if (!isRotating) {
                rotateCharacter.rotate(dir.x, dir.y, () -> {
                    centerCharacter.initiate();
                    mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
                    state = State.MINING;
                    resetTowerState();
                });
            }
            updateMining(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            return;
        }

        if (isRotating) {
            updateMining(false);
            return;
        }

        switch (phase) {
            case DIG -> {
                mc.options.jumpKey.setPressed(false);
                mc.options.useKey.setPressed(false);

                if (!isRotating) {
                    rotateCharacter.rotate(mc.player.getYaw(), -90f, () -> {
                        mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
                        updateMining(true);
                    });
                }

                if (phaseTimePassed()) {
                    updateMining(false);
                    switchPhase(Phase.TOWER);
                }
            }

            case TOWER -> {
                updateMining(false);
                if (!towerRotationDone && !isRotating) {
                    towerRotationDone = true;
                    rotateCharacter.rotate(mc.player.getYaw(), 90f, () -> mc.player.getInventory().selectedSlot = BlockSlot.getValue().intValue() - 1);
                }
                mc.options.jumpKey.setPressed(true);
                updateUsage(true);

                if (phaseTimePassed()) {
                    mc.options.jumpKey.setPressed(false);
                    updateUsage(false);
                    switchPhase(Phase.DIG);
                }
            }
        }
    }

    public void handleYRecovery() {
        if (!ensureObsidianInHotbarSlot())
            return;

        if (mc.player == null || mc.world == null)
            return;
        if (yRecoveryBasePos == null) {
            yRecoveryBasePos = mc.player.getBlockPos();
            phase = Phase.DIG;
            phaseStartTime = System.currentTimeMillis();
        }

        BlockPos currentPos = mc.player.getBlockPos();
        BlockPos belowPos = currentPos.down();
        if (currentPos.getY() >= MIN_Y_LEVEL && mc.world.getBlockState(belowPos).getBlock() != Blocks.AIR) {
            Vec2f dir = getValues(currentDirection);
            if (!isRotating) {
                rotateCharacter.rotate(dir.x, dir.y, () -> {
                    centerCharacter.initiate();
                    mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
                    state = State.MINING;
                    yRecoveryBasePos = null;
                    yRecoveryRotationDone = false;
                    phase = Phase.DIG;
                    phaseStartTime = System.currentTimeMillis();
                });
            }
            updateMining(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            return;
        }

        if (isRotating) {
            updateMining(false);
            return;
        }

        switch (phase) {
            case DIG -> {
                mc.options.jumpKey.setPressed(false);
                mc.options.useKey.setPressed(false);

                if (!isRotating) {
                    rotateCharacter.rotate(mc.player.getYaw(), -90f, () -> {
                        mc.player.getInventory().selectedSlot = findPickaxeInHotbar();
                        updateMining(true);
                    });
                }

                if (phaseTimePassed()) {
                    updateMining(false);
                    switchPhase(Phase.TOWER);
                }
            }

            case TOWER -> {
                updateMining(false);
                if (!yRecoveryRotationDone && !isRotating) {
                    yRecoveryRotationDone = true;
                    rotateCharacter.rotate(mc.player.getYaw(), 90f, () -> mc.player.getInventory().selectedSlot = BlockSlot.getValue().intValue() - 1);
                }
                mc.options.jumpKey.setPressed(true);
                mc.options.useKey.setPressed(true);

                if (phaseTimePassed()) {
                    mc.options.jumpKey.setPressed(false);
                    mc.options.useKey.setPressed(false);
                    yRecoveryRotationDone = false;
                    switchPhase(Phase.DIG);
                }
            }
        }
    }

    private boolean isPickaxeLowDurability() {
        if (mc.player == null)
            return false;

        ItemStack stack = mc.player.getMainHandStack();
        if (!(stack.getItem() instanceof PickaxeItem))
            return false;

        int max = stack.getMaxDamage();
        int remaining = max - stack.getDamage();

        if (max <= 0)
            return false;

        double percent = (remaining / (double) max) * 100.0;
        return percent <= 5.0;
    }

    public void handleEating() {
        if (!ensureGoldenCarrotInHotbarSlot())
            return;

        assert mc.player != null;
        if (mc.player.getInventory().selectedSlot != CarrotSlot.getValue().intValue() - 1) {
            mc.player.getInventory().selectedSlot = CarrotSlot.getValue().intValue() - 1;
        } else {
            if (mc.player.getHungerManager().getFoodLevel() <= 6) {
                updateUsage(true);
            } else {
                updateUsage(false);
                state = backup;
                backup = State.NONE;
            }
        }
    }

    @Override
    public void onTick() {
        super.onTick();
        if (mc.player == null || mc.world == null)
            return;

        if (jumped) {
            jumped = false;
            mc.options.jumpKey.setPressed(false);
        }

        mc.execute(() -> {
            if (shouldCloseInventory) {
                if (mc.currentScreen instanceof InventoryScreen) {
                    mc.currentScreen.close();
                }
                shouldCloseInventory = false;
            }

            scanForBase();

            if (mendingGraceTicks > 0) {
                mendingGraceTicks--;
            }

            if (state != State.AUTOMEND && state != State.AUTOEAT && state != State.BUYXP && state != State.BUYPEARL
                    && state != State.BUYCARROT && state != State.BUYOBI) {
                if (mendingGraceTicks <= 0 && !hasTotemInOffhand()) {
                    disconnect("YOUR TOTEM POPPED");
                }
                if (isPickaxeLowDurability() && state != State.BUYXP) {
                    backup = state;
                    state = State.AUTOMEND;
                }
                if (mc.player.getHungerManager().getFoodLevel() <= 6 && state != State.BUYCARROT) {
                    backup = state;
                    state = State.AUTOEAT;
                }
            }

            switch (state) {
                case NONE -> {
                }
                case MINING -> handleMining();
                case GOABOVEHAZARD -> handleGoAbove();
                case YRECOVERY -> handleYRecovery();
                case PEARL -> handlePearl();
                case AUTOMEND -> handleMend();
                case AUTOEAT -> handleEating();
                case BUYXP -> handleXPBuy();
                case BUYPEARL -> handlePearlBuy();
                case BUYCARROT -> handleCarrotBuy();
                case BUYOBI -> handleObiBuy();
            }
            if (centerCharacter.isCentering()) {
                centerCharacter.update();
            }
            wasScreenOpen = mc.currentScreen != null;
        });
    }

    private boolean isLava(Block block) {
        return block == Blocks.LAVA || block == Blocks.WATER;
    }

    private boolean isGravel(Block block) {
        return block == Blocks.GRAVEL;
    }

    private boolean hasTotemInOffhand() {
        if (mc.player == null)
            return false;
        ItemStack offhandStack = mc.player.getOffHandStack();
        return offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private boolean hasXPInOffhand() {
        if (mc.player == null)
            return false;
        ItemStack offhandStack = mc.player.getOffHandStack();
        return offhandStack.getItem() == Items.EXPERIENCE_BOTTLE;
    }

    private boolean isOffhandEmpty() {
        if (mc.player == null)
            return false;
        return mc.player.getOffHandStack().isEmpty();
    }

    private int findTotemInInventory() {
        if (mc.player == null)
            return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }

    private void offhandTotem(int invSlot) {
        if (mc.player == null)
            return;

        var handler = mc.player.playerScreenHandler;
        int sync = handler.syncId;
        int handlerSlot = convertSlotIndex(invSlot);
        int offHandSlot = 45;

        mc.interactionManager.clickSlot(sync, handlerSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(sync, offHandSlot, 0, SlotActionType.PICKUP, mc.player);
        if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.TOTEM_OF_UNDYING) {
            mc.interactionManager.clickSlot(sync, handlerSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    private void updateMining(boolean mining) {
        boolean isScreenOpen = mc.currentScreen != null;
        if (mining) {
            if (resetMiningTick > 0) {
                resetMiningTick--;
                mc.options.attackKey.setPressed(false);
                // wasScreenOpen = isScreenOpen; // Handled in onTick
                return;
            }

            if (wasScreenOpen && !isScreenOpen) {
                mc.options.attackKey.setPressed(false);
                resetMiningTick = 1;
                // wasScreenOpen = isScreenOpen; // Handled in onTick
                return;
            }

            mc.options.attackKey.setPressed(true);
        } else {
            resetMiningTick = 0;
            mc.options.attackKey.setPressed(false);
        }
        // wasScreenOpen = isScreenOpen; // Handled in onTick
    }

    private void updateUsage(boolean active) {
        boolean isScreenOpen = mc.currentScreen != null;
        if (active) {
            if (resetUseTick > 0) {
                resetUseTick--;
                mc.options.useKey.setPressed(false);
                return;
            }

            if (wasScreenOpen && !isScreenOpen) {
                mc.options.useKey.setPressed(false);
                resetUseTick = 1;
                return;
            }

            mc.options.useKey.setPressed(true);
        } else {
            resetUseTick = 0;
            mc.options.useKey.setPressed(false);
        }
    }

    private void stopMovement() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            updateUsage(false);
            mc.options.jumpKey.setPressed(false);
            updateMining(false);
        }
    }

    private boolean isInventoryFull() {
        if (mc.player == null)
            return true;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean dropJunkStack() {
        if (mc.player == null)
            return false;

        ScreenHandler handler = mc.player.currentScreenHandler;
        int syncId = handler.syncId;
        boolean droppedAny = false;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty())
                continue;

            boolean isJunk = false;
            for (Item junk : JUNK_ITEMS) {
                if (stack.getItem() == junk) {
                    isJunk = true;
                    break;
                }
            }

            if (isJunk) {
                int slot;
                if (handler instanceof GenericContainerScreenHandler container) {
                    int rows = container.getRows();
                    if (i < 9) {
                        slot = rows * 9 + 27 + i;
                    } else {
                        slot = rows * 9 + (i - 9);
                    }
                } else {
                    slot = convertSlotIndex(i);
                }

                mc.interactionManager.clickSlot(
                        syncId,
                        slot,
                        1,
                        SlotActionType.THROW,
                        mc.player);
                droppedAny = true;
            }
        }
        return droppedAny;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(GameRenderListener.class, this);
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            updateMining(false);
            updateUsage(false);
            mc.options.jumpKey.setPressed(false);
        }
        state = State.NONE;
        backup = State.NONE;
        isRotating = false;
        phase = Phase.DIG;
        phaseStartTime = 0;
        towerRotationDone = false;
        towerBasePos = null;
        currentDirection = null;
        yRecoveryRotationDone = false;
        yRecoveryBasePos = null;
        mendingGraceTicks = 0;
        wasScreenOpen = false;
        resetMiningTick = 0;
        resetUseTick = 0;
    }

    public Direction getDir() {
        PlayerEntity player = mc.player;
        if (player == null) {
            return Direction.NORTH;
        }
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (pitch > 60.0f) {
            return Direction.DOWN;
        }
        if (pitch < -60.0f) {
            return Direction.UP;
        }
        float wrappedYaw = yaw % 360.0f;
        if (wrappedYaw < 0.0f) {
            wrappedYaw += 360.0f;
        }
        if (wrappedYaw >= 45.0f && wrappedYaw < 135.0f) {
            return Direction.WEST;
        }
        if (wrappedYaw >= 135.0f && wrappedYaw < 225.0f) {
            return Direction.NORTH;
        }
        if (wrappedYaw >= 225.0f && wrappedYaw < 315.0f) {
            return Direction.EAST;
        }
        return Direction.SOUTH;
    }

    public Vec2f getValues(Direction direction) {
        float yaw = 0;
        float pitch = mode.getValue() == Mode.STANDING ? 45 : 0;
        switch (direction) {
            case NORTH:
                yaw = 180;
                break;
            case SOUTH:
                yaw = 0;
                break;
            case WEST:
                yaw = 90;
                break;
            case EAST:
                yaw = 270;
                break;
            case UP:
                yaw = 0;
                pitch = -90;
                break;
            case DOWN:
                yaw = 0;
                pitch = 90;
                break;
        }
        return new Vec2f(yaw, pitch);
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (rotateCharacter.isActive()) {
            isRotating = true;
            rotateCharacter.update(true, false);
        } else {
            isRotating = false;
        }
    }

    private enum Phase {
        DIG,
        TOWER
    }

    public enum Mode {
        CRAWL, STANDING, AMETHYST
    }
}