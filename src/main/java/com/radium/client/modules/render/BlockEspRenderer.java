package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.visual.BlockESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Map;

public final class BlockEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final BlockEspRenderer INSTANCE = new BlockEspRenderer();
    private static final int MAX_BLOCKS_RENDER = 500;
    private static final double MAX_RENDER_DISTANCE = 150.0;

    private BlockEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        BlockESP mod = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(BlockESP.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        renderBlocks(matrices, mod, mc, camPos);

        matrices.pop();
    }

    private void renderBlocks(MatrixStack matrices, BlockESP mod, MinecraftClient mc, Vec3d camPos) {
        int rendered = 0;
        Color blockColor = mod.getBlockColor();

        for (Map.Entry<BlockPos, net.minecraft.block.Block> entry : mod.getDetectedBlocks().entrySet()) {
            if (rendered >= MAX_BLOCKS_RENDER) break;

            BlockPos pos = entry.getKey();

            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
            if (distance > MAX_RENDER_DISTANCE) continue;

            RenderUtils.renderFilledBox(matrices,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    blockColor);

            rendered++;
        }
    }
}


