package com.radium.client.modules.misc;
// radium client

import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.SchematicBuilderRenderer;
import com.radium.client.utils.BlockUtil;
import com.radium.client.utils.ChatUtils;
import com.radium.client.utils.InventoryUtil;
import com.radium.client.utils.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.util.*;

import static com.radium.client.client.RadiumClient.eventManager;

public class SchematicBuilder extends Module implements TickListener, GameRenderListener {

    private static final double MAX_REACH = 3.0; // Maximum block reach distance
    private static final int MAX_PATHFIND_DISTANCE = 50; // Maximum pathfinding search distance
    private static final int STUCK_THRESHOLD = 40; // ticks (2 seconds at 20 tps)
    private static final int MAX_BUILD_ATTEMPTS = 5;
    // Settings
    public final StringSetting schematicPath = new StringSetting("Schematic Path", "");
    public final BooleanSetting loadSchematic = new BooleanSetting("Load Schematic", false);
    public final BooleanSetting startBuilding = new BooleanSetting("Start Building", false);
    public final BooleanSetting stopBuilding = new BooleanSetting("Stop Building", false);
    public final BooleanSetting pauseBuilding = new BooleanSetting("Pause Building", false);
    public final BooleanSetting resumeBuilding = new BooleanSetting("Resume Building", false);
    public final BooleanSetting setOrigin = new BooleanSetting("Set Origin", false);
    public final StringSetting originCoords = new StringSetting("Origin Coords", "0,0,0");
    public final NumberSetting placeDelay = new NumberSetting("Place Delay", 2.0, 0.0, 20.0, 1.0);
    public final BooleanSetting rotateSchematic = new BooleanSetting("Rotate Schematic", false);
    public final NumberSetting rotation = new NumberSetting("Rotation", 0.0, 0.0, 3.0, 1.0); // 0=0°, 1=90°, 2=180°, 3=270°
    public final BooleanSetting showProgress = new BooleanSetting("Show Progress", true);
    // Chest interaction settings
    public final BooleanSetting autoGetBlocks = new BooleanSetting("Auto Get Blocks", true);
    public final NumberSetting chestSearchRange = new NumberSetting("Chest Range", 10.0, 1.0, 32.0, 1.0);
    public final NumberSetting chestOpenDelay = new NumberSetting("Chest Open Delay", 5.0, 0.0, 20.0, 1.0);
    public final NumberSetting itemTakeDelay = new NumberSetting("Item Take Delay", 2.0, 0.0, 10.0, 1.0);
    private final List<Long> recentPlaceTimes = new ArrayList<>(); // For calculating BPS
    private final Set<BlockPos> countedBlocks = new HashSet<>(); // Track which blocks we've counted
    // Schematic data
    private Map<BlockPos, BlockState> schematicBlocks = new HashMap<>();
    private final Map<Item, Integer> requiredBlocks = new HashMap<>(); // Item -> count needed
    private BlockPos origin = null;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int minX = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;
    private boolean isBuilding = false;
    private boolean isPaused = false;
    private int placeDelayCounter = 0;
    // Progress tracking
    private int totalBlocks = 0;
    private int blocksPlaced = 0;
    private long buildStartTime = 0;
    private long lastPlaceTime = 0;
    private double blocksPerSecond = 0.0;
    // Layer-by-layer building
    private int currentLayer = -1;
    private final List<BlockPlaceTask> currentLayerBlocks = new ArrayList<>();
    private int currentLayerIndex = 0;
    private boolean waitingForSwap = false;
    private BlockHitResult pendingPlacement = null;
    private boolean isWalkingToBlock = false;
    private BlockPos walkTarget = null;
    // Pathfinding
    private List<BlockPos> currentPath = new ArrayList<>();
    private int pathIndex = 0;
    private boolean isBuildingPath = false;
    private BlockPos pathBuildTarget = null;
    private final Set<BlockPos> temporaryBlocks = new HashSet<>(); // Blocks built for pathfinding
    // Stuck detection
    private BlockPos lastPosition = null;
    private int stuckTicks = 0;
    private int pathBuildAttempts = 0;
    // Chest interaction state
    private ChestState chestState = ChestState.NONE;
    private BlockPos targetChest = null;
    private Item neededItem = null;
    private int chestOpenDelayCounter = 0;
    private int itemTakeDelayCounter = 0;
    private int currentChestSlot = 0;
    private final List<BlockPos> searchedChests = new ArrayList<>();

    public SchematicBuilder() {
        super("SchematicBuilder", "Builds structures from schematic files layer by layer", Category.MISC);
        addSettings(
                schematicPath, loadSchematic, startBuilding, stopBuilding, pauseBuilding, resumeBuilding,
                setOrigin, originCoords, placeDelay, rotateSchematic, rotation, showProgress,
                autoGetBlocks, chestSearchRange, chestOpenDelay, itemTakeDelay
        );
        SchematicBuilderRenderer.register();
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(GameRenderListener.class, this);
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(GameRenderListener.class, this);
        isBuilding = false;
        currentLayerBlocks.clear();
        closeChest();
        resetChestState();
        stopWalking();
        isWalkingToBlock = false;
        walkTarget = null;
        currentPath.clear();
        pathIndex = 0;
        isBuildingPath = false;
        pathBuildTarget = null;
        temporaryBlocks.clear();
        stuckTicks = 0;
        lastPosition = null;
        pathBuildAttempts = 0;
        isPaused = false;
        countedBlocks.clear();
    }

    @Override
    public void onTick2() {
        if (mc.world == null || mc.player == null) return;

        handleSettings();

        // Handle chest interaction if needed
        if (autoGetBlocks.getValue() && isBuilding && neededItem != null) {
            handleChestInteraction();
            return; // Don't build while getting blocks from chest
        }

        // Handle building layer by layer
        if (isBuilding && chestState == ChestState.NONE) {
            handleBuilding();
        }
    }

    private void handleSettings() {
        // Load schematic
        if (loadSchematic.getValue()) {
            loadSchematic.setValue(false);
            loadSchematicFile();
        }

        // Set origin
        if (setOrigin.getValue()) {
            setOrigin.setValue(false);
            setOriginPosition();
        }

        // Start building
        if (startBuilding.getValue()) {
            startBuilding.setValue(false);
            if (schematicBlocks.isEmpty()) {
                ChatUtils.e("No schematic loaded!");
                return;
            }
            if (origin == null) {
                ChatUtils.e("No origin set!");
                return;
            }
            startBuilding();
        }

        // Stop building
        if (stopBuilding.getValue()) {
            stopBuilding.setValue(false);
            stopBuilding();
        }
    }

    public void setSelectedSchematic(String path) {
        schematicPath.setValue(path);
        ChatUtils.m("Selected schematic: " + new java.io.File(path).getName());
    }

    private void loadSchematicFile() {
        String path = schematicPath.getValue();
        if (path.isEmpty()) {
            ChatUtils.e("Please enter a schematic file path");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            ChatUtils.e("Schematic file not found: " + path);
            return;
        }

        try {
            NbtCompound nbt = null;

            // Try compressed first (most common)
            try {
                nbt = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
            } catch (Exception e) {
                // Try uncompressed
                try {
                    java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.FileInputStream(file));
                    net.minecraft.nbt.NbtElement element = NbtIo.read(dis, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                    dis.close();
                    if (element instanceof NbtCompound) {
                        nbt = (NbtCompound) element;
                    } else {
                        ChatUtils.e("NBT root is not a compound tag");
                        return;
                    }
                } catch (Exception e2) {
                    ChatUtils.e("Failed to read NBT file (tried compressed and uncompressed): " + e2.getMessage());
                    return;
                }
            }

            if (nbt == null) {
                ChatUtils.e("Failed to read NBT file");
                return;
            }

            // Debug: show available keys
            java.util.List<String> keys = new java.util.ArrayList<>(nbt.getKeys());
            ChatUtils.m("NBT keys found: " + String.join(", ", keys));

            // Check if schematic data is nested in a "Schematic" compound (common format)
            if (nbt.contains("Schematic", 10)) {
                nbt = nbt.getCompound("Schematic");
                keys = new java.util.ArrayList<>(nbt.getKeys());
                ChatUtils.m("Found Schematic compound. Keys: " + String.join(", ", keys));
            }

            // Check for Litematica format first (most common .schem/.litematic format)
            if (nbt.contains("Regions", 10)) {
                ChatUtils.m("Detected Litematica format");
                loadLitematicaSchematic(nbt);
            } else if (nbt.contains("palette") && nbt.contains("blocks")) {
                ChatUtils.m("Detected Minecraft structure format");
                loadStructureFormat(nbt);
            } else if (nbt.contains("Blocks") && (nbt.contains("Width") || nbt.contains("Data"))) {
                // Legacy schematic format (.schematic) - has Blocks and either Width/Height/Length or Data
                ChatUtils.m("Detected legacy schematic format");
                loadLegacySchematic(nbt);
            } else {
                ChatUtils.e("Unknown schematic format. Available keys: " + String.join(", ", keys));
                return;
            }

            if (schematicBlocks.isEmpty()) {
                ChatUtils.e("Schematic loaded but contains no blocks!");
                return;
            }

            // Normalize positions to start from (0,0,0) at minimum corner
            normalizeSchematicPositions();

            calculateRequiredBlocks();
            ChatUtils.m("Schematic loaded! Size: " + (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1) +
                    ", " + schematicBlocks.size() + " blocks");
        } catch (Exception e) {
            ChatUtils.e("Failed to load schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStructureFormat(NbtCompound nbt) {
        schematicBlocks.clear();
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;

        // Read palette (block states)
        NbtList palette = nbt.getList("palette", 10); // COMPOUND_TYPE
        BlockState[] blockStates = new BlockState[palette.size()];

        for (int i = 0; i < palette.size(); i++) {
            NbtCompound blockTag = palette.getCompound(i);
            String blockName = blockTag.getString("Name");
            Block block = Registries.BLOCK.get(Identifier.tryParse(blockName));
            if (block != null) {
                BlockState state = block.getDefaultState();

                // Apply properties if present
                if (blockTag.contains("Properties", 10)) {
                    NbtCompound properties = blockTag.getCompound("Properties");
                    state = parseBlockState(state, properties);
                }

                blockStates[i] = state;
            } else {
                blockStates[i] = Blocks.AIR.getDefaultState();
            }
        }

        // Read blocks
        NbtList blocks = nbt.getList("blocks", 10); // COMPOUND_TYPE

        for (int i = 0; i < blocks.size(); i++) {
            NbtCompound blockTag = blocks.getCompound(i);
            int[] pos = blockTag.getIntArray("pos");
            int stateIndex = blockTag.getInt("state");

            if (stateIndex >= 0 && stateIndex < blockStates.length) {
                BlockPos relativePos = new BlockPos(pos[0], pos[1], pos[2]);
                BlockState state = blockStates[stateIndex];

                if (state.getBlock() != Blocks.AIR) {
                    schematicBlocks.put(relativePos, state);
                    minY = Math.min(minY, relativePos.getY());
                    maxY = Math.max(maxY, relativePos.getY());
                    minX = Math.min(minX, relativePos.getX());
                    maxX = Math.max(maxX, relativePos.getX());
                    minZ = Math.min(minZ, relativePos.getZ());
                    maxZ = Math.max(maxZ, relativePos.getZ());
                }
            }
        }
    }

    private void loadLegacySchematic(NbtCompound nbt) {
        schematicBlocks.clear();
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;

        // Get dimensions - can be short or int
        int width = 0, height = 0, length = 0;
        if (nbt.contains("Width", 3)) { // INT
            width = nbt.getInt("Width");
            height = nbt.getInt("Height");
            length = nbt.getInt("Length");
        } else if (nbt.contains("Width", 2)) { // SHORT
            width = nbt.getShort("Width");
            height = nbt.getShort("Height");
            length = nbt.getShort("Length");
        } else {
            ChatUtils.e("Legacy schematic missing Width/Height/Length");
            return;
        }

        // Get block data - can be byte array or different format
        byte[] blocks = null;
        byte[] data = null;

        // Try to get Blocks array - check multiple possible types
        if (nbt.contains("Blocks")) {
            try {
                // Try as byte array first (most common)
                if (nbt.getType("Blocks") == 7) { // BYTE_ARRAY
                    blocks = nbt.getByteArray("Blocks");
                } else if (nbt.getType("Blocks") == 11) { // INT_ARRAY
                    // Some schematics use int array instead
                    int[] blocksInt = nbt.getIntArray("Blocks");
                    blocks = new byte[blocksInt.length];
                    for (int i = 0; i < blocksInt.length; i++) {
                        blocks[i] = (byte) (blocksInt[i] & 0xFF);
                    }
                } else {
                    ChatUtils.e("Blocks field has unexpected type: " + nbt.getType("Blocks"));
                    return;
                }
            } catch (Exception e) {
                ChatUtils.e("Error reading Blocks array: " + e.getMessage());
                return;
            }
        } else {
            ChatUtils.e("Legacy schematic missing Blocks array");
            return;
        }

        if (nbt.contains("Data")) {
            try {
                if (nbt.getType("Data") == 7) { // BYTE_ARRAY
                    data = nbt.getByteArray("Data");
                } else if (nbt.getType("Data") == 11) { // INT_ARRAY
                    int[] dataInt = nbt.getIntArray("Data");
                    data = new byte[dataInt.length];
                    for (int i = 0; i < dataInt.length; i++) {
                        data[i] = (byte) (dataInt[i] & 0xFF);
                    }
                }
            } catch (Exception e) {
                // Data is optional, so just log and continue
                ChatUtils.m("Could not read Data array (optional): " + e.getMessage());
            }
        }

        // Handle offset if present
        int offsetX = 0, offsetY = 0, offsetZ = 0;
        if (nbt.contains("Offset", 11)) { // INT_ARRAY
            int[] offset = nbt.getIntArray("Offset");
            if (offset.length >= 3) {
                offsetX = offset[0];
                offsetY = offset[1];
                offsetZ = offset[2];
            }
        }

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    if (index < blocks.length) {
                        int blockId = blocks[index] & 0xFF;
                        int blockData = (data != null && index < data.length) ? data[index] & 0xF : 0;

                        // Convert legacy block ID to modern block
                        Block block = convertLegacyBlock(blockId, blockData);
                        if (block != null && block != Blocks.AIR) {
                            BlockPos relativePos = new BlockPos(
                                    x + offsetX,
                                    y + offsetY,
                                    z + offsetZ
                            );
                            BlockState state = block.getDefaultState();
                            schematicBlocks.put(relativePos, state);
                            minY = Math.min(minY, relativePos.getY());
                            maxY = Math.max(maxY, relativePos.getY());
                            minX = Math.min(minX, relativePos.getX());
                            maxX = Math.max(maxX, relativePos.getX());
                            minZ = Math.min(minZ, relativePos.getZ());
                            maxZ = Math.max(maxZ, relativePos.getZ());
                        }
                    }
                }
            }
        }
    }

    private Block convertLegacyBlock(int id, int data) {
        // Expanded legacy block conversion (Minecraft 1.12 and earlier block IDs)
        return switch (id) {
            case 0 -> Blocks.AIR;
            case 1 -> Blocks.STONE;
            case 2 -> Blocks.GRASS_BLOCK;
            case 3 -> Blocks.DIRT;
            case 4 -> Blocks.COBBLESTONE;
            case 5 -> switch (data & 0xF) {
                case 0 -> Blocks.OAK_PLANKS;
                case 1 -> Blocks.SPRUCE_PLANKS;
                case 2 -> Blocks.BIRCH_PLANKS;
                case 3 -> Blocks.JUNGLE_PLANKS;
                case 4 -> Blocks.ACACIA_PLANKS;
                case 5 -> Blocks.DARK_OAK_PLANKS;
                default -> Blocks.OAK_PLANKS;
            };
            case 6 -> Blocks.OAK_SAPLING;
            case 7 -> Blocks.BEDROCK;
            case 8, 9 -> Blocks.WATER;
            case 10, 11 -> Blocks.LAVA;
            case 12 -> Blocks.SAND;
            case 13 -> Blocks.GRAVEL;
            case 14 -> Blocks.GOLD_ORE;
            case 15 -> Blocks.IRON_ORE;
            case 16 -> Blocks.COAL_ORE;
            case 17 -> switch (data & 0x3) {
                case 0 -> Blocks.OAK_LOG;
                case 1 -> Blocks.SPRUCE_LOG;
                case 2 -> Blocks.BIRCH_LOG;
                case 3 -> Blocks.JUNGLE_LOG;
                default -> Blocks.OAK_LOG;
            };
            case 18 -> switch (data & 0x3) {
                case 0 -> Blocks.OAK_LEAVES;
                case 1 -> Blocks.SPRUCE_LEAVES;
                case 2 -> Blocks.BIRCH_LEAVES;
                case 3 -> Blocks.JUNGLE_LEAVES;
                default -> Blocks.OAK_LEAVES;
            };
            case 20 -> Blocks.GLASS;
            case 24 -> Blocks.SANDSTONE;
            case 35 -> switch (data & 0xF) {
                case 0 -> Blocks.WHITE_WOOL;
                case 1 -> Blocks.ORANGE_WOOL;
                case 2 -> Blocks.MAGENTA_WOOL;
                case 3 -> Blocks.LIGHT_BLUE_WOOL;
                case 4 -> Blocks.YELLOW_WOOL;
                case 5 -> Blocks.LIME_WOOL;
                case 6 -> Blocks.PINK_WOOL;
                case 7 -> Blocks.GRAY_WOOL;
                case 8 -> Blocks.LIGHT_GRAY_WOOL;
                case 9 -> Blocks.CYAN_WOOL;
                case 10 -> Blocks.PURPLE_WOOL;
                case 11 -> Blocks.BLUE_WOOL;
                case 12 -> Blocks.BROWN_WOOL;
                case 13 -> Blocks.GREEN_WOOL;
                case 14 -> Blocks.RED_WOOL;
                case 15 -> Blocks.BLACK_WOOL;
                default -> Blocks.WHITE_WOOL;
            };
            case 41 -> Blocks.GOLD_BLOCK;
            case 42 -> Blocks.IRON_BLOCK;
            case 45 -> Blocks.BRICKS;
            case 46 -> Blocks.TNT;
            case 47 -> Blocks.BOOKSHELF;
            case 48 -> Blocks.MOSSY_COBBLESTONE;
            case 49 -> Blocks.OBSIDIAN;
            case 50 -> Blocks.TORCH;
            case 53 -> Blocks.OAK_STAIRS;
            case 54 -> Blocks.CHEST;
            case 56 -> Blocks.DIAMOND_ORE;
            case 57 -> Blocks.DIAMOND_BLOCK;
            case 58 -> Blocks.CRAFTING_TABLE;
            case 60 -> Blocks.FARMLAND;
            case 61 -> Blocks.FURNACE;
            case 64 -> Blocks.OAK_DOOR;
            case 65 -> Blocks.LADDER;
            case 67 -> Blocks.COBBLESTONE_STAIRS;
            case 73 -> Blocks.REDSTONE_ORE;
            case 78 -> Blocks.SNOW;
            case 79 -> Blocks.ICE;
            case 80 -> Blocks.SNOW_BLOCK;
            case 81 -> Blocks.CACTUS;
            case 82 -> Blocks.CLAY;
            case 85 -> Blocks.OAK_FENCE;
            case 86 -> Blocks.PUMPKIN;
            case 87 -> Blocks.NETHERRACK;
            case 88 -> Blocks.SOUL_SAND;
            case 89 -> Blocks.GLOWSTONE;
            case 98 -> Blocks.STONE_BRICKS;
            case 102 -> Blocks.GLASS_PANE;
            case 103 -> Blocks.MELON;
            case 107 -> Blocks.OAK_FENCE_GATE;
            case 112 -> Blocks.NETHER_BRICKS;
            case 121 -> Blocks.END_STONE;
            case 123 -> Blocks.REDSTONE_LAMP;
            case 125 -> Blocks.OAK_SLAB;
            case 126 -> Blocks.OAK_SLAB;
            case 133 -> Blocks.EMERALD_BLOCK;
            case 134 -> Blocks.SPRUCE_STAIRS;
            case 135 -> Blocks.BIRCH_STAIRS;
            case 139 -> Blocks.COBBLESTONE_WALL;
            case 155 -> Blocks.QUARTZ_BLOCK;
            case 159 -> switch (data & 0xF) {
                case 0 -> Blocks.WHITE_TERRACOTTA;
                case 1 -> Blocks.ORANGE_TERRACOTTA;
                case 2 -> Blocks.MAGENTA_TERRACOTTA;
                case 3 -> Blocks.LIGHT_BLUE_TERRACOTTA;
                case 4 -> Blocks.YELLOW_TERRACOTTA;
                case 5 -> Blocks.LIME_TERRACOTTA;
                case 6 -> Blocks.PINK_TERRACOTTA;
                case 7 -> Blocks.GRAY_TERRACOTTA;
                case 8 -> Blocks.LIGHT_GRAY_TERRACOTTA;
                case 9 -> Blocks.CYAN_TERRACOTTA;
                case 10 -> Blocks.PURPLE_TERRACOTTA;
                case 11 -> Blocks.BLUE_TERRACOTTA;
                case 12 -> Blocks.BROWN_TERRACOTTA;
                case 13 -> Blocks.GREEN_TERRACOTTA;
                case 14 -> Blocks.RED_TERRACOTTA;
                case 15 -> Blocks.BLACK_TERRACOTTA;
                default -> Blocks.WHITE_TERRACOTTA;
            };
            default -> {
                // Try to find block by legacy name if available
                ChatUtils.m("Unknown legacy block ID: " + id + " (data: " + data + "), using AIR");
                yield Blocks.AIR;
            }
        };
    }

    private void loadLitematicaSchematic(NbtCompound nbt) {
        schematicBlocks.clear();
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;

        if (!nbt.contains("Regions", 10)) {
            ChatUtils.e("Invalid Litematica schematic: missing Regions");
            return;
        }

        NbtCompound regions = nbt.getCompound("Regions");

        if (regions.getKeys().isEmpty()) {
            ChatUtils.e("Invalid Litematica schematic: Regions compound is empty");
            return;
        }

        // Process each region in the schematic
        for (String regionName : regions.getKeys()) {
            NbtCompound region = regions.getCompound(regionName);

            if (!region.contains("BlockStates", 7)) { // 7 = LONG_ARRAY
                ChatUtils.e("Region " + regionName + " missing BlockStates");
                continue;
            }

            if (!region.contains("BlockStatePalette", 9)) { // 9 = LIST
                ChatUtils.e("Region " + regionName + " missing BlockStatePalette");
                continue;
            }

            // Get region size and position (Position is optional, defaults to 0,0,0)
            int[] size = region.getIntArray("Size");
            int[] position = region.getIntArray("Position");

            if (size.length < 3) {
                ChatUtils.e("Region " + regionName + " has invalid Size array");
                continue;
            }

            int width = size[0];
            int height = size[1];
            int length = size[2];
            int offsetX = position.length >= 1 ? position[0] : 0;
            int offsetY = position.length >= 2 ? position[1] : 0;
            int offsetZ = position.length >= 3 ? position[2] : 0;

            // Read block state palette
            NbtList palette = region.getList("BlockStatePalette", 10); // COMPOUND_TYPE
            BlockState[] blockStates = new BlockState[palette.size()];

            for (int i = 0; i < palette.size(); i++) {
                NbtCompound blockTag = palette.getCompound(i);
                String blockName = blockTag.getString("Name");
                Block block = Registries.BLOCK.get(Identifier.tryParse(blockName));
                if (block != null) {
                    BlockState state = block.getDefaultState();

                    // Apply properties if present
                    if (blockTag.contains("Properties", 10)) {
                        NbtCompound properties = blockTag.getCompound("Properties");
                        state = parseBlockState(state, properties);
                    }

                    blockStates[i] = state;
                } else {
                    blockStates[i] = Blocks.AIR.getDefaultState();
                }
            }

            // Read block states array (packed long array)
            long[] blockStatesArray = region.getLongArray("BlockStates");
            int totalBlocks = width * height * length;

            // Calculate bits per block - must be able to represent all palette indices
            int bitsPerBlock = Math.max(1, (int) Math.ceil(Math.log(Math.max(1, palette.size())) / Math.log(2)));
            // Ensure bitsPerBlock is at least 4 (common minimum)
            if (bitsPerBlock < 4 && palette.size() > 1) {
                bitsPerBlock = 4;
            }
            int blocksPerLong = 64 / bitsPerBlock;
            long mask = (1L << bitsPerBlock) - 1;

            ChatUtils.m("Decoding region " + regionName + ": " + width + "x" + height + "x" + length +
                    ", palette size: " + palette.size() + ", bits per block: " + bitsPerBlock);

            // Decode block states - iterate through all positions
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int index = (y * length + z) * width + x;

                        if (index >= totalBlocks) continue;

                        int longIndex = index / blocksPerLong;
                        int bitOffset = (index % blocksPerLong) * bitsPerBlock;

                        if (longIndex < blockStatesArray.length) {
                            long longValue = blockStatesArray[longIndex];
                            long value = (longValue >>> bitOffset) & mask;

                            if (value >= 0 && value < blockStates.length) {
                                BlockState state = blockStates[(int) value];

                                // Include ALL blocks, not just non-air (for debugging)
                                BlockPos relativePos = new BlockPos(
                                        x + offsetX,
                                        y + offsetY,
                                        z + offsetZ
                                );

                                // Only store non-air blocks
                                if (state.getBlock() != Blocks.AIR) {
                                    schematicBlocks.put(relativePos, state);
                                    minY = Math.min(minY, relativePos.getY());
                                    maxY = Math.max(maxY, relativePos.getY());
                                    minX = Math.min(minX, relativePos.getX());
                                    maxX = Math.max(maxX, relativePos.getX());
                                    minZ = Math.min(minZ, relativePos.getZ());
                                    maxZ = Math.max(maxZ, relativePos.getZ());
                                }
                            }
                        }
                    }
                }
            }

            ChatUtils.m("Loaded " + schematicBlocks.size() + " blocks from region " + regionName);
        }
    }

    private BlockState parseBlockState(BlockState state, NbtCompound properties) {
        // Apply block properties from NBT
        // Note: Full property parsing requires complex type handling
        // For now, we use the default state - this can be enhanced later
        // to properly parse properties like facing, rotation, etc.
        // The block state from the palette should already have correct properties
        // from the schematic file, so we return it as-is
        return state;
    }

    private void normalizeSchematicPositions() {
        if (schematicBlocks.isEmpty()) return;

        // Calculate the minimum corner
        int offsetX = minX;
        int offsetY = minY;
        int offsetZ = minZ;

        // If already normalized, skip
        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) {
            return;
        }

        // Create new map with normalized positions
        Map<BlockPos, BlockState> normalizedBlocks = new HashMap<>();
        BlockPos offset = new BlockPos(offsetX, offsetY, offsetZ);

        for (Map.Entry<BlockPos, BlockState> entry : schematicBlocks.entrySet()) {
            BlockPos oldPos = entry.getKey();
            BlockPos newPos = oldPos.subtract(offset);
            normalizedBlocks.put(newPos, entry.getValue());
        }

        schematicBlocks = normalizedBlocks;

        // Update bounds
        maxX -= offsetX;
        maxY -= offsetY;
        maxZ -= offsetZ;
        minX = 0;
        minY = 0;
        minZ = 0;

        ChatUtils.m("Normalized schematic positions. Offset: (" + offsetX + ", " + offsetY + ", " + offsetZ + ")");
    }

    private void calculateRequiredBlocks() {
        requiredBlocks.clear();
        for (BlockState state : schematicBlocks.values()) {
            Block block = state.getBlock();
            Item item = block.asItem();
            if (item != null) {
                requiredBlocks.put(item, requiredBlocks.getOrDefault(item, 0) + 1);
            }
        }
    }

    private void setOriginPosition() {
        if (mc.crosshairTarget != null &&
                mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
            origin = hit.getBlockPos();
            originCoords.setValue(origin.getX() + "," + origin.getY() + "," + origin.getZ());
            ChatUtils.m("Origin set to " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ());
        } else {
            BlockPos parsed = parseCoordinates(originCoords.getValue());
            if (parsed != null) {
                origin = parsed;
                ChatUtils.m("Origin set to " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ());
            } else {
                ChatUtils.e("Invalid coordinates or look at a block");
            }
        }
    }

    private BlockPos parseCoordinates(String coords) {
        try {
            String[] parts = coords.trim().split(",");
            if (parts.length != 3) return null;
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            return new BlockPos(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private void startBuilding() {
        if (schematicBlocks.isEmpty() || origin == null) return;

        isBuilding = true;
        isPaused = false;
        currentLayer = minY;
        currentLayerBlocks.clear();
        currentLayerIndex = 0;
        searchedChests.clear();
        calculateRequiredBlocks();

        // Initialize progress tracking
        totalBlocks = schematicBlocks.size();
        blocksPlaced = 0;
        buildStartTime = System.currentTimeMillis();
        lastPlaceTime = buildStartTime;
        recentPlaceTimes.clear();
        blocksPerSecond = 0.0;

        // Initialize first layer
        loadLayer(currentLayer);

        ChatUtils.m("Started building! " + (maxY - minY + 1) + " layers, " + totalBlocks + " blocks total");
    }

    private void pauseBuilding() {
        if (!isBuilding) return;

        isPaused = true;
        stopWalking();
        ChatUtils.m("Building paused. Progress: " + getProgressPercentage() + "% (" + blocksPlaced + "/" + totalBlocks + " blocks)");
    }

    private void resumeBuilding() {
        if (!isBuilding || !isPaused) return;

        isPaused = false;
        ChatUtils.m("Building resumed. Progress: " + getProgressPercentage() + "%");
    }

    private void loadLayer(int layerY) {
        currentLayerBlocks.clear();
        currentLayerIndex = 0;

        for (Map.Entry<BlockPos, BlockState> entry : schematicBlocks.entrySet()) {
            if (entry.getKey().getY() == layerY) {
                BlockPos relativePos = entry.getKey();
                BlockState blockState = entry.getValue();

                // Apply rotation if needed
                if (rotateSchematic.getValue()) {
                    relativePos = rotatePosition(relativePos, rotation.getValue().intValue());
                }

                BlockPos worldPos = origin.add(relativePos);
                currentLayerBlocks.add(new BlockPlaceTask(worldPos, blockState));
            }
        }

        // Sort by distance from player for better building order
        if (mc.player != null) {
            BlockPos playerPos = mc.player.getBlockPos();
            currentLayerBlocks.sort((a, b) -> {
                double distA = playerPos.getSquaredDistance(a.pos);
                double distB = playerPos.getSquaredDistance(b.pos);
                return Double.compare(distA, distB);
            });
        }
    }

    private BlockPos rotatePosition(BlockPos pos, int rotation) {
        int x = pos.getX();
        int z = pos.getZ();

        switch (rotation) {
            case 1: // 90° clockwise
                return new BlockPos(-z, pos.getY(), x);
            case 2: // 180°
                return new BlockPos(-x, pos.getY(), -z);
            case 3: // 270° clockwise
                return new BlockPos(z, pos.getY(), -x);
            default:
                return pos;
        }
    }

    private void stopBuilding() {
        isBuilding = false;
        currentLayerBlocks.clear();
        closeChest();
        resetChestState();
        ChatUtils.m("Building stopped");
    }

    private void handleBuilding() {
        if (currentLayer > maxY) {
            ChatUtils.m("Building complete!");
            isBuilding = false;
            return;
        }

        // Check if current layer is done - verify all blocks are actually placed
        if (currentLayerIndex >= currentLayerBlocks.size()) {
            // Verify all blocks in current layer are placed before moving to next
            // But allow some tolerance - if most blocks are placed, move on
            int placedCount = 0;
            int totalInLayer = currentLayerBlocks.size();

            for (BlockPlaceTask task : currentLayerBlocks) {
                BlockState currentState = mc.world.getBlockState(task.pos);
                if (currentState.equals(task.blockState)) {
                    placedCount++;
                }
            }

            // Consider layer complete if 95% or more blocks are placed
            // This prevents getting stuck on unreachable blocks
            boolean layerComplete = placedCount >= totalInLayer * 0.95 || placedCount == totalInLayer;

            if (layerComplete) {
                // Move to next layer
                currentLayer++;
                if (currentLayer <= maxY) {
                    loadLayer(currentLayer);
                    ChatUtils.m("Building layer " + (currentLayer - minY + 1) + "/" + (maxY - minY + 1) +
                            " (" + placedCount + "/" + totalInLayer + " blocks placed)");
                } else {
                    // All layers complete
                    isBuilding = false;
                    ChatUtils.m("Finished building all layers!");
                }
            } else {
                // Reload current layer to retry unplaced blocks
                // But skip blocks that are clearly unreachable after multiple attempts
                loadLayer(currentLayer);
                ChatUtils.m("Retrying layer " + (currentLayer - minY + 1) + " (" + placedCount + "/" + totalInLayer + " placed)");
            }
            return;
        }

        // Handle pending placement after swap
        if (waitingForSwap && pendingPlacement != null) {
            // Rotate to face the placement position
            Vec3d targetVec = pendingPlacement.getPos();
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), targetVec);
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(MathHelper.clamp(rotations[1], -90.0f, 90.0f));

            // Place the block
            BlockUtil.interactWithBlock(pendingPlacement, true);

            // Update progress
            blocksPlaced++;
            long currentTime = System.currentTimeMillis();
            lastPlaceTime = currentTime;
            recentPlaceTimes.add(currentTime);

            // Keep only last 20 place times for BPS calculation
            if (recentPlaceTimes.size() > 20) {
                recentPlaceTimes.remove(0);
            }

            // Reset state
            waitingForSwap = false;
            pendingPlacement = null;

            // Advance to next block
            currentLayerIndex++;
            placeDelayCounter = placeDelay.getValue().intValue();
            return;
        }

        // Delay between placements
        if (placeDelayCounter > 0) {
            placeDelayCounter--;
            return;
        }

        BlockPlaceTask task = currentLayerBlocks.get(currentLayerIndex);
        if (task == null) return;

        BlockPos targetPos = task.pos;
        BlockState targetState = task.blockState;
        Block targetBlock = targetState.getBlock();

        // Check if already placed correctly
        BlockState currentState = mc.world.getBlockState(targetPos);
        if (currentState.equals(targetState)) {
            // Already placed, count it as progress
            if (!isBlockCounted(targetPos)) {
                blocksPlaced++;
                markBlockAsCounted(targetPos);
            }
            currentLayerIndex++;
            isWalkingToBlock = false;
            walkTarget = null;
            return;
        }

        // Check distance to target block - use eye position for vertical reach
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetCenter = Vec3d.ofCenter(targetPos);
        double distance = playerPos.distanceTo(targetCenter);

        // For blocks above player, allow slightly more reach (player can reach up)
        double maxReachForBlock = MAX_REACH;
        if (targetPos.getY() > mc.player.getBlockY() + 1) {
            maxReachForBlock = MAX_REACH + 1.5; // Extra reach for upward placement
        }

        // If too far, walk closer
        if (distance > maxReachForBlock) {
            if (!isWalkingToBlock || walkTarget == null || !walkTarget.equals(targetPos)) {
                isWalkingToBlock = true;
                walkTarget = targetPos;
            }
            walkToBlock(targetPos);
            return; // Don't try to place until we're close enough
        } else {
            // Close enough, stop walking
            if (isWalkingToBlock) {
                stopWalking();
                isWalkingToBlock = false;
                walkTarget = null;
            }
        }

        // Find block in inventory
        Item blockItem = targetBlock.asItem();
        if (blockItem == null) {
            currentLayerIndex++; // Skip if no item form
            return;
        }

        // Check if we have the block
        if (!hasBlockInInventory(blockItem)) {
            // Try to get from chest if enabled
            if (autoGetBlocks.getValue()) {
                neededItem = blockItem;
                chestState = ChestState.SEARCHING;
                searchedChests.clear();
                return; // Don't advance, will retry after getting blocks
            } else {
                ChatUtils.e("Missing block: " + targetBlock.getName().getString());
                currentLayerIndex++;
                return;
            }
        }

        // Find placement side
        BlockHitResult hitResult = findPlacementSide(targetPos);
        if (hitResult == null) {
            currentLayerIndex++; // Skip if can't place
            return;
        }

        // Check if placement position is within reach (use eye position)
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d placementPos = hitResult.getPos();
        double placementDistance = eyePos.distanceTo(placementPos);

        // For upward placement, allow more reach
        double maxReachForPlacement = MAX_REACH;
        if (hitResult.getBlockPos().getY() > mc.player.getBlockY() + 1) {
            maxReachForPlacement = MAX_REACH + 1.5;
        }

        if (placementDistance > maxReachForPlacement) {
            // Need to get closer to placement position
            if (!isWalkingToBlock || walkTarget == null || !walkTarget.equals(hitResult.getBlockPos())) {
                isWalkingToBlock = true;
                walkTarget = hitResult.getBlockPos();
            }
            walkToBlock(hitResult.getBlockPos());
            return;
        }

        // Swap to the block and prepare placement
        if (InventoryUtil.swap(blockItem)) {
            waitingForSwap = true;
            pendingPlacement = hitResult;
        } else {
            ChatUtils.e("Could not swap to block: " + targetBlock.getName().getString());
            currentLayerIndex++;
        }
    }

    private void handleChestInteraction() {
        switch (chestState) {
            case NONE -> {
                // Check if we need items
                if (neededItem == null) return;
                if (hasBlockInInventory(neededItem)) {
                    neededItem = null;
                    return;
                }
                chestState = ChestState.SEARCHING;
                searchedChests.clear();
            }

            case SEARCHING -> {
                targetChest = findNearestChest();
                if (targetChest == null) {
                    ChatUtils.e("No chest found with " + neededItem.getName().getString());
                    neededItem = null;
                    chestState = ChestState.NONE;
                    return;
                }
                chestState = ChestState.OPENING;
                chestOpenDelayCounter = chestOpenDelay.getValue().intValue();
            }

            case OPENING -> {
                if (chestOpenDelayCounter > 0) {
                    chestOpenDelayCounter--;
                    return;
                }

                if (targetChest == null) {
                    chestState = ChestState.NONE;
                    return;
                }

                // Check if already open
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    chestState = ChestState.TAKING_ITEMS;
                    currentChestSlot = 0;
                    itemTakeDelayCounter = 0;
                    return;
                }

                // Open chest
                openChest(targetChest);
                chestOpenDelayCounter = 5; // Wait for GUI to open
            }

            case TAKING_ITEMS -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    // Chest closed unexpectedly
                    chestState = ChestState.NONE;
                    targetChest = null;
                    return;
                }

                if (itemTakeDelayCounter > 0) {
                    itemTakeDelayCounter--;
                    return;
                }

                ScreenHandler handler = screen.getScreenHandler();
                int containerSlots = handler.slots.size() - 36;

                // Search through chest slots
                boolean foundItem = false;
                for (int i = currentChestSlot; i < containerSlots; i++) {
                    Slot slot = handler.slots.get(i);
                    if (slot.hasStack() && slot.getStack().getItem() == neededItem) {
                        // Take the item
                        mc.interactionManager.clickSlot(
                                handler.syncId,
                                i,
                                0,
                                SlotActionType.PICKUP,
                                mc.player
                        );

                        foundItem = true;
                        itemTakeDelayCounter = itemTakeDelay.getValue().intValue();
                        currentChestSlot = i + 1;

                        ChatUtils.m("Taking " + neededItem.getName().getString() + " from chest");
                        break;
                    }
                }

                if (!foundItem) {
                    // No more items in this chest, close and search for another
                    closeChest();
                    searchedChests.add(targetChest);
                    targetChest = null;
                    chestState = ChestState.SEARCHING;
                    currentChestSlot = 0;
                } else if (hasBlockInInventory(neededItem)) {
                    // Got the item, close chest and continue building
                    closeChest();
                    neededItem = null;
                    chestState = ChestState.NONE;
                    targetChest = null;
                    currentChestSlot = 0;
                }
            }
        }
    }

    private BlockPos findNearestChest() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        double range = chestSearchRange.getValue();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        int rangeInt = (int) Math.ceil(range);
        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();

                    // Check for chests, ender chests, barrels, shulkers
                    if (isStorageBlock(block)) {
                        // Skip if already searched
                        if (searchedChests.contains(checkPos)) continue;

                        double dist = playerPos.getSquaredDistance(checkPos);
                        if (dist <= range * range && dist < nearestDist) {
                            nearest = checkPos;
                            nearestDist = dist;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private boolean isStorageBlock(Block block) {
        return block == Blocks.CHEST || block == Blocks.ENDER_CHEST ||
                block == Blocks.BARREL || block == Blocks.SHULKER_BOX ||
                block == Blocks.WHITE_SHULKER_BOX || block == Blocks.ORANGE_SHULKER_BOX ||
                block == Blocks.MAGENTA_SHULKER_BOX || block == Blocks.LIGHT_BLUE_SHULKER_BOX ||
                block == Blocks.YELLOW_SHULKER_BOX || block == Blocks.LIME_SHULKER_BOX ||
                block == Blocks.PINK_SHULKER_BOX || block == Blocks.GRAY_SHULKER_BOX ||
                block == Blocks.LIGHT_GRAY_SHULKER_BOX || block == Blocks.CYAN_SHULKER_BOX ||
                block == Blocks.PURPLE_SHULKER_BOX || block == Blocks.BLUE_SHULKER_BOX ||
                block == Blocks.BROWN_SHULKER_BOX || block == Blocks.GREEN_SHULKER_BOX ||
                block == Blocks.RED_SHULKER_BOX || block == Blocks.BLACK_SHULKER_BOX;
    }

    private void openChest(BlockPos chestPos) {
        if (mc.player == null || mc.interactionManager == null) return;

        Vec3d targetVec = Vec3d.ofCenter(chestPos);
        BlockHitResult hitResult = new BlockHitResult(
                targetVec,
                Direction.UP,
                chestPos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        ChatUtils.m("Opening chest at " + chestPos.toShortString());
    }

    private void closeChest() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.closeHandledScreen();
                }
            });
        }
    }

    private void resetChestState() {
        chestState = ChestState.NONE;
        targetChest = null;
        neededItem = null;
        chestOpenDelayCounter = 0;
        itemTakeDelayCounter = 0;
        currentChestSlot = 0;
    }

    private boolean hasBlockInInventory(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item && stack.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private void walkToBlock(BlockPos targetPos) {
        if (mc.player == null || mc.world == null) return;

        // Check if stuck
        BlockPos currentPos = mc.player.getBlockPos();
        if (lastPosition != null && lastPosition.equals(currentPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPosition = currentPos;
        }

        // If stuck, try to recover
        if (stuckTicks > STUCK_THRESHOLD) {
            handleStuck(targetPos);
            return;
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetCenter = Vec3d.ofCenter(targetPos);
        double distance = playerPos.distanceTo(targetCenter);

        // If close enough, use direct movement
        if (distance <= MAX_REACH + 1.0) {
            walkDirectlyTo(targetPos);
            return;
        }

        // If far away, use pathfinding
        if (currentPath.isEmpty() || pathIndex >= currentPath.size() ||
                !currentPath.get(currentPath.size() - 1).equals(targetPos)) {
            // Calculate new path
            BlockPos start = mc.player.getBlockPos();
            List<BlockPos> path = findPath(start, targetPos);
            if (path != null && !path.isEmpty()) {
                currentPath = path;
                pathIndex = 0;
                stuckTicks = 0; // Reset stuck counter on new path
            } else {
                // Pathfinding failed, fall back to direct movement
                walkDirectlyTo(targetPos);
                return;
            }
        }

        // Follow path - skip unreachable waypoints
        while (pathIndex < currentPath.size()) {
            BlockPos nextPos = currentPath.get(pathIndex);
            Vec3d nextCenter = Vec3d.ofCenter(nextPos);
            double distToNext = playerPos.distanceTo(nextCenter);

            // If waypoint is too far or unreachable, skip it
            if (distToNext > MAX_REACH * 2 && pathIndex < currentPath.size() - 1) {
                pathIndex++;
                continue;
            }

            if (distToNext < 0.5) {
                pathIndex++; // Reached this waypoint, move to next
            } else {
                walkDirectlyTo(nextPos);
                break;
            }
        }

        // If we've gone through all waypoints, walk directly to target
        if (pathIndex >= currentPath.size()) {
            walkDirectlyTo(targetPos);
        }
    }

    private void handleStuck(BlockPos targetPos) {
        ChatUtils.m("Detected stuck! Attempting recovery...");

        // Reset pathfinding state
        currentPath.clear();
        pathIndex = 0;
        stuckTicks = 0;
        isBuildingPath = false;
        pathBuildTarget = null;
        pathBuildAttempts = 0;

        // Try jumping to get unstuck
        if (mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
        }

        // Try to recalculate path from current position
        BlockPos start = mc.player.getBlockPos();
        List<BlockPos> newPath = findPath(start, targetPos);

        if (newPath != null && !newPath.isEmpty()) {
            currentPath = newPath;
            pathIndex = 0;
            ChatUtils.m("Recalculated path with " + newPath.size() + " waypoints");
        } else {
            // If pathfinding fails, try direct movement with building
            ChatUtils.m("Pathfinding failed, using direct movement");
            walkDirectlyTo(targetPos);
        }
    }

    private void walkDirectlyTo(BlockPos targetPos) {
        if (mc.player == null || mc.world == null) return;

        // Check if we need to build a block first
        if (isBuildingPath && pathBuildTarget != null) {
            pathBuildAttempts++;

            // If we've tried too many times, give up on this block
            if (pathBuildAttempts > MAX_BUILD_ATTEMPTS) {
                ChatUtils.m("Failed to build path block after " + MAX_BUILD_ATTEMPTS + " attempts, skipping");
                isBuildingPath = false;
                pathBuildTarget = null;
                pathBuildAttempts = 0;
                // Try to find alternative path
                currentPath.clear();
                pathIndex = 0;
                return;
            }

            if (!buildPathBlock(pathBuildTarget)) {
                // Still building, wait
                return;
            }
            // Building complete, continue
            isBuildingPath = false;
            pathBuildTarget = null;
            pathBuildAttempts = 0;
        }

        // Check if we need to build a block to walk on
        BlockPos playerBlock = mc.player.getBlockPos();
        BlockPos nextStep = getNextStepPosition(playerBlock, targetPos);

        if (nextStep != null && !nextStep.equals(playerBlock)) {
            // Need to build a block to continue
            if (shouldBuildPathBlock(nextStep)) {
                isBuildingPath = true;
                pathBuildTarget = nextStep;
                pathBuildAttempts = 0;
                return;
            } else {
                // Can't build here, try to find alternative
                Vec3d playerPos = mc.player.getPos();
                double distToTarget = playerPos.distanceTo(Vec3d.ofCenter(targetPos));

                // If target is close, try jumping over
                if (distToTarget < MAX_REACH * 2) {
                    checkAndJump();
                } else {
                    // Too far, recalculate path
                    currentPath.clear();
                    pathIndex = 0;
                }
            }
        }

        // Check if we need to jump
        checkAndJump();

        Vec3d playerPos = mc.player.getPos();
        double targetX = targetPos.getX() + 0.5;
        double targetZ = targetPos.getZ() + 0.5;

        double offsetX = playerPos.x - targetX;
        double offsetZ = playerPos.z - targetZ;

        // Stop all movement first
        stopWalking();

        // Calculate movement based on player's yaw
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);

        double moveX = -offsetX;
        double moveZ = -offsetZ;

        // Convert to relative movement
        double relativeForward = moveX * (-Math.sin(yawRad)) + moveZ * Math.cos(yawRad);
        double relativeStrafe = moveX * (-Math.cos(yawRad)) + moveZ * (-Math.sin(yawRad));

        // Apply movement
        if (Math.abs(relativeForward) > 0.1) {
            if (relativeForward > 0) {
                mc.options.forwardKey.setPressed(true);
            } else {
                mc.options.backKey.setPressed(true);
            }
        }

        if (Math.abs(relativeStrafe) > 0.1) {
            if (relativeStrafe > 0) {
                mc.options.rightKey.setPressed(true);
            } else {
                mc.options.leftKey.setPressed(true);
            }
        }
    }

    private BlockPos getNextStepPosition(BlockPos current, BlockPos target) {
        // Get the next block position we need to step on
        int dx = Integer.compare(target.getX(), current.getX());
        int dz = Integer.compare(target.getZ(), current.getZ());

        if (dx == 0 && dz == 0) return null;

        BlockPos next = current.add(dx, 0, dz);

        // Check if we need to go up or down
        BlockState nextState = mc.world.getBlockState(next);
        BlockState nextBelow = mc.world.getBlockState(next.down());
        BlockState nextAbove = mc.world.getBlockState(next.up());

        // If next position is blocked, check if we can go up
        if (!nextState.isAir() && !nextState.isReplaceable()) {
            // Check if we can step up
            if (nextAbove.isAir() && !nextState.getBlock().getDefaultState().isAir()) {
                return next.up();
            }
            return null; // Can't go there
        }

        // If next position has no floor, we need to build down
        if (nextBelow.isAir() || nextBelow.isReplaceable()) {
            return next.down();
        }

        return next;
    }

    private boolean shouldBuildPathBlock(BlockPos pos) {
        if (mc.world == null) return false;

        // Don't build if it's part of the schematic
        if (origin != null && schematicBlocks.containsKey(pos.subtract(origin))) {
            return false;
        }

        // Don't build if already built
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) {
            return false;
        }

        // Check if we have blocks to build with
        Item scaffoldItem = findScaffoldItem();
        if (scaffoldItem == null) {
            return false; // No blocks available
        }

        // Check if it's a reasonable position (not too far)
        double dist = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
        return dist <= MAX_REACH + 2.0;
    }

    private boolean buildPathBlock(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;

        // Check if already built
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) {
            temporaryBlocks.add(pos);
            return true; // Already built
        }

        // Check distance - don't build if too far
        Vec3d playerPos = mc.player.getPos();
        double dist = playerPos.distanceTo(Vec3d.ofCenter(pos));
        if (dist > MAX_REACH + 1.0) {
            // Too far, need to get closer first
            return false;
        }

        // Find scaffold item
        Item scaffoldItem = findScaffoldItem();
        if (scaffoldItem == null) {
            return false; // No blocks available
        }

        // Swap to scaffold item
        if (!InventoryUtil.swap(scaffoldItem)) {
            return false;
        }

        // Small delay to ensure swap completes
        if (pathBuildAttempts == 1) {
            return false; // Wait one tick for swap
        }

        // Find placement side
        BlockHitResult hitResult = findPlacementSide(pos);
        if (hitResult == null) {
            // Try placing from below
            BlockPos below = pos.down();
            BlockState belowState = mc.world.getBlockState(below);
            if (!belowState.isAir() && !belowState.isReplaceable()) {
                Vec3d hitPos = Vec3d.ofCenter(below);
                hitResult = new BlockHitResult(hitPos, Direction.UP, below, false);
            } else {
                // Can't place here
                return false;
            }
        }

        // Rotate and place
        Vec3d targetVec = hitResult.getPos();
        float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), targetVec);
        mc.player.setYaw(rotations[0]);
        mc.player.setPitch(MathHelper.clamp(rotations[1], -90.0f, 90.0f));

        BlockUtil.interactWithBlock(hitResult, true);

        // Check if block was placed (wait a tick)
        if (pathBuildAttempts >= 2) {
            BlockState newState = mc.world.getBlockState(pos);
            if (!newState.isAir() && !newState.isReplaceable()) {
                temporaryBlocks.add(pos);
                return true; // Successfully built
            }
        }

        return false; // Still building
    }

    private Item findScaffoldItem() {
        // Prefer cheap blocks like dirt, cobblestone, or any block item
        Item[] preferred = {Items.DIRT, Items.COBBLESTONE, Items.STONE, Items.OAK_PLANKS};

        for (Item item : preferred) {
            if (hasBlockInInventory(item)) {
                return item;
            }
        }

        // Fall back to any block in inventory
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
                return stack.getItem();
            }
        }

        return null;
    }

    private void checkAndJump() {
        if (mc.player == null || mc.world == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos forward = playerPos.offset(Direction.fromRotation(mc.player.getYaw()));

        // Check if there's a 1-block obstacle we can jump over
        BlockState forwardState = mc.world.getBlockState(forward);
        BlockState forwardUp = mc.world.getBlockState(forward.up());
        BlockState forwardUp2 = mc.world.getBlockState(forward.up(2));
        BlockState forwardUp3 = mc.world.getBlockState(forward.up(3));

        // If there's a block in front that's 1 block high, jump
        if (!forwardState.isAir() && !forwardState.isReplaceable()) {
            if (forwardUp.isAir() && forwardUp2.isAir() && forwardUp3.isAir()) {
                // 1-block obstacle, can jump over
                if (mc.player.isOnGround() || mc.player.getVelocity().y > 0) {
                    mc.options.jumpKey.setPressed(true);
                }
            }
        }

        // Also check if we're walking into a step-up situation
        BlockState currentBelow = mc.world.getBlockState(playerPos.down());
        BlockState forwardBelow = mc.world.getBlockState(forward.down());

        if (!currentBelow.isAir() && !currentBelow.isReplaceable() &&
                !forwardBelow.isAir() && !forwardBelow.isReplaceable()) {
            // Check if forward is 1 block higher
            if (forward.getY() > playerPos.getY() && forwardUp.isAir() && forwardUp2.isAir()) {
                if (mc.player.isOnGround()) {
                    mc.options.jumpKey.setPressed(true);
                }
            }
        }
    }

    private List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        if (mc.world == null) return null;

        // Simple A* pathfinding
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> gScore = new HashMap<>();
        Map<BlockPos, Double> fScore = new HashMap<>();

        PriorityQueue<BlockPos> openSet = new PriorityQueue<>((a, b) -> {
            double fA = fScore.getOrDefault(a, Double.MAX_VALUE);
            double fB = fScore.getOrDefault(b, Double.MAX_VALUE);
            return Double.compare(fA, fB);
        });

        gScore.put(start, 0.0);
        fScore.put(start, heuristic(start, goal));
        openSet.add(start);

        int iterations = 0;
        while (!openSet.isEmpty() && iterations < 1000) {
            iterations++;
            BlockPos current = openSet.poll();

            if (current.equals(goal) || current.getSquaredDistance(goal) <= 4) {
                // Reconstruct path
                List<BlockPos> path = new ArrayList<>();
                BlockPos node = current;
                while (node != null) {
                    path.add(0, node);
                    node = cameFrom.get(node);
                }
                return path;
            }

            closedSet.add(current);

            // Check neighbors (including diagonal movement and vertical movement)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) { // Allow vertical movement
                        if (dx == 0 && dz == 0 && dy == 0) continue;

                        BlockPos neighbor = current.add(dx, dy, dz);

                        // Check if walkable
                        if (!isWalkable(neighbor)) continue;
                        if (closedSet.contains(neighbor)) continue;

                        // Calculate movement cost
                        double moveCost = 1.0;
                        if (dx != 0 && dz != 0) moveCost = 1.414; // Diagonal
                        if (dy != 0) moveCost += 0.5; // Vertical movement (slight penalty)
                        if (dy > 0) moveCost += 0.3; // Going up costs more

                        // Building blocks costs more
                        BlockState neighborBelow = mc.world.getBlockState(neighbor.down());
                        if (dy < 0 && (neighborBelow.isAir() || neighborBelow.isReplaceable())) {
                            moveCost += 2.0; // Building a block costs more
                        }

                        double tentativeG = gScore.getOrDefault(current, Double.MAX_VALUE) + moveCost;

                        if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                            cameFrom.put(neighbor, current);
                            gScore.put(neighbor, tentativeG);
                            fScore.put(neighbor, tentativeG + heuristic(neighbor, goal));

                            if (!openSet.contains(neighbor)) {
                                openSet.add(neighbor);
                            }
                        }
                    }
                }
            }

            // Check if we're too far
            if (current.getSquaredDistance(start) > MAX_PATHFIND_DISTANCE * MAX_PATHFIND_DISTANCE) {
                break;
            }
        }

        return null; // No path found
    }

    private boolean isWalkable(BlockPos pos) {
        if (mc.world == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        BlockState above = mc.world.getBlockState(pos.up());
        BlockState above2 = mc.world.getBlockState(pos.up(2));
        BlockState below = mc.world.getBlockState(pos.down());

        // Can walk if: current block is air/passable, above is air/passable, below is solid
        boolean canStand = state.isAir() || state.isReplaceable();
        boolean headClear = (above.isAir() || above.isReplaceable()) &&
                (above2.isAir() || above2.isReplaceable());
        boolean hasFloor = !below.isAir() && !below.isReplaceable();

        // Can jump over 1-block obstacles
        if (!canStand && headClear) {
            // Check if it's a 1-block high obstacle we can jump over
            BlockState obstacleTop = mc.world.getBlockState(pos.up(3));
            if (obstacleTop.isAir() || obstacleTop.isReplaceable()) {
                return true; // Can jump over
            }
        }

        // Also check if it's a block we're going to place (allow walking through)
        if (origin != null && schematicBlocks.containsKey(pos.subtract(origin))) {
            return headClear && (hasFloor || canStand);
        }

        // Check if we can build a block here (for pathfinding)
        if (canStand && !hasFloor) {
            // Can build a block below to create a floor
            BlockPos buildPos = pos.down();
            if (canBuildPathBlock(buildPos)) {
                return true; // Can build here
            }
        }

        return headClear && hasFloor;
    }

    private boolean canBuildPathBlock(BlockPos pos) {
        // Check if we have blocks and it's reasonable to build here
        if (findScaffoldItem() == null) return false;

        // Don't build if it's part of the schematic
        if (origin != null && schematicBlocks.containsKey(pos.subtract(origin))) {
            return false;
        }

        // Check distance
        if (mc.player != null) {
            double dist = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
            return !(dist > MAX_PATHFIND_DISTANCE);
        }

        return true;
    }

    private double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private void stopWalking() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }

    private BlockHitResult findPlacementSide(BlockPos pos) {
        if (mc.world == null || mc.player == null) return null;

        // For blocks above player, prefer placing from below
        if (pos.getY() > mc.player.getBlockY()) {
            BlockPos below = pos.down();
            BlockState belowState = mc.world.getBlockState(below);
            if (!belowState.isAir() && !belowState.isReplaceable()) {
                Vec3d hitPos = Vec3d.ofCenter(below);
                return new BlockHitResult(hitPos, Direction.UP, below, false);
            }
        }

        // Check all 6 sides for a valid placement position
        // Prioritize sides that are easier to reach
        Direction[] priorityDirs;
        if (pos.getY() > mc.player.getBlockY()) {
            // For blocks above, prioritize: DOWN, then horizontal sides
            priorityDirs = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                    Direction.EAST, Direction.WEST, Direction.UP};
        } else {
            // For blocks at or below, prioritize: UP, then horizontal sides
            priorityDirs = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH,
                    Direction.EAST, Direction.WEST, Direction.DOWN};
        }

        for (Direction dir : priorityDirs) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighborPos);

            if (!neighborState.isAir() && !neighborState.isReplaceable()) {
                Vec3d hitPos = Vec3d.ofCenter(neighborPos);
                return new BlockHitResult(hitPos, dir.getOpposite(), neighborPos, false);
            }
        }

        // If no solid neighbor found, check if we can place from adjacent air blocks
        // This allows building outward/upward when there's no solid block nearby
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighborPos);

            // If neighbor is air, we can place from the block behind it
            if (neighborState.isAir() || neighborState.isReplaceable()) {
                BlockPos behindNeighbor = neighborPos.offset(dir);
                BlockState behindState = mc.world.getBlockState(behindNeighbor);

                if (!behindState.isAir() && !behindState.isReplaceable()) {
                    Vec3d hitPos = Vec3d.ofCenter(neighborPos);
                    return new BlockHitResult(hitPos, dir.getOpposite(), neighborPos, false);
                }
            }
        }

        // Last resort: if block is in air and we have a floor below, place from below
        BlockPos below = pos.down();
        BlockState belowState = mc.world.getBlockState(below);
        if (!belowState.isAir() && !belowState.isReplaceable()) {
            Vec3d hitPos = Vec3d.ofCenter(below);
            return new BlockHitResult(hitPos, Direction.UP, below, false);
        }

        return null;
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        // Preview is rendered by SchematicBuilderRenderer

        // Render progress if enabled and building
        if (showProgress.getValue() && isBuilding && !isPaused && totalBlocks > 0) {
            renderProgress(event);
        }
    }

    private void renderProgress(GameRenderEvent event) {
        if (mc.player == null || mc.textRenderer == null) return;

        // Progress rendering will be done in HUD or via chat messages
        // For now, we'll just update the progress calculation
        updateProgress();
    }

    private void updateProgress() {
        if (recentPlaceTimes.size() < 2) {
            blocksPerSecond = 0.0;
            return;
        }

        // Calculate BPS from recent placements
        long oldest = recentPlaceTimes.get(0);
        long newest = recentPlaceTimes.get(recentPlaceTimes.size() - 1);
        long timeSpan = newest - oldest;

        if (timeSpan > 0) {
            blocksPerSecond = (recentPlaceTimes.size() - 1) * 1000.0 / timeSpan;
        } else {
            blocksPerSecond = 0.0;
        }
    }

    private double getProgressPercentage() {
        if (totalBlocks == 0) return 0.0;
        return (blocksPlaced / (double) totalBlocks) * 100.0;
    }

    private String getTimeRemainingText() {
        if (blocksPerSecond <= 0 || blocksPlaced >= totalBlocks) {
            return "";
        }

        int remaining = totalBlocks - blocksPlaced;
        long secondsRemaining = (long) (remaining / blocksPerSecond);

        if (secondsRemaining < 60) {
            return secondsRemaining + "s remaining";
        } else if (secondsRemaining < 3600) {
            long minutes = secondsRemaining / 60;
            long secs = secondsRemaining % 60;
            return String.format("%dm %ds remaining", minutes, secs);
        } else {
            long hours = secondsRemaining / 3600;
            long minutes = (secondsRemaining % 3600) / 60;
            return String.format("%dh %dm remaining", hours, minutes);
        }
    }

    private boolean isBlockCounted(BlockPos pos) {
        return countedBlocks.contains(pos);
    }

    private void markBlockAsCounted(BlockPos pos) {
        countedBlocks.add(pos);
    }

    // Getters for renderer
    public boolean hasSchematicLoaded() {
        return !schematicBlocks.isEmpty();
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public int getCurrentLayer() {
        return currentLayer;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public Map<BlockPos, BlockState> getSchematicBlocks() {
        return schematicBlocks;
    }

    public boolean isBlockPlaced(BlockPos worldPos) {
        if (mc.world == null) return false;
        // Check if block exists at this position (simplified check)
        // In a full implementation, you'd verify the block state matches
        return !mc.world.getBlockState(worldPos).isAir();
    }

    private enum ChestState {
        NONE,
        SEARCHING,
        OPENING,
        TAKING_ITEMS
    }

    // Helper classes
        private record BlockPlaceTask(BlockPos pos, BlockState blockState) {
    }
}

