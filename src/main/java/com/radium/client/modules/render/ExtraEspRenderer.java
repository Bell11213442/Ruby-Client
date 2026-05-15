package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.ExtraESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Map;

public final class ExtraEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final ExtraEspRenderer INSTANCE = new ExtraEspRenderer();
    private static final int MAX_BLOCKS_RENDER = 300;
    private static final double MAX_RENDER_DISTANCE = 150.0;

    private ExtraEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        ExtraESP mod = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(ExtraESP.class)
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

        renderBlocks(matrices, mod, mc, camPos, tickDelta, centerScreenPos);
        renderEntities(matrices, mod, mc, camPos, tickDelta, centerScreenPos);

        matrices.pop();
    }

    private void renderBlocks(MatrixStack matrices, ExtraESP mod, MinecraftClient mc, Vec3d camPos, float tickDelta, Vec3d centerScreenPos) {
        int rendered = 0;

        for (Map.Entry<BlockPos, ExtraESP.ESPBlockType> entry : mod.getDetectedBlocks().entrySet()) {
            if (rendered >= MAX_BLOCKS_RENDER) break;

            BlockPos pos = entry.getKey();
            ExtraESP.ESPBlockType type = entry.getValue();

            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
            if (distance > MAX_RENDER_DISTANCE) continue;

            Color color = mod.getColorForBlockType(type);
            if (color == null) continue;

            RenderUtils.renderFilledBox(matrices,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    color);

            if (mod.isTracers()) {

                Vec3d blockCenter = Vec3d.ofCenter(pos);
                RenderUtils.renderLine(matrices, color, centerScreenPos, blockCenter);
            }

            rendered++;
        }
    }

    private void renderEntities(MatrixStack matrices, ExtraESP mod, MinecraftClient mc, Vec3d camPos, float tickDelta, Vec3d centerScreenPos) {
        for (Map.Entry<Entity, ExtraESP.ESPEntityType> entry : mod.getDetectedEntities().entrySet()) {
            Entity entity = entry.getKey();
            ExtraESP.ESPEntityType type = entry.getValue();

            double distance = mc.player.distanceTo(entity);
            if (distance > MAX_RENDER_DISTANCE) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            Box box = entity.getBoundingBox()
                    .offset(-entity.getX(), -entity.getY(), -entity.getZ())
                    .offset(x, y, z);

            Color color = mod.getColorForEntityType(type);
            if (color == null) continue;

            Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
            Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 200);

            RenderUtils.drawBox(matrices, box, fillColor.getRGB(), false);
            RenderUtils.drawBox(matrices, box, outlineColor.getRGB(), true);

            if (mod.isTracers()) {

                Vec3d entityCenter = new Vec3d(x, y + entity.getHeight() / 2.0, z);
                RenderUtils.renderLine(matrices, outlineColor, centerScreenPos, entityCenter);
            }
        }
    }

}
