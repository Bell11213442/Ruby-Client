package com.radium.client.modules.visual;
// radium client

import com.radium.client.events.event.GameRenderListener;
import com.radium.client.gui.settings.BlockSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.BlockEspRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.radium.client.client.RadiumClient.eventManager;

public class BlockESP extends Module implements GameRenderListener {
    private static final long CLEANUP_INTERVAL = 5000;
    private static final long VALIDATION_INTERVAL = 500;

    private final BlockSetting selectedBlocks = new BlockSetting("Selected Blocks", new HashSet<>());
    private final ColorSetting blockColor = new ColorSetting("Block Color", new Color(255, 0, 0, 150));
    private final ModeSetting<ScanSpeed> scanSpeed = new ModeSetting<>("Scan Speed", ScanSpeed.ULTRA_FAST, ScanSpeed.class);
    private final Map<BlockPos, Block> detectedBlocks = new ConcurrentHashMap<>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Queue<WorldChunk> chunkScanQueue = new ConcurrentLinkedQueue<>();
    private final Map<ChunkPos, Long> chunkLastSeen = new ConcurrentHashMap<>();
    private ExecutorService scannerPool;
    private volatile boolean shouldScan = false;
    private long lastCleanup = 0;
    private long lastBlockValidation = 0;
    private Set<Block> lastSelectedBlocks = new HashSet<>();
    private ChunkPos lastPlayerChunk = null;

    public BlockESP() {
        super("BlockESP", "Shows selected blocks with ESP", Category.VISUAL);
        addSettings(selectedBlocks, blockColor, scanSpeed);
        BlockEspRenderer.register();
    }

    @Override
    public void onEnable() {
        if (mc.world == null) return;

        eventManager.add(GameRenderListener.class, this);
        clearAll();
        shouldScan = true;
        lastCleanup = System.currentTimeMillis();
        lastBlockValidation = 0;
        lastSelectedBlocks = new HashSet<>(selectedBlocks.getBlocks());
        lastPlayerChunk = null;

        int threads = scanSpeed.getValue().getThreads();
        scannerPool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "BlockESP-Worker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });

        startInitialScan();
    }

    @Override
    public void onDisable() {
        eventManager.remove(GameRenderListener.class, this);
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

        Set<Block> currentBlocks = selectedBlocks.getBlocks();
        if (currentBlocks.isEmpty()) {
            detectedBlocks.clear();
            scannedChunks.clear();
            chunkLastSeen.clear();
            lastSelectedBlocks.clear();
            return;
        }

        if (!currentBlocks.equals(lastSelectedBlocks)) {
            scannedChunks.clear();
            chunkLastSeen.clear();
            lastSelectedBlocks = new HashSet<>(currentBlocks);
            startInitialScan();
        }

        long now = System.currentTimeMillis();

        if (now - lastCleanup > CLEANUP_INTERVAL) {
            performCleanup();
            lastCleanup = now;
        }

        if (now - lastBlockValidation > VALIDATION_INTERVAL) {
            validateDetectedBlocks();
            lastBlockValidation = now;
        }

        processChunkQueue();

        checkBlockUpdates();
    }

    @Override
    public void onGameRender(GameRenderListener.GameRenderEvent event) {
        if (mc.world == null || mc.player == null || !shouldScan) return;

        ChunkPos currentChunk = mc.player.getChunkPos();
        if (lastPlayerChunk == null || !lastPlayerChunk.equals(currentChunk)) {
            lastPlayerChunk = currentChunk;
            checkForNewChunks();
        }

        long now = System.currentTimeMillis();
        chunkLastSeen.put(currentChunk, now);
    }

    private void checkBlockUpdates() {
        if (mc.world == null || mc.player == null) return;
        Set<Block> targetBlocks = selectedBlocks.getBlocks();
        if (targetBlocks.isEmpty()) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();
        int playerChunkX = playerChunk.x;
        int playerChunkZ = playerChunk.z;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;

                ChunkPos chunkPos = chunk.getPos();
                if (!scannedChunks.contains(chunkPos)) {
                    chunkScanQueue.offer(chunk);
                    scannedChunks.add(chunkPos);
                }
            }
        }
    }

    private void clearAll() {
        detectedBlocks.clear();
        scannedChunks.clear();
        chunkScanQueue.clear();
        chunkLastSeen.clear();
    }

    private void startInitialScan() {
        if (mc.world == null || mc.player == null || scannerPool == null) return;

        List<WorldChunk> chunks = getLoadedChunks();
        for (WorldChunk chunk : chunks) {
            if (!shouldScan) break;
            if (chunk != null && !chunk.isEmpty()) {
                ChunkPos pos = chunk.getPos();
                if (!scannedChunks.contains(pos)) {
                    chunkScanQueue.offer(chunk);
                    scannedChunks.add(pos);
                }
            }
        }
    }

    private void performCleanup() {
        if (mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();
        int playerChunkX = playerChunk.x;
        int playerChunkZ = playerChunk.z;
        long now = System.currentTimeMillis();

        detectedBlocks.keySet().removeIf(pos -> {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            int dx = Math.abs(chunkX - playerChunkX);
            int dz = Math.abs(chunkZ - playerChunkZ);
            if (dx > viewDist + 2 || dz > viewDist + 2) {
                return true;
            }
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            Long lastSeen = chunkLastSeen.get(chunkPos);
            return lastSeen != null && now - lastSeen > 10000;
        });

        scannedChunks.removeIf(pos -> {
            int dx = Math.abs(pos.x - playerChunkX);
            int dz = Math.abs(pos.z - playerChunkZ);
            return dx > viewDist + 2 || dz > viewDist + 2;
        });

        chunkLastSeen.entrySet().removeIf(entry -> {
            ChunkPos pos = entry.getKey();
            int dx = Math.abs(pos.x - playerChunkX);
            int dz = Math.abs(pos.z - playerChunkZ);
            return dx > viewDist + 2 || dz > viewDist + 2;
        });
    }

    private void checkForNewChunks() {
        if (mc.world == null || mc.player == null) return;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();
        int playerChunkX = playerChunk.x;
        int playerChunkZ = playerChunk.z;

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                ChunkPos pos = new ChunkPos(playerChunkX + x, playerChunkZ + z);
                if (!scannedChunks.contains(pos)) {
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
                    if (chunk != null && !chunk.isEmpty()) {
                        chunkScanQueue.offer(chunk);
                        scannedChunks.add(pos);
                        chunkLastSeen.put(pos, System.currentTimeMillis());
                    }
                }
            }
        }
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
                scanChunk(chunk);
            } catch (Exception e) {
            }
        });
    }

    private void scanChunk(WorldChunk chunk) {
        if (!shouldScan || chunk == null) return;

        ChunkPos chunkPos = chunk.getPos();
        Set<Block> targetBlocks = selectedBlocks.getBlocks();

        if (targetBlocks.isEmpty()) return;

        try {
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
                        Block block = state.getBlock();

                        if (targetBlocks.contains(block)) {
                            detectedBlocks.put(pos, block);
                        } else {
                            detectedBlocks.remove(pos);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void validateDetectedBlocks() {
        if (mc.world == null) return;
        Set<Block> targetBlocks = selectedBlocks.getBlocks();

        detectedBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            try {
                BlockState state = mc.world.getBlockState(pos);
                Block block = state.getBlock();
                return !targetBlocks.contains(block);
            } catch (Exception e) {
                return true;
            }
        });
    }

    private List<WorldChunk> getLoadedChunks() {
        List<WorldChunk> chunks = new ArrayList<>();
        if (mc.world == null || mc.player == null) return chunks;

        int viewDist = mc.options.getViewDistance().getValue();
        ChunkPos playerChunk = mc.player.getChunkPos();
        int playerChunkX = playerChunk.x;
        int playerChunkZ = playerChunk.z;

        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(playerChunkX + x, playerChunkZ + z);
                if (chunk != null && !chunk.isEmpty()) {
                    chunks.add(chunk);
                    ChunkPos pos = chunk.getPos();
                    chunkLastSeen.put(pos, System.currentTimeMillis());
                }
            }
        }
        return chunks;
    }

    public Map<BlockPos, Block> getDetectedBlocks() {
        return detectedBlocks;
    }

    public Color getBlockColor() {
        return blockColor.getValue();
    }

    public enum ScanSpeed {
        SLOW("Slow", 2, 100, 0, 4),
        MEDIUM("Medium", 4, 50, 0, 8),
        FAST("Fast", 6, 25, 0, 12),
        ULTRA("Ultra", 8, 10, 0, 16),
        ULTRA_FAST("Ultra Fast", 16, 5, 0, 32);

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


