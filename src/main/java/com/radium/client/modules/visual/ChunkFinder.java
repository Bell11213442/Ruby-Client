package com.radium.client.modules.visual;

import com.radium.client.events.event.GameRenderListener;
import com.radium.client.gui.RenderUtils;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import com.radium.client.utils.ToastUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.client.font.TextRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.radium.client.client.RadiumClient.eventManager;

public final class ChunkFinder extends Module implements GameRenderListener {

    private static final boolean USE_THREADS = true;
    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    private static final int SCAN_DELAY_MS = 100;
    private static final int MAX_CONCURRENT_SCANS = 3;
    private static final int DEEPSLATE_THRESHOLD = 3;
    private static final int ROTATED_THRESHOLD = 1;
    private static final long RESET_INTERVAL_MS = 500;
    private final Set<ChunkPos> flaggedChunks = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<ChunkPos, ChunkAnalysis> chunkData = new ConcurrentHashMap<>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkPos> scanQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong activeScans = new AtomicLong(0);
    private final BooleanSetting ignorePlayerChunk = new BooleanSetting("Ignore Player Chunk", true);
    private final BooleanSetting showReasons = new BooleanSetting("Show Reasons", true);
    private final BooleanSetting detectItems = new BooleanSetting("Check Items", true);
    private final NumberSetting maxItems = new NumberSetting("Max Items", 3, 0, 100, 1);
    private final BooleanSetting detectXP = new BooleanSetting("Check XP Orbs", true);
    private final NumberSetting maxXP = new NumberSetting("Max XP Orbs", 3, 0, 100, 1);
    private final Map<ChunkPos, Integer> chunkItemCounts = new ConcurrentHashMap<>();
    private final Map<ChunkPos, Integer> chunkXPCounts = new ConcurrentHashMap<>();

    private final BooleanSetting alertCoorrds = new BooleanSetting(("Alert Coordinates"), true);
    // Explicit thresholds
    private final NumberSetting deepslateThreshold = new NumberSetting("Deepslate Limit", 3, 1, 50, 1);
    private final NumberSetting rotatedThreshold = new NumberSetting("Rotated Deepslate Limit", 1, 1, 20, 1);
    
    private final boolean detectDeepslate = false;
    private final boolean detectRotatedDeepslate = true;
    private final boolean detectLongDripstone = true;
    private final int minDripstoneLength = 7;
    private final boolean detectFullVines = true;
    private final int minVineLength = 30;
    private final boolean detectFullKelp = true;
    private final int minKelpLength = 6;
    private final boolean detectDioriteVeins = true;
    private final int minDioriteVeinLength = 5;
    private final boolean detectObsidianVeins = true;
    private final int minObsidianVeinLength = 15;
    private final int minScanY = -5;
    private final int maxScanY = 25;
    private final int scanRadius = 16;
    private final boolean chatAlerts = true;
    private final boolean soundAlerts = true;
    private final boolean toastAlerts = true;
    private ChunkPos lastPlayerChunk = null;
    private ExecutorService pool;
    private volatile boolean scanning = false;
    private long lastResetTime = 0;


    public ChunkFinder() {
        super(("Chunk Finder"), ("Detects suspicious chunks that might contain bases."), Category.VISUAL);
        addSettings(alertCoorrds, ignorePlayerChunk, showReasons, detectItems, maxItems, detectXP, maxXP, deepslateThreshold, rotatedThreshold);
    }

    public void Reset() {
        scanning = false;
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        scannedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;

        scanning = true;
        scannedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;

        if (USE_THREADS) {
            pool = Executors.newFixedThreadPool(THREAD_COUNT);
        }
    }

    @Override
    public void onTick() {
        long currentTime = System.currentTimeMillis();
        if (mc.world != null) {
            chunkItemCounts.clear();
            chunkXPCounts.clear();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemEntity) {
                    chunkItemCounts.merge(entity.getChunkPos(), 1, Integer::sum);
                } else if (entity instanceof ExperienceOrbEntity) {
                    chunkXPCounts.merge(entity.getChunkPos(), 1, Integer::sum);
                }
            }
        }

        if (currentTime - lastResetTime >= RESET_INTERVAL_MS) {
            Reset();
            lastResetTime = currentTime;
        }

        if (mc.world == null) {
            scanning = false;
            if (pool != null) {
                pool.shutdownNow();
                pool = null;
            }
            scannedChunks.clear();
            flaggedChunks.clear();
            chunkData.clear();
            scanQueue.clear();
            scanning = true;
            scannedChunks.clear();
            flaggedChunks.clear();
            chunkData.clear();
            scanQueue.clear();
            lastPlayerChunk = null;

            if (USE_THREADS) {
                pool = Executors.newFixedThreadPool(THREAD_COUNT);
            }
        }
        super.onTick();
    }

    @Override
    public void onEnable() {
        eventManager.add(GameRenderListener.class, this);
        scanning = true;
        scannedChunks.clear();
        flaggedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;

        if (USE_THREADS) {
            pool = Executors.newFixedThreadPool(THREAD_COUNT);
        }
    }

    @Override
    public void onDisable() {
        eventManager.remove(GameRenderListener.class, this);
        scanning = false;
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        scannedChunks.clear();
        flaggedChunks.clear();
        chunkData.clear();
        scanQueue.clear();
        lastPlayerChunk = null;
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (lastPlayerChunk == null && scanning) {
            int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
            int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
            lastPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);

            buildBFSScanQueue(lastPlayerChunk);
            tryStartScans();
        }

        updateScanQueue();
        tryStartScans();
        renderFlaggedChunks(event.matrices);
    }


    private void updateScanQueue() {
        if (mc.player == null) return;

        int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
        int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
        ChunkPos currentPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);

        if (currentPlayerChunk.equals(lastPlayerChunk)) return;

        lastPlayerChunk = currentPlayerChunk;

        cleanupDistantChunks(currentPlayerChunk);

        scanQueue.clear();
        buildBFSScanQueue(currentPlayerChunk);
    }

    private void cleanupDistantChunks(ChunkPos center) {
        int radius = scanRadius;
        int cleanupRadius = radius + 2;

        scannedChunks.removeIf(chunk -> {
            int dx = Math.abs(chunk.x - center.x);
            int dz = Math.abs(chunk.z - center.z);
            return dx > cleanupRadius || dz > cleanupRadius;
        });
    }

    private void buildBFSScanQueue(ChunkPos center) {
        int radius = scanRadius;
        Set<ChunkPos> visited = new HashSet<>();
        Queue<ChunkPos> bfsQueue = new LinkedList<>();

        bfsQueue.offer(center);
        visited.add(center);

        while (!bfsQueue.isEmpty()) {
            ChunkPos current = bfsQueue.poll();

            if (!scannedChunks.contains(current)) {
                scanQueue.offer(current);
            }

            int[][] offsets = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            for (int[] offset : offsets) {
                ChunkPos neighbor = new ChunkPos(current.x + offset[0], current.z + offset[1]);

                int dx = Math.abs(neighbor.x - center.x);
                int dz = Math.abs(neighbor.z - center.z);

                if (dx <= radius && dz <= radius && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    bfsQueue.offer(neighbor);
                }
            }
        }
    }

    private void tryStartScans() {
        if (!scanning || mc.world == null || mc.player == null) return;

        while (activeScans.get() < MAX_CONCURRENT_SCANS && !scanQueue.isEmpty()) {
            ChunkPos pos = scanQueue.poll();
            if (pos == null || scannedChunks.contains(pos)) continue;

            scannedChunks.add(pos);
            Runnable task = () -> analyzeChunk(pos);

            if (USE_THREADS && pool != null) {
                pool.submit(wrapScanTask(task));
            } else {
                wrapScanTask(task).run();
            }
        }
    }

    private Runnable wrapScanTask(Runnable task) {
        return () -> {
            activeScans.incrementAndGet();
            try {
                task.run();
                Thread.sleep(SCAN_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeScans.decrementAndGet();
            }
        };
    }

    private void analyzeChunk(ChunkPos pos) {
        if (mc.world == null) return;

        int startX = pos.getStartX();
        int startZ = pos.getStartZ();
        int minY = Math.max(minScanY, mc.world.getBottomY());
        int maxY = Math.min(maxScanY, mc.world.getTopY() - 1);

        ChunkAnalysis analysis = new ChunkAnalysis();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!scanning) return;

                    BlockPos bp = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = mc.world.getBlockState(bp);
                    analyzeBlock(bp, state, y, analysis);
                }
            }
        }

        if (detectLongDripstone) {
            BlockPos dripstonePos = checkHasLongDripstone(pos);
            if (dripstonePos != null) {
                analysis.hasLongDripstone = true;
                if (analysis.susBlockPos == null) {
                    analysis.susBlockPos = dripstonePos;
                }
            }
        }

        if (detectFullVines) {
            BlockPos vinePos = checkHasLongVine(pos);
            if (vinePos != null) {
                analysis.hasLongVine = true;
                if (analysis.susBlockPos == null) {
                    analysis.susBlockPos = vinePos;
                }
            }
        }

        if (detectFullKelp) {
            BlockPos kelpPos = checkAllKelpFullyGrown(pos);
            if (kelpPos != null) {
                analysis.allKelpFull = true;
                if (analysis.susBlockPos == null) {
                    analysis.susBlockPos = kelpPos;
                }
            }
        }

        if (detectDioriteVeins) {
            BlockPos dioriteVeinPos = checkHasDioriteVein(pos);
            if (dioriteVeinPos != null) {
                analysis.hasDioriteVein = true;
                if (analysis.susBlockPos == null) {
                    analysis.susBlockPos = dioriteVeinPos;
                }
            }
        }

        if (detectObsidianVeins) {
            BlockPos obsidianVeinPos = checkHasObsidianVein(pos);
            if (obsidianVeinPos != null) {
                analysis.hasObsidianVein = true;
                if (analysis.susBlockPos == null) {
                    analysis.susBlockPos = obsidianVeinPos;
                }
            }
        }

        chunkData.put(pos, analysis);
        evaluateChunk(pos, analysis);
    }

    private void analyzeBlock(BlockPos pos, BlockState state, int worldY, ChunkAnalysis analysis) {
        SuspiciousType type = null;
        Block block = state.getBlock();

        if (detectDeepslate && isNormalDeepslate(state) && worldY >= 8) {
            analysis.deepslateCount++;
            type = SuspiciousType.DEEPSLATE;
            if (analysis.susBlockPos == null)
                analysis.susBlockPos = pos;
        }

        if (detectRotatedDeepslate && isRotatedDeepslate(state)) {
            analysis.rotatedCount++;
            type = SuspiciousType.ROTATED_DEEPSLATE;
            if (analysis.susBlockPos == null)
                analysis.susBlockPos = pos;
        }
    }

    private BlockPos checkHasDioriteVein(ChunkPos chunkPos) {
        if (mc.world == null) return null;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int yMin = Math.max(mc.world.getBottomY(), -64);
        int yMax = mc.world.getTopY();

        Set<BlockPos> visited = new HashSet<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yMin; y < yMax; y++) {
                    if (!scanning) return null;

                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (visited.contains(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    if (!isTargetBlock(state)) continue;
                    int lenUp = countVerticalRun(pos, Direction.UP);
                    int lenDown = countVerticalRun(pos, Direction.DOWN);
                    int total = lenUp + 1 + lenDown;

                    if (total >= minDioriteVeinLength) {
                        BlockPos start = pos.offset(Direction.DOWN, lenDown);
                        boolean enclosed = true;
                        for (int i = 0; i < total; i++) {
                            BlockPos bp = start.offset(Direction.UP, i);
                            visited.add(bp);
                            if (!isEnclosedByStone(bp)) {
                                enclosed = false;
                            }
                        }

                        if (enclosed) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private BlockPos checkHasObsidianVein(ChunkPos chunkPos) {
        if (mc.world == null) return null;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int yMin = 15;
        int yMax = 63;

        Set<BlockPos> visited = new HashSet<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    if (!scanning) return null;

                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (visited.contains(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    if (!state.isOf(Blocks.OBSIDIAN)) continue;
                    int lenUp = countVerticalRunObsidian(pos, Direction.UP);
                    int lenDown = countVerticalRunObsidian(pos, Direction.DOWN);
                    int total = lenUp + 1 + lenDown;

                    if (total >= minObsidianVeinLength) {
                        BlockPos start = pos.offset(Direction.DOWN, lenDown);
                        boolean enclosed = true;
                        for (int i = 0; i < total; i++) {
                            BlockPos bp = start.offset(Direction.UP, i);
                            visited.add(bp);
                            if (!isEnclosedByNonObsidian(bp)) {
                                enclosed = false;
                            }
                        }

                        if (enclosed) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isTargetBlock(BlockState state) {
        return state.isOf(Blocks.GRANITE) || state.isOf(Blocks.DIORITE) || state.isOf(Blocks.ANDESITE);
    }

    private int countVerticalRun(BlockPos from, Direction dir) {
        int count = 0;
        BlockPos.Mutable m = new BlockPos.Mutable(from.getX(), from.getY(), from.getZ());
        while (true) {
            m.move(dir);
            if (isTargetBlock(mc.world.getBlockState(m))) {
                count++;
            } else {
                break;
            }
            if (count > 20) break;
        }
        return count;
    }

    private int countVerticalRunObsidian(BlockPos from, Direction dir) {
        int count = 0;
        BlockPos.Mutable m = new BlockPos.Mutable(from.getX(), from.getY(), from.getZ());
        while (true) {
            m.move(dir);
            if (mc.world.getBlockState(m).isOf(Blocks.OBSIDIAN)) {
                count++;
            } else {
                break;
            }
            if (count > 20) break;
        }
        return count;
    }

    private boolean isEnclosedByStone(BlockPos pos) {
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : horizontalDirections) {
            BlockPos adj = pos.offset(d);
            BlockState st = mc.world.getBlockState(adj);
            if (!st.isOf(Blocks.STONE)) return false;
        }
        return true;
    }

    private boolean isEnclosedByNonObsidian(BlockPos pos) {
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : horizontalDirections) {
            BlockPos adj = pos.offset(d);
            BlockState st = mc.world.getBlockState(adj);
            if (st.isOf(Blocks.OBSIDIAN)) return false;
        }
        return true;
    }

    private BlockPos checkHasLongDripstone(ChunkPos chunkPos) {
        if (mc.world == null) return null;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getTopY() - 1;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMaxY; y >= worldMinY; y--) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (state.getBlock() == Blocks.POINTED_DRIPSTONE && isTopOfDripstone(pos, state)) {
                        BlockPos bottomPos = pos;
                        int length = 1;
                        BlockPos current = pos.down();

                        while (current.getY() >= worldMinY && length < 50) {
                            BlockState currentState = mc.world.getBlockState(current);
                            if (currentState.getBlock() != Blocks.POINTED_DRIPSTONE) break;
                            if (!currentState.contains(Properties.VERTICAL_DIRECTION)) break;
                            if (currentState.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) break;
                            bottomPos = current;
                            length++;
                            current = current.down();
                        }

                        if (length >= minDripstoneLength) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isTopOfDripstone(BlockPos pos, BlockState state) {
        if (mc.world == null) return false;
        if (state.getBlock() != Blocks.POINTED_DRIPSTONE) return false;

        if (!state.contains(Properties.VERTICAL_DIRECTION)) return false;
        if (state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) return false;

        BlockPos above = pos.up();
        BlockState aboveState = mc.world.getBlockState(above);
        return aboveState.getBlock() != Blocks.POINTED_DRIPSTONE;
    }

    private int measureDripstoneLength(BlockPos startPos) {
        if (mc.world == null) return 0;

        BlockState startState = mc.world.getBlockState(startPos);
        if (startState.getBlock() != Blocks.POINTED_DRIPSTONE) return 0;

        if (!startState.contains(Properties.VERTICAL_DIRECTION)) return 0;
        if (startState.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) return 0;

        int length = 1;
        BlockPos current = startPos.down();

        while (length < 30) {
            BlockState state = mc.world.getBlockState(current);
            if (state.getBlock() != Blocks.POINTED_DRIPSTONE) break;
            if (!state.contains(Properties.VERTICAL_DIRECTION)) break;
            if (state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) break;
            length++;
            current = current.down();
        }

        return length;
    }

    private BlockPos checkHasLongVine(ChunkPos chunkPos) {
        if (mc.world == null) return null;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        Set<BlockPos> processedVineTops = ConcurrentHashMap.newKeySet();

        int scanTopY = Math.min(mc.world.getTopY() - 1, 320);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = scanTopY; y >= 40; y--) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);

                    if (processedVineTops.contains(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);

                    if (state.getBlock() == Blocks.VINE) {
                        BlockPos topPos = pos.up();
                        BlockState topState = mc.world.getBlockState(topPos);

                        boolean isVineTop = topState.getBlock() != Blocks.VINE &&
                                (topState.isSolidBlock(mc.world, topPos) || !topState.isAir());

                        if (!isVineTop) continue;

                        processedVineTops.add(pos);

                        BlockPos bottomPos = pos;
                        int vineLength = 1;
                        BlockPos current = pos.down();

                        while (current.getY() >= Math.max(mc.world.getBottomY(), 40)) {
                            BlockState currentState = mc.world.getBlockState(current);
                            if (currentState.getBlock() == Blocks.VINE) {
                                bottomPos = current;
                                vineLength++;
                                current = current.down();
                            } else {
                                break;
                            }
                        }

                        if (vineLength >= minVineLength) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private BlockPos checkAllKelpFullyGrown(ChunkPos chunkPos) {
        if (mc.world == null) return null;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int kelpPlantsFound = 0;
        int fullKelpPlants = 0;
        BlockPos firstKelpPos = null;

        Set<BlockPos> processedKelpBases = ConcurrentHashMap.newKeySet();

        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getTopY() - 1;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMinY; y <= worldMaxY; y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);

                    if (processedKelpBases.contains(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);

                    if (state.getBlock() == Blocks.KELP || state.getBlock() == Blocks.KELP_PLANT) {
                        BlockPos belowPos = pos.down();
                        BlockState belowState = mc.world.getBlockState(belowPos);

                        boolean isKelpBase = belowState.getBlock() != Blocks.KELP &&
                                belowState.getBlock() != Blocks.KELP_PLANT;

                        if (!isKelpBase) continue;

                        processedKelpBases.add(pos);

                        if (firstKelpPos == null) {
                            firstKelpPos = pos;
                        }

                        BlockPos topPos = pos;
                        BlockPos current = pos.up();
                        boolean reachedWaterSurface = false;
                        int kelpLength = 1;

                        while (current.getY() <= worldMaxY) {
                            BlockState currentState = mc.world.getBlockState(current);
                            if (currentState.getBlock() == Blocks.KELP || currentState.getBlock() == Blocks.KELP_PLANT) {
                                topPos = current;
                                kelpLength++;
                                current = current.up();
                            } else if (!currentState.getFluidState().isEmpty()) {
                                break;
                            } else {
                                reachedWaterSurface = true;
                                break;
                            }
                        }
                        if (kelpLength < minKelpLength && reachedWaterSurface) {
                            continue;
                        }
                        kelpPlantsFound++;
                        if (reachedWaterSurface) {
                            fullKelpPlants++;
                        }
                    }
                }
            }
        }
        if (kelpPlantsFound < 10) return null;

        boolean allFull = kelpPlantsFound == fullKelpPlants;
        return allFull ? firstKelpPos : null;
    }

    private void evaluateChunk(ChunkPos pos, ChunkAnalysis analysis) {
        boolean suspicious = false;
        List<String> reasonList = new ArrayList<>();

        // Safeguards
        if (ignorePlayerChunk.getValue() && pos.equals(lastPlayerChunk)) {
             flaggedChunks.remove(pos);
             return;
        }

        if (detectItems.getValue()) {
            int items = chunkItemCounts.getOrDefault(pos, 0);
            if (items > maxItems.getValue()) {
                 flaggedChunks.remove(pos);
                 return;
            }
        }
        
        if (detectXP.getValue()) {
             int xp = chunkXPCounts.getOrDefault(pos, 0);
             if (xp > maxXP.getValue()) {
                  flaggedChunks.remove(pos);
                  return;
             }
        }

        if (detectDeepslate && analysis.deepslateCount >= deepslateThreshold.getValue()) {
            suspicious = true;
            reasonList.add("Deepslate: " + analysis.deepslateCount);
        }
        if (detectRotatedDeepslate && analysis.rotatedCount >= rotatedThreshold.getValue()) {
             suspicious = true;
             reasonList.add("Rotated: " + analysis.rotatedCount);
        }
        if (detectLongDripstone && analysis.hasLongDripstone) {
            suspicious = true;
            reasonList.add("Long Dripstone");
        }
        if (detectFullVines && analysis.hasLongVine) {
            suspicious = true;
            reasonList.add("Long Vine");
        }
        if (detectFullKelp && analysis.allKelpFull) {
            suspicious = true;
            reasonList.add("Grown Kelp");
        }
        if (detectDioriteVeins && analysis.hasDioriteVein) {
            suspicious = true;
             reasonList.add("Diorite Vein");
        }
        if (detectObsidianVeins && analysis.hasObsidianVein) {
            suspicious = true;
             reasonList.add("Obsidian Vein");
        }
        
        analysis.reasons = reasonList;

        if (suspicious) {
            if (flaggedChunks.add(pos)) {
                StringBuilder reasons = new StringBuilder();
                for (String reason : reasonList) reasons.append(reason).append(" ");
                
                int susBlockX = 0;
                int susBlockZ = 0;

                if (analysis.susBlockPos != null) {
                    susBlockX = analysis.susBlockPos.getX();
                    susBlockZ = analysis.susBlockPos.getZ();
                }

                if (chatAlerts && mc.player != null) {
                     ChatUtils.m("Suspicious Chunk Detected - " + reasons);
                }
                if (soundAlerts) {
                    try {
                        mc.getSoundManager().play(PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
                    } catch (Throwable ignored) {
                    }
                }
                if (toastAlerts) {
                    try {
                        final int finalX = susBlockX;
                        final int finalZ = susBlockZ;
                        if (alertCoorrds.getValue()) {
                            mc.execute(() -> mc.getToastManager().add(new ToastUtil(Items.CHEST, "ChunkFinder", "X: " + finalX + " Z: " + finalZ, false) {
                            }));
                        } else {
                            mc.execute(() -> mc.getToastManager().add(new ToastUtil(Items.CHEST, "ChunkFinder", "Suspicious Chunk Detected", false) {
                            }));
                        }

                    } catch (Throwable ignored) {
                    }
                }
            }
        } else {
            flaggedChunks.remove(pos);
        }
    }

    private boolean isNormalDeepslate(BlockState state) {
        return state.getBlock() == Blocks.DEEPSLATE;
    }

    private boolean isRotatedDeepslate(BlockState state) {
        if (!state.contains(Properties.AXIS)) return false;
        Direction.Axis axis = state.get(Properties.AXIS);
        if (axis == Direction.Axis.Y) return false;
        Block block = state.getBlock();
        return block == Blocks.DEEPSLATE;
    }

    private void renderFlaggedChunks(MatrixStack matrices) {
        if (flaggedChunks.isEmpty()) return;
        Camera cam = mc.getBlockEntityRenderDispatcher().camera;
        if (cam == null) return;

        Vec3d camPos = cam.getPos();
        int rendered = 0;
        int renderY = 63;

        for (ChunkPos pos : flaggedChunks) {
            if (rendered++ >= 50) break;

            double startX = pos.getStartX();
            double startZ = pos.getStartZ();
            double endX = pos.getEndX() + 1;
            double endZ = pos.getEndZ() + 1;

            Color c = new Color(0, 255, 0, 70);

            matrices.push();
            Vec3d vec = cam.getPos();

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180F));
            matrices.translate(-vec.x, -vec.y, -vec.z);

            RenderUtils.renderFilledBox(
                    matrices,
                    (float) startX, (float) renderY, (float) startZ,
                    (float) endX, (float) renderY, (float) endZ,
                    c
            );

            if (showReasons.getValue()) {
                ChunkAnalysis analysis = chunkData.get(pos);
                if (analysis != null && analysis.reasons != null && !analysis.reasons.isEmpty()) {
                    TextRenderer tr = mc.textRenderer;
                    float scale = 0.05f;
                    
                    matrices.push();
                    matrices.translate(startX + 8, renderY + 2, startZ + 8);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cam.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
                    matrices.scale(-scale, -scale, scale);
                    
                    int yOffset = 0;
                    for (String reason : analysis.reasons) {
                        float width = tr.getWidth(reason);

                        tr.draw(
                                reason,
                                -tr.getWidth(reason) / 2f,
                                yOffset,
                                0xFFFFFFFF,
                                true,
                                matrices.peek().getPositionMatrix(),
                                mc.getBufferBuilders().getEntityVertexConsumers(),
                                TextRenderer.TextLayerType.NORMAL,
                                0,
                                LightmapTextureManager.MAX_LIGHT_COORDINATE
                        );


                        yOffset += 10;
                    }
                    
                    matrices.pop();
                }
            }

            matrices.pop();
        }
    }

    private enum SuspiciousType {
        DEEPSLATE,
        ROTATED_DEEPSLATE,
        LONG_DRIPSTONE,
        LONG_VINE,
        FULL_KELP,
        DIORITE_VEIN,
        OBSIDIAN_VEIN
    }

    private static class ChunkAnalysis {
        int deepslateCount = 0;
        int rotatedCount = 0;
        boolean hasLongDripstone = false;
        boolean hasLongVine = false;
        boolean allKelpFull = false;
        boolean hasDioriteVein = false;
        boolean hasObsidianVein = false;
        
        List<String> reasons = new ArrayList<>();

        BlockPos susBlockPos = null;
    }
}