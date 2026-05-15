package com.radium.client.modules.visual;
// radium client

import com.mojang.blaze3d.systems.RenderSystem;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class LightESP extends Module {
    private static final int CHUNK_RADIUS = 2;
    private static final int MIN_Y = -63;
    private static final int MAX_Y = -1;
    private static final int MIN_LIGHT_LEVEL = 5;
    private static final Set<BlockPos> blocksToSkip = ConcurrentHashMap.newKeySet();
    private static final long OPTIMIZATION_COOLDOWN = 1000L;
    private static long lastOptimizationTime = 0L;
    private final ColorSetting minLightColor = new ColorSetting("Min Light Color", new Color(64, 0, 128, 128));
    private final ColorSetting maxLightColor = new ColorSetting("Max Light Color", new Color(255, 255, 255, 255));

    private final ExecutorService executor = Executors
            .newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private volatile Map<Integer, List<BlockPos>> groupedLights = new HashMap<>();
    private volatile Set<BlockPos> allLightPositions = new HashSet<>();

    public LightESP() {
        super("LightDebug", "Shows Light Level Under Y=0", Category.VISUAL);
        settings.add(minLightColor);
        settings.add(maxLightColor);
        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
    }

    private static boolean shouldRenderBlock(BlockPos pos, Set<BlockPos> allLightPositions) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!allLightPositions.contains(neighbor))
                return true;
        }
        return false;
    }

    private static void renderExposedFacesAnyLightLevel(BufferBuilder buffer, Matrix4f matrix, float x, float y,
                                                        float z, BlockPos currentPos, Set<BlockPos> allLightPositions, float r, float g, float b, float a) {

        if (!allLightPositions.contains(currentPos.down())) {
            buffer.vertex(matrix, x, y, z).color(r, g, b, a);
            buffer.vertex(matrix, x, y, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y, z).color(r, g, b, a);
        }

        if (!allLightPositions.contains(currentPos.up())) {
            buffer.vertex(matrix, x, y + 1f, z).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y + 1f, z).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y + 1f, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x, y + 1f, z + 1f).color(r, g, b, a);
        }

        if (!allLightPositions.contains(currentPos.north())) {
            buffer.vertex(matrix, x, y, z).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y, z).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y + 1f, z).color(r, g, b, a);
            buffer.vertex(matrix, x, y + 1f, z).color(r, g, b, a);
        }

        if (!allLightPositions.contains(currentPos.south())) {
            buffer.vertex(matrix, x, y, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x, y + 1f, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y + 1f, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y, z + 1f).color(r, g, b, a);
        }

        if (!allLightPositions.contains(currentPos.west())) {
            buffer.vertex(matrix, x, y, z).color(r, g, b, a);
            buffer.vertex(matrix, x, y + 1f, z).color(r, g, b, a);
            buffer.vertex(matrix, x, y + 1f, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x, y, z + 1f).color(r, g, b, a);
        }

        if (!allLightPositions.contains(currentPos.east())) {
            buffer.vertex(matrix, x + 1f, y, z).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y + 1f, z + 1f).color(r, g, b, a);
            buffer.vertex(matrix, x + 1f, y + 1f, z).color(r, g, b, a);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        blocksToSkip.clear();
        if (isScanning.compareAndSet(false, true)) {
            executor.submit(this::updateLights);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        groupedLights = new HashMap<>();
        allLightPositions = new HashSet<>();
    }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null)
            return;

        if (isScanning.compareAndSet(false, true)) {
            executor.submit(this::updateLights);
        }
    }

    private void updateLights() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null)
                return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastOptimizationTime >= OPTIMIZATION_COOLDOWN) {
                optimizeWeakLights(client);
                lastOptimizationTime = currentTime;
            }

            Map<Integer, List<BlockPos>> newGroupedLights = new ConcurrentHashMap<>();
            Set<BlockPos> newAllLightPositions = ConcurrentHashMap.newKeySet();

            ChunkPos playerChunkPos = new ChunkPos(client.player.getBlockPos());
            List<ChunkPos> chunksToScan = new ArrayList<>();
            for (int chunkX = playerChunkPos.x - CHUNK_RADIUS; chunkX <= playerChunkPos.x + CHUNK_RADIUS; ++chunkX) {
                for (int chunkZ = playerChunkPos.z - CHUNK_RADIUS; chunkZ <= playerChunkPos.z
                        + CHUNK_RADIUS; ++chunkZ) {
                    chunksToScan.add(new ChunkPos(chunkX, chunkZ));
                }
            }


            for (ChunkPos cp : chunksToScan) {
                WorldChunk chunk = client.world.getChunk(cp.x, cp.z);
                if (chunk != null && chunk.getStatus().isAtLeast(ChunkStatus.FULL)) {
                    int startX = cp.getStartX();
                    int startZ = cp.getStartZ();

                    for (int x = 0; x < 16; ++x) {
                        for (int z = 0; z < 16; ++z) {
                            for (int y = MIN_Y; y <= MAX_Y; ++y) {
                                BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                                if (!blocksToSkip.contains(pos)) {
                                    int blockLight = client.world.getLightLevel(LightType.BLOCK, pos);
                                    int skyLight = client.world.getLightLevel(LightType.SKY, pos);
                                    if (blockLight >= MIN_LIGHT_LEVEL && blockLight > skyLight) {
                                        newAllLightPositions.add(pos);
                                        newGroupedLights.computeIfAbsent(blockLight,
                                                k -> Collections.synchronizedList(new ArrayList<>())).add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.groupedLights = newGroupedLights;
            this.allLightPositions = newAllLightPositions;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isScanning.set(false);
        }
    }

    private void onWorldRender(WorldRenderContext context) {
        if (!isEnabled())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null)
            return;

        Map<Integer, List<BlockPos>> currentGroupedLights = this.groupedLights;
        Set<BlockPos> currentAllLightPositions = this.allLightPositions;

        if (currentGroupedLights.isEmpty())
            return;

        Frustum frustum = context.frustum();
        if (frustum == null)
            return;


        Map<Integer, List<BlockPos>> visibleGroupedLights = new HashMap<>();
        boolean hasAnyVisible = false;

        for (Map.Entry<Integer, List<BlockPos>> entry : currentGroupedLights.entrySet()) {
            int lightLevel = entry.getKey();
            List<BlockPos> positions = entry.getValue();
            List<BlockPos> visibleInLevel = new ArrayList<>();

            for (BlockPos pos : positions) {
                if (frustum.isVisible(new Box(pos))) {
                    if (lightLevel >= 15 || shouldRenderBlock(pos, currentAllLightPositions)) {
                        visibleInLevel.add(pos);
                        hasAnyVisible = true;
                    }
                }
            }
            if (!visibleInLevel.isEmpty()) {
                visibleGroupedLights.put(lightLevel, visibleInLevel);
            }
        }

        if (hasAnyVisible) {
            MatrixStack ms = context.matrixStack();
            Vec3d camPos = context.camera().getPos();

            ms.push();
            ms.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f positionMatrix = ms.peek().getPositionMatrix();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR);

            for (Map.Entry<Integer, List<BlockPos>> entry : visibleGroupedLights.entrySet()) {
                int lightLevel = entry.getKey();
                List<BlockPos> positions = entry.getValue();
                float[] color = getGradientColor(lightLevel);
                float r = color[0], g = color[1], b = color[2], a = color[3];

                for (BlockPos pos : positions) {
                    float x = (float) pos.getX();
                    float y = (float) pos.getY();
                    float z = (float) pos.getZ();
                    renderExposedFacesAnyLightLevel(bufferBuilder, positionMatrix, x, y, z, pos,
                            currentAllLightPositions, r, g, b, a);
                }
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            ms.pop();
        }
    }

    private float[] getGradientColor(int lightLevel) {
        Color minColor = minLightColor.getValue();
        Color maxColor = maxLightColor.getValue();
        float t = (lightLevel - MIN_LIGHT_LEVEL) / (float) (15 - MIN_LIGHT_LEVEL);
        t = Math.max(0f, Math.min(1f, t));
        float r = lerp(minColor.getRed() / 255f, maxColor.getRed() / 255f, t);
        float g = lerp(minColor.getGreen() / 255f, maxColor.getGreen() / 255f, t);
        float b = lerp(minColor.getBlue() / 255f, maxColor.getBlue() / 255f, t);
        float a = lerp(minColor.getAlpha() / 255f, maxColor.getAlpha() / 255f, t);

        return new float[]{r, g, b, a};
    }

    private float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private void optimizeWeakLights(MinecraftClient client) {
        if (client.world == null || client.player == null)
            return;

        clearOldCacheEntries(client);

        ChunkPos playerChunkPos = new ChunkPos(client.player.getBlockPos());
        Set<BlockPos> weakLightSources = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();

        for (int chunkX = playerChunkPos.x - CHUNK_RADIUS; chunkX <= playerChunkPos.x + CHUNK_RADIUS; ++chunkX) {
            for (int chunkZ = playerChunkPos.z - CHUNK_RADIUS; chunkZ <= playerChunkPos.z + CHUNK_RADIUS; ++chunkZ) {
                WorldChunk chunk = client.world.getChunk(chunkX, chunkZ);
                if (chunk != null && chunk.getStatus().isAtLeast(ChunkStatus.FULL)) {
                    int startX = chunkX * 16;
                    int startZ = chunkZ * 16;

                    for (int x = 0; x < 16; ++x) {
                        for (int z = 0; z < 16; ++z) {
                            for (int y = MIN_Y; y <= MAX_Y; ++y) {
                                BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                                int blockLight = client.world.getLightLevel(LightType.BLOCK, pos);
                                int skyLight = client.world.getLightLevel(LightType.SKY, pos);
                                if ((blockLight == 6 || blockLight == 7) && blockLight > skyLight) {
                                    weakLightSources.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (BlockPos weakSource : weakLightSources) {
            if (!visited.contains(weakSource)) {
                int sourceLevel = client.world.getLightLevel(LightType.BLOCK, weakSource);
                int startLevel = sourceLevel + 1;
                if (!hasHigherLevelLightIn3x3Area(client, weakSource, startLevel)) {
                    propagateDeletionWithLevelFilter(client, weakSource, visited, sourceLevel);
                }
            }
        }
    }

    private boolean hasHigherLevelLightIn3x3Area(MinecraftClient client, BlockPos centerPos, int requiredLevel) {
        int startX = centerPos.getX() - 1;
        int endX = centerPos.getX() + 1;
        int startY = Math.max(MIN_Y, centerPos.getY() - 1);
        int endY = Math.min(MAX_Y, centerPos.getY() + 1);
        int startZ = centerPos.getZ() - 1;
        int endZ = centerPos.getZ() + 1;

        for (int x = startX; x <= endX; ++x) {
            for (int y = startY; y <= endY; ++y) {
                for (int z = startZ; z <= endZ; ++z) {
                    if (x != centerPos.getX() || y != centerPos.getY() || z != centerPos.getZ()) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        if (isWithinScanArea(client, checkPos)) {
                            int blockLight = client.world.getLightLevel(LightType.BLOCK, checkPos);
                            int skyLight = client.world.getLightLevel(LightType.SKY, checkPos);
                            if (blockLight >= requiredLevel && blockLight > skyLight) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void propagateDeletionWithLevelFilter(MinecraftClient client, BlockPos startPos, Set<BlockPos> visited,
                                                  int maxLevel) {
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            blocksToSkip.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (isWithinScanArea(client, neighbor) && !visited.contains(neighbor)
                        && !blocksToSkip.contains(neighbor)) {
                    int neighborLight = client.world.getLightLevel(LightType.BLOCK, neighbor);
                    int neighborSkyLight = client.world.getLightLevel(LightType.SKY, neighbor);
                    if (neighborLight >= MIN_LIGHT_LEVEL && neighborLight > neighborSkyLight
                            && neighborLight <= maxLevel) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    private void clearOldCacheEntries(MinecraftClient client) {
        if (client.player == null) {
            blocksToSkip.clear();
        } else {
            ChunkPos playerChunk = new ChunkPos(client.player.getBlockPos());
            Iterator<BlockPos> iterator = blocksToSkip.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                ChunkPos posChunk = new ChunkPos(pos);
                if (!(Math.abs(posChunk.x - playerChunk.x) > CHUNK_RADIUS + 1
                        || Math.abs(posChunk.z - playerChunk.z) > CHUNK_RADIUS + 1)) {
                    continue;
                }
                iterator.remove();
            }
        }
    }

    private boolean isWithinScanArea(MinecraftClient client, BlockPos pos) {
        if (client.player == null)
            return false;
        ChunkPos playerChunkPos = new ChunkPos(client.player.getBlockPos());
        int minX = (playerChunkPos.x - CHUNK_RADIUS) * 16;
        int maxX = (playerChunkPos.x + CHUNK_RADIUS + 1) * 16;
        int minZ = (playerChunkPos.z - CHUNK_RADIUS) * 16;
        int maxZ = (playerChunkPos.z + CHUNK_RADIUS + 1) * 16;
        if (pos.getX() >= minX && pos.getX() < maxX && pos.getZ() >= minZ && pos.getZ() < maxZ) {
            return pos.getY() >= MIN_Y && pos.getY() <= MAX_Y;
        } else {
            return false;
        }
    }
}
