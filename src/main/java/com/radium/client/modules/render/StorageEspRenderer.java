package com.radium.client.modules.render;
// radium client

import com.mojang.blaze3d.systems.RenderSystem;
import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.StorageESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public final class StorageEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final StorageEspRenderer INSTANCE = new StorageEspRenderer();

    private StorageEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> INSTANCE.afterEntities(ctx));
        WorldRenderEvents.LAST.register(ctx -> INSTANCE.afterEntities(ctx));
    }

    private static boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity
                || be instanceof EnderChestBlockEntity
                || be instanceof ShulkerBoxBlockEntity
                || be instanceof BarrelBlockEntity
                || be instanceof DispenserBlockEntity
                || be instanceof HopperBlockEntity
                || be instanceof MobSpawnerBlockEntity
                || be instanceof AbstractFurnaceBlockEntity;
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        StorageESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(StorageESP.class)
                : null;
        if (mod == null || !mod.isEnabled()) return;
        if (MinecraftClient.getInstance().world == null) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);

        MinecraftClient mc = MinecraftClient.getInstance();
        Freecam freecam = RadiumClient.moduleManager.getModule(Freecam.class);
        Vec3d lookVec;
        if (freecam != null && freecam.isEnabled()) {
            lookVec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        } else {
            lookVec = mc.player.getRotationVec(tickDelta);
        }
        Vec3d centerScreenPos = camPos.add(lookVec.multiply(10.0));


        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        ClientWorld world = context.world();
        int viewDist = MinecraftClient.getInstance().options.getViewDistance().getValue();
        int pChunkX = MinecraftClient.getInstance().player.getBlockX() >> 4;
        int pChunkZ = MinecraftClient.getInstance().player.getBlockZ() >> 4;
        for (int cx = pChunkX - viewDist; cx <= pChunkX + viewDist; cx++) {
            for (int cz = pChunkZ - viewDist; cz <= pChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!isStorage(be)) continue;

                    BlockPos pos = be.getPos();

                    String name = be.getCachedState().getBlock().getTranslationKey();
                    if (!mod.shouldHighlight(name)) continue;

                    BlockState state = be.getCachedState();
                    VoxelShape shape = state.getOutlineShape(world, pos);
                    List<Box> boxes = shape.getBoundingBoxes();
                    if (boxes.isEmpty()) {
                        Box fallback = new Box(pos);
                        if (mod.shouldFill()) RenderUtils.drawBox(matrices, fallback, mod.getFillColor(name), false);
                        if (mod.shouldOutline())
                            RenderUtils.drawBox(matrices, fallback, mod.getOutlineColor(name), true);
                    } else {
                        for (Box part : boxes) {

                            Box partWorld = part.expand(0.002).offset(pos);
                            if (mod.shouldFill())
                                RenderUtils.drawBox(matrices, partWorld, mod.getFillColor(name), false);
                            if (mod.shouldOutline())
                                RenderUtils.drawBox(matrices, partWorld, mod.getOutlineColor(name), true);
                        }
                    }

                    if (mod.shouldTracers()) {

                        Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        int tracerColor = mod.getOutlineColor(name);
                        RenderUtils.renderLine(matrices, new Color(tracerColor), centerScreenPos, blockCenter);
                    }
                }
            }
        }

        matrices.pop();
    }
}

final class RenderTracer {
    private RenderTracer() {
    }

    static void line(MatrixStack matrices, Vec3d from, Vec3d to, int argb) {
        float a = (argb >>> 24 & 0xFF) / 255f;
        float r = (argb >>> 16 & 0xFF) / 255f;
        float g = (argb >>> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        buf.vertex(m, (float) from.x, (float) from.y, (float) from.z).color(r, g, b, a);
        buf.vertex(m, (float) to.x, (float) to.y, (float) to.z).color(r, g, b, a);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }
}
