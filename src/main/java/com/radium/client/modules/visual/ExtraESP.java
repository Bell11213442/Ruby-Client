package com.radium.client.modules.visual;

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.ExtraEspRenderer;
import net.minecraft.block.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExtraESP extends Module {
    private static final long CLEANUP_INTERVAL = 30000;
    private static final long VALIDATION_INTERVAL = 1000;
    private static final Set<Block> DEEPSLATE_BLOCKS = Set.of(
            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE,
            Blocks.POLISHED_DEEPSLATE,
            Blocks.DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_TILES,
            Blocks.CHISELED_DEEPSLATE
    );
    private final BooleanSetting detectVines = new BooleanSetting("Detect Vines", true);
    private final BooleanSetting detectKelp = new BooleanSetting("Detect Kelp", true);
    private final BooleanSetting detectVillagers = new BooleanSetting("Detect Villagers", true);
    private final BooleanSetting detectZombieVillagers = new BooleanSetting("Detect Zombie Villagers", true);
    private final BooleanSetting detectPillagers = new BooleanSetting("Detect Pillagers", true);
    private final BooleanSetting detectWanderingTraders = new BooleanSetting("Detect Wandering Traders", true);
    private final BooleanSetting detectDeepslate = new BooleanSetting("Detect Deepslate", true);
    private final BooleanSetting detectRotatedDeepslate = new BooleanSetting("Detect Rotated Deepslate", true);
    private final BooleanSetting detectLlamas = new BooleanSetting("Detect Llamas", true);
    private final BooleanSetting detectAmethyst = new BooleanSetting("Detect Amethyst", true);
    private final BooleanSetting detectOneByOneHoles = new BooleanSetting("Detect 1x1 Holes", true);
    private final BooleanSetting playSound = new BooleanSetting("Sound Alert", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", false);
    private final ModeSetting<ScanSpeed> scanSpeed = new ModeSetting<>("Scan Speed", ScanSpeed.MEDIUM, ScanSpeed.class);
    private final ColorSetting vineColor = new ColorSetting("Vine Color", new Color(14, 232, 14, 245));
    private final ColorSetting kelpColor = new ColorSetting("Kelp Color", new Color(43, 181, 43, 237));
    private final ColorSetting deepslateColor = new ColorSetting("Deepslate Color", new Color(15, 177, 229, 239));
    private final ColorSetting rotatedDeepslateColor = new ColorSetting("Rotated Deepslate Color", new Color(255, 192, 203, 150));
    private final ColorSetting amethystColor = new ColorSetting("Amethyst Color", new Color(99, 12, 230, 219));
    private final ColorSetting oneByOneHoleColor = new ColorSetting("1x1 Hole Color", new Color(255, 165, 0, 239));
    private final ColorSetting villagerColor = new ColorSetting("Villager Color", new Color(60, 179, 113, 255));
    private final ColorSetting zombieVillagerColor = new ColorSetting("Zombie Villager Color", new Color(34, 139, 34, 255));
    private final ColorSetting pillagerColor = new ColorSetting("Pillager Color", new Color(105, 105, 105, 255));
    private final ColorSetting llamaColor = new ColorSetting("Llama Color", new Color(222, 184, 135, 255));
    private final ColorSetting wanderingTraderColor = new ColorSetting("Wandering Trader Color", new Color(30, 144, 255, 255));
    private final Map<BlockPos, ESPBlockType> detectedBlocks = new ConcurrentHashMap<>();
    private final Map<Entity, ESPEntityType> detectedEntities = new ConcurrentHashMap<>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> processedSoundChunks = ConcurrentHashMap.newKeySet();
    private final Set<Entity> processedSoundEntities = ConcurrentHashMap.newKeySet();
    private final Queue<WorldChunk> chunkScanQueue = new ConcurrentLinkedQueue<>();
    private ExecutorService scannerPool;
    private volatile boolean shouldScan = false;
    private long lastCleanup = 0;
    private long lastScan = 0;
    private long lastBlockValidation = 0;
    private long lastNearbyRescan = 0;

    public ExtraESP() {
        super("ExtraESP", "Detects various blocks and entities", Category.VISUAL);
        addSettings(detectVines, detectKelp, detectVillagers, detectZombieVillagers,
                detectPillagers, detectWanderingTraders, detectDeepslate, detectRotatedDeepslate, detectLlamas,
                detectAmethyst, detectOneByOneHoles, playSound, tracers, scanSpeed,
                vineColor, kelpColor, deepslateColor, rotatedDeepslateColor, amethystColor, oneByOneHoleColor,
                villagerColor, zombieVillagerColor, pillagerColor, llamaColor, wanderingTraderColor);
        ExtraEspRenderer.register();
    }

    @Override
    public void onEnable() {
        if (mc.world == null) return;

        clearAll();
        shouldScan = true;
        lastCleanup = System.currentTimeMillis();
        lastScan = 0;
        lastBlockValidation = 0;

        int threads = scanSpeed.getValue().getThreads();
        scannerPool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "ExtraESP-Worker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });

        startInitialScan();
    }

    @Override
    public void onDisable() {
        shouldScan = false;

        if (scannerPool != null) {
            scannerPool.shutdownNow();
            scannerPool = null;
        }

        clearAll();
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        long scanInterval = scanSpeed.getValue().getScanInterval();

        if (now - lastCleanup > CLEANUP_INTERVAL) {
            performCleanup();
            lastCleanup = now;
        }

        if (now - lastBlockValidation > VALIDATION_INTERVAL) {
            validateDetectedBlocks();
            lastBlockValidation = now;
        }

        if (now - lastScan > scanInterval) {
            if (now - lastNearbyRescan > 1000) { // Rescan scanning area every 1s
                rescanNearbyChunks();
                lastNearbyRescan = now;
            }

            scanEntities();
            checkForNewChunks();
            processChunkQueue();
            lastScan = now;
        }
    }

    private void clearAll() {
        detectedBlocks.clear();
        detectedEntities.clear();
        scannedChunks.clear();
        processedSoundChunks.clear();
        processedSoundEntities.clear();
        chunkScanQueue.clear();
    }

    private void startInitialScan() {
        if (scannerPool == null) return;

        scannerPool.submit(() -> {
            try {
                if (mc.world == null || mc.player == null) return;

                List<WorldChunk> chunks = getLoadedChunks();
                for (WorldChunk chunk : chunks) {
                    if (!shouldScan) break;
                    if (chunk != null && !chunk.isEmpty()) {
                        scanChunk(chunk);
                        Thread.sleep(scanSpeed.getValue().getChunkDelay());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void checkForNewChunks() {
        if (mc.world == null || mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = (int) mc.player.getX() / 16;
        int playerChunkZ = (int) mc.player.getZ() / 16;

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                ChunkPos pos = new ChunkPos(playerChunkX + x, playerChunkZ + z);
                if (!scannedChunks.contains(pos)) {
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
                    if (chunk != null && !chunk.isEmpty()) {
                        chunkScanQueue.offer(chunk);
                        scannedChunks.add(pos);
                    }
                }
            }
        }
    }

    private void rescanNearbyChunks() {
        if (mc.player == null) return;
        ChunkPos playerPos = new ChunkPos(mc.player.getBlockPos());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Optional: skip current chunk if needed, but scanning all is better
                ChunkPos pos = new ChunkPos(playerPos.x + x, playerPos.z + z);
                scannedChunks.remove(pos);
            }
        }
        scannedChunks.remove(playerPos);
    }

    private void processChunkQueue() {
        if (scannerPool == null || chunkScanQueue.isEmpty()) return;

        int maxProcess = scanSpeed.getValue().getMaxConcurrent();
        int processed = 0;

        while (!chunkScanQueue.isEmpty() && processed < maxProcess) {
            WorldChunk chunk = chunkScanQueue.poll();
            if (chunk != null) {
                scheduleChunkScan(chunk);
                processed++;
            }
        }
    }

    private void scheduleChunkScan(WorldChunk chunk) {
        if (scannerPool == null) return;

        scannerPool.submit(() -> {
            try {
                Thread.sleep(scanSpeed.getValue().getChunkDelay() / 2);
                scanChunk(chunk);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void scanChunk(WorldChunk chunk) {
        if (!shouldScan || chunk == null) return;

        ChunkPos chunkPos = chunk.getPos();

        try {
            boolean foundSomething = false;

            int minY = Math.max(chunk.getBottomY(), -64);
            int maxY = Math.min(chunk.getBottomY() + chunk.getHeight(), 320);

            int xStart = chunkPos.getStartX();
            int zStart = chunkPos.getStartZ();

            for (int x = xStart; x < xStart + 16; x++) {
                for (int z = zStart; z < zStart + 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        if (!shouldScan) return;

                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = chunk.getBlockState(pos);

                        ESPBlockType type = checkBlock(pos, state, chunk);
                        if (type != null) {
                            detectedBlocks.put(pos, type);
                            foundSomething = true;
                        }
                    }
                }
            }

            if (foundSomething && playSound.getValue() && !processedSoundChunks.contains(chunkPos)) {
                processedSoundChunks.add(chunkPos);
                playFoundSound();
            }
        } catch (Exception e) {
        }
    }

    private void validateDetectedBlocks() {
        if (mc.world == null) return;

        detectedBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            ESPBlockType expectedType = entry.getValue();

            try {
                BlockState state = mc.world.getBlockState(pos);
                Block block = state.getBlock();

                if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
                    return true;
                }

                ESPBlockType currentType = checkBlock(pos, state, null);
                return currentType != expectedType;
            } catch (Exception e) {
                return true;
            }
        });
    }

    private ESPBlockType checkBlock(BlockPos pos, BlockState state, WorldChunk chunk) {
        Block block = state.getBlock();

        if (detectVines.getValue() && block == Blocks.VINE) {
            if (isGroundedVine(pos, chunk)) {
                return ESPBlockType.VINE;
            }
        }

        if (detectKelp.getValue() && (block instanceof KelpBlock || block instanceof KelpPlantBlock)) {
            if (isTopKelp(pos, chunk)) {
                return ESPBlockType.KELP;
            }
        }

        if (detectRotatedDeepslate.getValue() && DEEPSLATE_BLOCKS.contains(block)) {
            if (isRotatedDeepslate(state)) {
                return ESPBlockType.ROTATED_DEEPSLATE;
            }
        }

        if (detectDeepslate.getValue() && block == Blocks.DEEPSLATE && pos.getY() > 16) {
            return ESPBlockType.DEEPSLATE;
        }

        if (detectAmethyst.getValue() && (block == Blocks.AMETHYST_CLUSTER ||
                block == Blocks.LARGE_AMETHYST_BUD ||
                block == Blocks.MEDIUM_AMETHYST_BUD ||
                block == Blocks.SMALL_AMETHYST_BUD)) {
            return ESPBlockType.AMETHYST;
        }

        if (detectOneByOneHoles.getValue() && block == Blocks.AIR) {
            if (isOneByOneHole(pos, chunk)) {
                return ESPBlockType.ONE_BY_ONE_HOLE;
            }
        }

        return null;
    }

    private boolean isGroundedVine(BlockPos pos, WorldChunk chunk) {
        if (pos.getY() <= 0) return false;

        BlockPos below = pos.down();
        BlockState belowState = chunk != null ? chunk.getBlockState(below) : mc.world.getBlockState(below);

        if (!belowState.isAir() && belowState.isSolidBlock(mc.world, below) &&
                belowState.getBlock() != Blocks.VINE) {

            int length = 1;
            for (BlockPos current = pos.up();
                 (chunk != null ? chunk.getBlockState(current) : mc.world.getBlockState(current)).getBlock() == Blocks.VINE;
                 current = current.up()) {
                length++;
                if (length > 100) break;
            }
            return length >= 20;
        }
        return false;
    }

    private boolean isTopKelp(BlockPos pos, WorldChunk chunk) {
        BlockState state = chunk != null ? chunk.getBlockState(pos) : mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof KelpBlock || state.getBlock() instanceof KelpPlantBlock)) {
            return false;
        }

        BlockState above = chunk != null ? chunk.getBlockState(pos.up()) : mc.world.getBlockState(pos.up());
        if (above.getBlock() instanceof KelpBlock || above.getBlock() instanceof KelpPlantBlock) {
            return false;
        }

        if (above.isAir()) {
            return false;
        }

        int length = 0;
        BlockPos current = pos;
        while (length < 100) {
            BlockState currentState = chunk != null ? chunk.getBlockState(current) : mc.world.getBlockState(current);
            if (!(currentState.getBlock() instanceof KelpBlock || currentState.getBlock() instanceof KelpPlantBlock)) {
                break;
            }
            length++;
            current = current.down();
        }

        return length >= 8 && pos.getY() >= 60;
    }

    private boolean isRotatedDeepslate(BlockState state) {
        if (!state.contains(Properties.AXIS)) return false;
        Direction.Axis axis = state.get(Properties.AXIS);
        return axis != Direction.Axis.Y;
    }

    private boolean isOneByOneHole(BlockPos pos, WorldChunk chunk) {
        if (pos.getY() <= 2) return false;

        int solidNeighbors = 0;
        for (Direction dir : Direction.values()) {
            BlockState neighbor = chunk != null ? chunk.getBlockState(pos.offset(dir)) : mc.world.getBlockState(pos.offset(dir));
            if (neighbor.isSolidBlock(mc.world, pos.offset(dir))) {
                solidNeighbors++;
            }
        }

        if (solidNeighbors < 6) return false;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    BlockState checkState = chunk != null ? chunk.getBlockState(checkPos) : mc.world.getBlockState(checkPos);
                    if (checkState.getBlock() == Blocks.AIR) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void scanEntities() {
        if (mc.world == null || mc.player == null) return;

        detectedEntities.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance > 100) continue;

            ESPEntityType type = null;

            if (detectVillagers.getValue() && entity instanceof VillagerEntity) {
                type = ESPEntityType.VILLAGER;
            } else if (detectZombieVillagers.getValue() && entity instanceof ZombieVillagerEntity) {
                type = ESPEntityType.ZOMBIE_VILLAGER;
            } else if (detectPillagers.getValue() && entity instanceof PillagerEntity) {
                type = ESPEntityType.PILLAGER;
            } else if (detectWanderingTraders.getValue() && entity instanceof WanderingTraderEntity) {
                type = ESPEntityType.WANDERING_TRADER;
            } else if (detectLlamas.getValue() && entity instanceof LlamaEntity) {
                type = ESPEntityType.LLAMA;
            }

            if (type != null) {
                detectedEntities.put(entity, type);
                if (playSound.getValue() && !processedSoundEntities.contains(entity)) {
                    processedSoundEntities.add(entity);
                    playFoundSound();
                }
            }
        }
    }

    private void playFoundSound() {
        if (mc.player == null) return;

        mc.execute(() -> {
            mc.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        });
    }

    private void performCleanup() {
        if (mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = (int) mc.player.getX() / 16;
        int playerChunkZ = (int) mc.player.getZ() / 16;

        detectedBlocks.keySet().removeIf(pos -> {
            int chunkX = pos.getX() / 16;
            int chunkZ = pos.getZ() / 16;
            int dx = Math.abs(chunkX - playerChunkX);
            int dz = Math.abs(chunkZ - playerChunkZ);
            return dx > viewDist + 3 || dz > viewDist + 3;
        });

        scannedChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunkX);
            int dz = Math.abs(pos.z - playerChunkZ);
            return dx > viewDist + 2 || dz > viewDist + 2;
        });

        processedSoundChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunkX);
            int dz = Math.abs(pos.z - playerChunkZ);
            return dx > viewDist + 2 || dz > viewDist + 2;
        });

        processedSoundEntities.removeIf(entity -> {
            if (entity == null || !entity.isAlive()) return true;
            double distance = mc.player.distanceTo(entity);
            return distance > 100;
        });
    }

    private List<WorldChunk> getLoadedChunks() {
        List<WorldChunk> chunks = new ArrayList<>();
        if (mc.world == null || mc.player == null) return chunks;

        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = (int) mc.player.getX() / 16;
        int playerChunkZ = (int) mc.player.getZ() / 16;

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(playerChunkX + x, playerChunkZ + z);
                if (chunk != null && !chunk.isEmpty()) {
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }

    public Map<BlockPos, ESPBlockType> getDetectedBlocks() {
        return detectedBlocks;
    }

    public Map<Entity, ESPEntityType> getDetectedEntities() {
        return detectedEntities;
    }

    public boolean isTracers() {
        return tracers.getValue();
    }

    public String getInfoString() {
        return String.valueOf(detectedBlocks.size() + detectedEntities.size());
    }

    public Color getColorForBlockType(ESPBlockType type) {
        return switch (type) {
            case VINE -> vineColor.getValue();
            case KELP -> kelpColor.getValue();
            case DEEPSLATE -> deepslateColor.getValue();
            case ROTATED_DEEPSLATE -> rotatedDeepslateColor.getValue();
            case AMETHYST -> amethystColor.getValue();
            case ONE_BY_ONE_HOLE -> oneByOneHoleColor.getValue();
        };
    }

    public Color getColorForEntityType(ESPEntityType type) {
        return switch (type) {
            case VILLAGER -> villagerColor.getValue();
            case ZOMBIE_VILLAGER -> zombieVillagerColor.getValue();
            case PILLAGER -> pillagerColor.getValue();
            case LLAMA -> llamaColor.getValue();
            case WANDERING_TRADER -> wanderingTraderColor.getValue();
        };
    }

    public enum ESPBlockType {
        VINE,
        KELP,
        DEEPSLATE,
        ROTATED_DEEPSLATE,
        AMETHYST,
        ONE_BY_ONE_HOLE
    }

    public enum ESPEntityType {
        VILLAGER,
        ZOMBIE_VILLAGER,
        PILLAGER,
        WANDERING_TRADER,
        LLAMA
    }

    public enum ScanSpeed {
        SLOW("Slow", 1, 300, 100, 2),
        MEDIUM("Medium", 2, 150, 50, 4),
        FAST("Fast", 3, 75, 25, 6),
        ULTRA("Ultra", 4, 50, 10, 8),
        ULTRA_FAST("Ultra Fast", 8, 25, 5, 16);

        private final String name;
        private final int threads;
        private final long scanInterval;
        private final long chunkDelay;
        private final int maxConcurrent;

        ScanSpeed(String name, int threads, long scanInterval, long chunkDelay, int maxConcurrent) {
            this.name = name;
            this.threads = threads;
            this.scanInterval = scanInterval;
            this.chunkDelay = chunkDelay;
            this.maxConcurrent = maxConcurrent;
        }

        public int getThreads() {
            return threads;
        }

        public long getScanInterval() {
            return scanInterval;
        }

        public long getChunkDelay() {
            return chunkDelay;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
