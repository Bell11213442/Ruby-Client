package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.BeehiveESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;

public final class BeehiveEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final BeehiveEspRenderer INSTANCE = new BeehiveEspRenderer();

    private BeehiveEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        BeehiveESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(BeehiveESP.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);

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

        renderBeehives(mod, mc, matrices, camPos, centerScreenPos, context.world());

        matrices.pop();
    }

    private void renderBeehives(BeehiveESP mod, MinecraftClient mc, MatrixStack matrices, Vec3d camPos, Vec3d centerScreenPos, ClientWorld world) {
        int viewDist = mc.options.getViewDistance().getValue();
        int pChunkX = mc.player.getBlockX() >> 4;
        int pChunkZ = mc.player.getBlockZ() >> 4;

        for (int cx = pChunkX - viewDist; cx <= pChunkX + viewDist; cx++) {
            for (int cz = pChunkZ - viewDist; cz <= pChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
                    if (mc.player.getPos().distanceTo(Vec3d.ofCenter(blockPos)) > mod.range.getValue()) continue;

                    BlockState blockState = world.getBlockState(blockPos);
                    Block block = blockState.getBlock();
                    if (block != Blocks.BEEHIVE && block != Blocks.BEE_NEST) continue;

                    if (!isValidBeehive(mod, blockState)) continue;

                    int honeyLevel = getHoneyLevel(blockState);
                    BlockPos pos = blockPos;

                    Color baseFillColor = new Color(mod.getFillColor(), true);
                    Color baseOutlineColor = new Color(mod.getOutlineColor(), true);
                    Color levelFillColor = getColorForLevel(baseFillColor, honeyLevel);
                    Color levelOutlineColor = getColorForLevel(baseOutlineColor, honeyLevel);

                    if (mod.fill.getValue() || mod.outline.getValue()) {
                        Box box = new Box(pos);

                        if (mod.fill.getValue()) {
                            RenderUtils.drawBox(matrices, box, levelFillColor.getRGB(), false);
                        }
                        if (mod.outline.getValue()) {
                            RenderUtils.drawBox(matrices, box, levelOutlineColor.getRGB(), true);
                        }
                    }

                    if (mod.tracers.getValue()) {
                        Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        RenderUtils.renderLine(matrices, new Color(mod.getTracerColor(), true), centerScreenPos, blockCenter);
                    }
                }
            }
        }
    }

    private boolean isValidBeehive(BeehiveESP mod, BlockState state) {
        Block block = state.getBlock();
        boolean isBeehive = mod.includeBeehives.getValue() && block == Blocks.BEEHIVE;
        boolean isBeeNest = mod.includeBeeNests.getValue() && block == Blocks.BEE_NEST;

        if (!isBeehive && !isBeeNest) return false;

        int honeyLevel = getHoneyLevel(state);

        if (honeyLevel == 0 && mod.includeLevel0.getValue()) return true;
        if (honeyLevel == 1 && mod.includeLevel1.getValue()) return true;
        if (honeyLevel == 2 && mod.includeLevel2.getValue()) return true;
        if (honeyLevel == 3 && mod.includeLevel3.getValue()) return true;
        if (honeyLevel == 4 && mod.includeLevel4.getValue()) return true;
        return honeyLevel == 5 && mod.includeLevel5.getValue();
    }

    private int getHoneyLevel(BlockState state) {
        try {
            if (state.contains(BeehiveBlock.HONEY_LEVEL)) {
                return state.get(BeehiveBlock.HONEY_LEVEL);
            }
        } catch (Exception e) {
        }
        return -1;
    }

    private Color getColorForLevel(Color baseColor, int honeyLevel) {
        float intensity;
        switch (honeyLevel) {
            case 0:
                intensity = 0.2f;
                break;
            case 1:
                intensity = 0.4f;
                break;
            case 2:
                intensity = 0.6f;
                break;
            case 3:
                intensity = 0.8f;
                break;
            case 4:
                intensity = 1.0f;
                break;
            case 5:
                return new Color(
                        Math.min(255, (int) (baseColor.getRed() * 1.3f)),
                        Math.min(255, (int) (baseColor.getGreen() * 1.3f)),
                        Math.min(255, (int) (baseColor.getBlue() * 1.1f)),
                        baseColor.getAlpha()
                );
            default:
                intensity = 1.0f;
        }
        return new Color(
                (int) (baseColor.getRed() * intensity),
                (int) (baseColor.getGreen() * intensity),
                (int) (baseColor.getBlue() * intensity),
                baseColor.getAlpha()
        );
    }
}

