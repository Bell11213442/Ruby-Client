package com.radium.client.modules.visual;
import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.utils.BlockUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class ClusterFinder extends Module implements WorldRenderEvents.AfterEntities {
    private final NumberSetting alpha = new NumberSetting("Alpha", 1.0, 255.0, 125.0, 1.0);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", true);

    private final Set<BlockPos> clusters = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final ExecutorService scanExecutor = Executors.newFixedThreadPool(4);

    public ClusterFinder() {
        super("Cluster ESP", "Shows Amethyst Clusters", Category.VISUAL);
        this.addSettings(this.alpha, tracers);
        register(this);
    }

    public static void register(ClusterFinder inst) {
        WorldRenderEvents.AFTER_ENTITIES.register(inst);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clusters.clear();
        scannedChunks.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clusters.clear();
        scannedChunks.clear();
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null)
            return;

        // Scan and Cleanup every 20 ticks
        if (mc.player.age % 20 == 0) {
            java.util.List<WorldChunk> loadedChunks = BlockUtil.getLoadedChunks().toList();
            Set<ChunkPos> loadedPositions = loadedChunks.stream()
                    .map(WorldChunk::getPos)
                    .collect(Collectors.toSet());

            // Scan new chunks
            for (WorldChunk chunk : loadedChunks) {
                if (scannedChunks.add(chunk.getPos())) {
                    scanExecutor.submit(() -> scanChunk(chunk));
                }
            }

            // Cleanup clusters and scanned chunks that are no longer "loaded"
            clusters.removeIf(pos -> !loadedPositions.contains(new ChunkPos(pos)));
            scannedChunks.removeIf(cp -> !loadedPositions.contains(cp));
        }
    }

    private void scanChunk(WorldChunk chunk) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        // Minecraft geodes generate between -64 and 30. Limiting scan range for speed.
        int minY = Math.max(mc.world.getBottomY(), -64);
        int maxY = Math.min(320, 64);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    mutable.set(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    if (chunk.getBlockState(mutable).isOf(Blocks.AMETHYST_CLUSTER)) {
                        clusters.add(mutable.toImmutable());
                    }
                }
            }
        }
    }

    public void renderClusters(MatrixStack matrices) {
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null)
            return;

        Vec3d camPos = cam.getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        int color = new Color(200, 100, 255, alpha.getValue().intValue()).getRGB();

        for (BlockPos pos : clusters) {
            Box box = new Box(pos);
            RenderUtils.drawBox(matrices, box, color, true);
        }

        matrices.pop();
    }

    public void renderTracers(WorldRenderContext ctx, MatrixStack matrices) {
        if (!this.tracers.getValue() || clusters.isEmpty())
            return;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null)
            return;

        Vec3d camPos = cam.getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Color color = new Color(200, 100, 255, 255);

        Vec3d origin = (mc.crosshairTarget != null && mc.crosshairTarget.getPos() != null)
                ? mc.crosshairTarget.getPos()
                : camPos;

        for (BlockPos pos : clusters) {
            Vec3d lookVec;
            float tickDelta = ctx.tickCounter().getTickDelta(true);
            Freecam freecam = RadiumClient.moduleManager.getModule(Freecam.class);
            if (freecam != null && freecam.isEnabled()) {
                lookVec = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
            } else {
                lookVec = mc.player.getRotationVec(tickDelta);
            }
            Vec3d centerScreenPos = camPos.add(lookVec.multiply(10.0));
            Vec3d targetPos = new Vec3d(pos.getX(), pos.getY() + mc.player.getHeight() / 2.0, pos.getZ());
            RenderUtils.renderLine(matrices, color, centerScreenPos, targetPos);
        }

        matrices.pop();
    }

    @Override
    public void afterEntities(WorldRenderContext worldRenderContext) {
        if(this.isEnabled() == false || this == null) {
            return;
        }
        renderClusters(worldRenderContext.matrixStack());
        renderTracers(worldRenderContext, worldRenderContext.matrixStack());
    }
}

