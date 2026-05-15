package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.SchematicBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public final class SchematicBuilderRenderer implements WorldRenderEvents.AfterEntities {
    private static final SchematicBuilderRenderer INSTANCE = new SchematicBuilderRenderer();
    private static final double MAX_RENDER_DISTANCE = 200.0;

    private SchematicBuilderRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        SchematicBuilder mod = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(SchematicBuilder.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Only render if schematic is loaded
        if (!mod.hasSchematicLoaded() || mod.getOrigin() == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        renderPreview(matrices, mod, mc, camPos);

        matrices.pop();
    }

    private void renderPreview(MatrixStack matrices, SchematicBuilder mod,
                               MinecraftClient mc, Vec3d camPos) {
        BlockPos origin = mod.getOrigin();
        if (origin == null) return;

        int currentLayer = mod.getCurrentLayer();

        // Render layer-by-layer preview
        for (BlockPos relativePos : mod.getSchematicBlocks().keySet()) {
            BlockPos worldPos = origin.add(relativePos);

            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(worldPos));
            if (distance > MAX_RENDER_DISTANCE) continue;

            int blockY = relativePos.getY();
            Color color;

            // Determine color based on layer and placement status
            if (mod.isBlockPlaced(worldPos)) {
                // Already placed - green
                color = new Color(0, 255, 0, 100);
            } else if (blockY == currentLayer) {
                // Current layer - bright cyan/yellow
                color = new Color(0, 255, 255, 150);
            } else if (blockY < currentLayer) {
                // Past layers (should be placed but aren't) - red
                color = new Color(255, 0, 0, 120);
            } else {
                // Future layers - gray/transparent
                int alpha = Math.max(30, 150 - (blockY - currentLayer) * 20);
                color = new Color(128, 128, 128, alpha);
            }

            RenderUtils.renderFilledBox(matrices,
                    worldPos.getX(), worldPos.getY(), worldPos.getZ(),
                    worldPos.getX() + 1, worldPos.getY() + 1, worldPos.getZ() + 1,
                    color);
        }
    }
}

