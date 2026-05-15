package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.visual.HoleTunnelStairsESP;
import com.radium.client.modules.visual.HoleTunnelStairsESP.DetectionMode;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HoleTunnelStairsRenderer implements WorldRenderEvents.AfterEntities {
    private static final HoleTunnelStairsRenderer INSTANCE = new HoleTunnelStairsRenderer();
    private static final Direction[] DIRECTIONS = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};

    private final Map<Long, TChunk> chunks = new ConcurrentHashMap<>();
    private final Queue<Chunk> chunkQueue = new LinkedList<>();
    private final Set<Box> holes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Box> tunnels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Box> staircases = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Box> holes3x1 = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private int tickCounter = 0;

    private HoleTunnelStairsRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> INSTANCE.afterEntities(ctx));
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        HoleTunnelStairsESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(HoleTunnelStairsESP.class)
                : null;
        if (mod == null || !mod.isEnabled()) {
            clear();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;


        tickCounter++;
        if (tickCounter % 20 == 0) {
            updateChunks(mc.world, mod);
        }


        MatrixStack matrices = context.matrixStack();
        Vec3d cam = RenderUtils.getCameraPos();

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        DetectionMode mode = mod.getDetectionMode();

        switch (mode) {
            case ALL:
                renderHoles(matrices, mod);
                render3x1Holes(matrices, mod);
                renderTunnels(matrices, mod);
                renderStaircases(matrices, mod);
                break;
            case HOLES_AND_TUNNELS:
                renderHoles(matrices, mod);
                render3x1Holes(matrices, mod);
                renderTunnels(matrices, mod);
                break;
            case HOLES_AND_STAIRCASES:
                renderHoles(matrices, mod);
                render3x1Holes(matrices, mod);
                renderStaircases(matrices, mod);
                break;
            case TUNNELS_AND_STAIRCASES:
                renderTunnels(matrices, mod);
                renderStaircases(matrices, mod);
                break;
            case HOLES:
                renderHoles(matrices, mod);
                render3x1Holes(matrices, mod);
                break;
            case TUNNELS:
                renderTunnels(matrices, mod);
                break;
            case STAIRCASES:
                renderStaircases(matrices, mod);
                break;
            case HOLES_3X1_AND_TUNNELS:
                renderHoles(matrices, mod);
                render3x1Holes(matrices, mod);
                renderTunnels(matrices, mod);
                break;
        }

        matrices.pop();
    }

    private void updateChunks(ClientWorld world, HoleTunnelStairsESP mod) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;


        for (TChunk tChunk : chunks.values()) {
            tChunk.marked = false;
        }


        int viewDist = mc.options.getViewDistance().getValue();
        int pChunkX = mc.player.getBlockX() >> 4;
        int pChunkZ = mc.player.getBlockZ() >> 4;


        for (int cx = pChunkX - viewDist; cx <= pChunkX + viewDist; cx++) {
            for (int cz = pChunkZ - viewDist; cz <= pChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                long key = ChunkPos.toLong(cx, cz);
                if (chunks.containsKey(key)) {
                    chunks.get(key).marked = true;
                } else if (!chunkQueue.contains(chunk)) {
                    chunkQueue.add(chunk);
                }
            }
        }


        processChunkQueue(world, mod);


        chunks.values().removeIf(tChunk -> !tChunk.marked);


        removeBoxesOutsideRenderDistance(world);
    }

    private void processChunkQueue(ClientWorld world, HoleTunnelStairsESP mod) {
        int maxChunksPerTick = mod.getMaxChunks();
        int processed = 0;

        while (!chunkQueue.isEmpty() && processed < maxChunksPerTick) {
            Chunk chunk = chunkQueue.poll();
            if (chunk != null) {
                TChunk tChunk = new TChunk(chunk.getPos().x, chunk.getPos().z);
                chunks.put(tChunk.getKey(), tChunk);

                executor.execute(() -> searchChunk(world, chunk, mod));
                processed++;
            }
        }
    }

    private void searchChunk(ClientWorld world, Chunk chunk, HoleTunnelStairsESP mod) {
        ChunkSection[] sections = chunk.getSectionArray();
        int Ymin = world.getBottomY() + mod.getMinY();
        int Ymax = world.getTopY() - mod.getMaxY();
        int Y = world.getBottomY();

        for (ChunkSection section : sections) {
            if (section != null && !section.isEmpty()) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            int currentY = Y + y;
                            if (currentY <= Ymin || currentY >= Ymax) continue;

                            BlockPos pos = chunk.getPos().getBlockPos(x, currentY, z);
                            if (isPassableBlock(world, pos, mod.isAirBlocksOnly())) {
                                DetectionMode mode = mod.getDetectionMode();

                                switch (mode) {
                                    case ALL:
                                        checkHole(world, pos, mod);
                                        check3x1Hole(world, pos, mod);
                                        checkTunnel(world, pos, mod);
                                        if (mod.shouldDetectDiagonals()) checkDiagonalTunnel(world, pos, mod);
                                        checkStaircase(world, pos, mod);
                                        break;
                                    case HOLES_AND_TUNNELS:
                                    case HOLES_3X1_AND_TUNNELS:
                                        checkHole(world, pos, mod);
                                        check3x1Hole(world, pos, mod);
                                        checkTunnel(world, pos, mod);
                                        if (mod.shouldDetectDiagonals()) checkDiagonalTunnel(world, pos, mod);
                                        break;
                                    case HOLES_AND_STAIRCASES:
                                        checkHole(world, pos, mod);
                                        check3x1Hole(world, pos, mod);
                                        checkStaircase(world, pos, mod);
                                        break;
                                    case TUNNELS_AND_STAIRCASES:
                                        checkTunnel(world, pos, mod);
                                        if (mod.shouldDetectDiagonals()) checkDiagonalTunnel(world, pos, mod);
                                        checkStaircase(world, pos, mod);
                                        break;
                                    case HOLES:
                                        checkHole(world, pos, mod);
                                        check3x1Hole(world, pos, mod);
                                        break;
                                    case TUNNELS:
                                        checkTunnel(world, pos, mod);
                                        if (mod.shouldDetectDiagonals()) checkDiagonalTunnel(world, pos, mod);
                                        break;
                                    case STAIRCASES:
                                        checkStaircase(world, pos, mod);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            Y += 16;
        }
    }

    private void checkHole(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {
        if (isValidHoleSection(world, pos, mod.isAirBlocksOnly())) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            while (isValidHoleSection(world, currentPos, mod.isAirBlocksOnly())) {
                currentPos.move(Direction.UP);
            }
            if (currentPos.getY() - pos.getY() >= mod.getMinHoleDepth()) {
                Box holeBox = new Box(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, currentPos.getY(), pos.getZ() + 1
                );
                if (!holes.contains(holeBox) && holes.stream().noneMatch(h -> h.intersects(holeBox))) {
                    holes.add(holeBox);
                }
            }
        }
    }

    private void check3x1Hole(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {

        if (isValid3x1HoleSectionX(world, pos, mod.isAirBlocksOnly())) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            while (isValid3x1HoleSectionX(world, currentPos, mod.isAirBlocksOnly())) {
                currentPos.move(Direction.UP);
            }
            if (currentPos.getY() - pos.getY() >= mod.getMinHoleDepth()) {
                Box holeBox = new Box(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 3, currentPos.getY(), pos.getZ() + 1
                );
                if (!holes3x1.contains(holeBox) && holes3x1.stream().noneMatch(h -> h.intersects(holeBox))) {
                    holes3x1.add(holeBox);
                }
            }
        }


        if (isValid3x1HoleSectionZ(world, pos, mod.isAirBlocksOnly())) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            while (isValid3x1HoleSectionZ(world, currentPos, mod.isAirBlocksOnly())) {
                currentPos.move(Direction.UP);
            }
            if (currentPos.getY() - pos.getY() >= mod.getMinHoleDepth()) {
                Box holeBox = new Box(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, currentPos.getY(), pos.getZ() + 3
                );
                if (!holes3x1.contains(holeBox) && holes3x1.stream().noneMatch(h -> h.intersects(holeBox))) {
                    holes3x1.add(holeBox);
                }
            }
        }
    }

    private void checkTunnel(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            int stepCount = 0;
            BlockPos startPos = null;
            BlockPos endPos = null;
            int maxHeight = 0;

            if (isTunnelSection(world, currentPos, dir, mod)) {
                startPos = currentPos.toImmutable();
            }

            while (isTunnelSection(world, currentPos, dir, mod)) {
                maxHeight = Math.max(maxHeight, getTunnelHeight(world, currentPos, mod));
                endPos = currentPos.toImmutable();
                currentPos.move(dir);
                stepCount++;
            }

            if (stepCount >= mod.getMinTunnelLength() && maxHeight >= mod.getMinTunnelHeight() && maxHeight <= mod.getMaxTunnelHeight()) {
                Box tunnelBox = new Box(
                        Math.min(startPos.getX(), endPos.getX()),
                        startPos.getY(),
                        Math.min(startPos.getZ(), endPos.getZ()),
                        Math.max(startPos.getX(), endPos.getX()) + 1,
                        startPos.getY() + maxHeight,
                        Math.max(startPos.getZ(), endPos.getZ()) + 1
                );

                if (!tunnels.contains(tunnelBox) && tunnels.stream().noneMatch(t -> t.intersects(tunnelBox))) {
                    tunnels.add(tunnelBox);
                }
            }
        }
    }

    private void checkDiagonalTunnel(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {

        for (Direction dir : DIRECTIONS) {
            for (int width = mod.getMinDiagonalWidth(); width < mod.getMaxDiagonalWidth(); width++) {
                BlockPos.Mutable currentPos = pos.mutableCopy();
                int stepCount = 0;
                List<Box> potentialBoxes = new ArrayList<>();

                while (isDiagonalTunnelSection(world, currentPos, dir, mod)) {
                    int height = getTunnelHeight(world, currentPos, mod);
                    Box tunnelBox = new Box(
                            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                            currentPos.getX() + 1, currentPos.getY() + height, currentPos.getZ() + 1
                    );
                    potentialBoxes.add(tunnelBox);
                    currentPos.move(dir);
                    stepCount++;
                }

                if (stepCount >= mod.getMinDiagonalLength()) {
                    potentialBoxes.forEach(box -> {
                        if (!tunnels.contains(box) && tunnels.stream().noneMatch(t -> t.intersects(box))) {
                            tunnels.add(box);
                        }
                    });
                }
            }
        }
    }

    private void checkStaircase(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.Mutable currentPos = pos.mutableCopy();
            int stepCount = 0;
            List<Box> potentialBoxes = new ArrayList<>();

            while (isStaircaseSection(world, currentPos, dir, mod)) {
                int height = getStaircaseHeight(world, currentPos, mod);
                Box stairsBox = new Box(
                        currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                        currentPos.getX() + 1, currentPos.getY() + height, currentPos.getZ() + 1
                );
                potentialBoxes.add(stairsBox);
                currentPos.move(dir);
                currentPos.move(Direction.UP);
                stepCount++;
            }

            if (stepCount >= mod.getMinStaircaseLength()) {
                potentialBoxes.forEach(box -> {
                    if (!staircases.contains(box) && staircases.stream().noneMatch(s -> s.intersects(box))) {
                        staircases.add(box);
                    }
                });
            }
        }
    }


    private boolean isValidHoleSection(ClientWorld world, BlockPos pos, boolean airOnly) {
        return isPassableBlock(world, pos, airOnly) &&
                !isPassableBlock(world, pos.north(), airOnly) &&
                !isPassableBlock(world, pos.south(), airOnly) &&
                !isPassableBlock(world, pos.east(), airOnly) &&
                !isPassableBlock(world, pos.west(), airOnly);
    }

    private boolean isValid3x1HoleSectionX(ClientWorld world, BlockPos pos, boolean airOnly) {
        return isPassableBlock(world, pos, airOnly) &&
                isPassableBlock(world, pos.east(), airOnly) &&
                isPassableBlock(world, pos.east(2), airOnly) &&
                !isPassableBlock(world, pos.north(), airOnly) &&
                !isPassableBlock(world, pos.south(), airOnly) &&
                !isPassableBlock(world, pos.east(3), airOnly) &&
                !isPassableBlock(world, pos.west(), airOnly);
    }

    private boolean isValid3x1HoleSectionZ(ClientWorld world, BlockPos pos, boolean airOnly) {
        return isPassableBlock(world, pos, airOnly) &&
                isPassableBlock(world, pos.south(), airOnly) &&
                isPassableBlock(world, pos.south(2), airOnly) &&
                !isPassableBlock(world, pos.east(), airOnly) &&
                !isPassableBlock(world, pos.west(), airOnly) &&
                !isPassableBlock(world, pos.south(3), airOnly) &&
                !isPassableBlock(world, pos.north(), airOnly);
    }

    private boolean isTunnelSection(ClientWorld world, BlockPos pos, Direction dir, HoleTunnelStairsESP mod) {
        int height = getTunnelHeight(world, pos, mod);
        if (height < mod.getMinTunnelHeight() || height > mod.getMaxTunnelHeight()) return false;
        if (isPassableBlock(world, pos.down(), mod.isAirBlocksOnly()) ||
                isPassableBlock(world, pos.up(height), mod.isAirBlocksOnly())) return false;

        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ?
                new Direction[]{Direction.NORTH, Direction.SOUTH} :
                new Direction[]{Direction.EAST, Direction.WEST};

        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(world, pos.up(i).offset(perpDir), mod.isAirBlocksOnly())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isDiagonalTunnelSection(ClientWorld world, BlockPos pos, Direction dir, HoleTunnelStairsESP mod) {
        int height = getTunnelHeight(world, pos, mod);
        if (height < mod.getMinTunnelHeight() || height > mod.getMaxTunnelHeight()) return false;
        if (isPassableBlock(world, pos.down(), mod.isAirBlocksOnly()) ||
                isPassableBlock(world, pos.up(height), mod.isAirBlocksOnly())) return false;

        for (int i = 0; i < height; i++) {
            if (isPassableBlock(world, pos.up(i).offset(dir), mod.isAirBlocksOnly())) {
                return false;
            }
        }
        return true;
    }

    private boolean isStaircaseSection(ClientWorld world, BlockPos pos, Direction dir, HoleTunnelStairsESP mod) {
        int height = getStaircaseHeight(world, pos, mod);
        if (height < mod.getMinStaircaseHeight() || height > mod.getMaxStaircaseHeight()) return false;
        if (isPassableBlock(world, pos.down(), mod.isAirBlocksOnly()) ||
                isPassableBlock(world, pos.up(height), mod.isAirBlocksOnly())) return false;

        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ?
                new Direction[]{Direction.NORTH, Direction.SOUTH} :
                new Direction[]{Direction.EAST, Direction.WEST};

        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(world, pos.up(i).offset(perpDir), mod.isAirBlocksOnly())) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getTunnelHeight(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {
        int height = 0;
        while (isPassableBlock(world, pos.up(height), mod.isAirBlocksOnly()) &&
                height < mod.getMaxTunnelHeight()) {
            height++;
        }
        return height;
    }

    private int getStaircaseHeight(ClientWorld world, BlockPos pos, HoleTunnelStairsESP mod) {
        int height = 0;
        while (isPassableBlock(world, pos.up(height), mod.isAirBlocksOnly()) &&
                height < mod.getMaxStaircaseHeight()) {
            height++;
        }
        return height;
    }

    private boolean isPassableBlock(ClientWorld world, BlockPos pos, boolean airOnly) {
        BlockState state = world.getBlockState(pos);
        if (airOnly) {
            return state.isAir();
        } else {
            VoxelShape shape = state.getCollisionShape(world, pos);
            return shape.isEmpty() || !VoxelShapes.fullCube().equals(shape);
        }
    }

    private void removeBoxesOutsideRenderDistance(ClientWorld world) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Set<WorldChunk> worldChunks = new HashSet<>();
        int viewDist = mc.options.getViewDistance().getValue();
        int pChunkX = mc.player.getBlockX() >> 4;
        int pChunkZ = mc.player.getBlockZ() >> 4;

        for (int cx = pChunkX - viewDist; cx <= pChunkX + viewDist; cx++) {
            for (int cz = pChunkZ - viewDist; cz <= pChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk != null) worldChunks.add(chunk);
            }
        }

        holes.removeIf(box -> !isBoxInChunks(world, box, worldChunks));
        tunnels.removeIf(box -> !isBoxInChunks(world, box, worldChunks));
        staircases.removeIf(box -> !isBoxInChunks(world, box, worldChunks));
        holes3x1.removeIf(box -> !isBoxInChunks(world, box, worldChunks));
    }

    private boolean isBoxInChunks(ClientWorld world, Box box, Set<WorldChunk> chunks) {
        BlockPos center = new BlockPos(
                (int) Math.floor(box.getCenter().getX()),
                (int) Math.floor(box.getCenter().getY()),
                (int) Math.floor(box.getCenter().getZ())
        );
        return chunks.contains(world.getChunk(center));
    }


    private void renderHoles(MatrixStack matrices, HoleTunnelStairsESP mod) {
        for (Box box : holes) {
            if (mod.shouldFill()) {
                RenderUtils.drawBox(matrices, box, mod.getHoleSideColor(), false);
            }
            if (mod.shouldOutline()) {
                RenderUtils.drawBox(matrices, box, mod.getHoleLineColor(), true);
            }
        }
    }

    private void render3x1Holes(MatrixStack matrices, HoleTunnelStairsESP mod) {
        for (Box box : holes3x1) {
            if (mod.shouldFill()) {
                RenderUtils.drawBox(matrices, box, mod.getHole3x1SideColor(), false);
            }
            if (mod.shouldOutline()) {
                RenderUtils.drawBox(matrices, box, mod.getHole3x1LineColor(), true);
            }
        }
    }

    private void renderTunnels(MatrixStack matrices, HoleTunnelStairsESP mod) {
        for (Box box : tunnels) {
            if (mod.shouldFill()) {
                RenderUtils.drawBox(matrices, box, mod.getTunnelSideColor(), false);
            }
            if (mod.shouldOutline()) {
                RenderUtils.drawBox(matrices, box, mod.getTunnelLineColor(), true);
            }
        }
    }

    private void renderStaircases(MatrixStack matrices, HoleTunnelStairsESP mod) {
        for (Box box : staircases) {
            if (mod.shouldFill()) {
                RenderUtils.drawBox(matrices, box, mod.getStaircaseSideColor(), false);
            }
            if (mod.shouldOutline()) {
                RenderUtils.drawBox(matrices, box, mod.getStaircaseLineColor(), true);
            }
        }
    }

    private void clear() {
        chunks.clear();
        chunkQueue.clear();
        holes.clear();
        tunnels.clear();
        staircases.clear();
        holes3x1.clear();
    }

    private static class TChunk {
        private final int x, z;
        public boolean marked;

        public TChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.marked = true;
        }

        public long getKey() {
            return ChunkPos.toLong(x, z);
        }
    }
}
