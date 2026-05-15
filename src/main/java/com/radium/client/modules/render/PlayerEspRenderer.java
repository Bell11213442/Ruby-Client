package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.PlayerESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public final class PlayerEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final PlayerEspRenderer INSTANCE = new PlayerEspRenderer();

    private PlayerEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        PlayerESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(PlayerESP.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

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

        renderPlayers(mod, mc, matrices, centerScreenPos, tickDelta);

        matrices.pop();
    }

    private void renderPlayers(PlayerESP mod, MinecraftClient mc, MatrixStack matrices, Vec3d centerScreenPos, float tickDelta) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (mod.ignoreSelf.getValue() && player == mc.player) {
                continue;
            }

            if (mc.player.distanceTo(player) > mod.range.getValue()) {
                continue;
            }


            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());

            Box box = player.getBoundingBox()
                    .offset(-player.getX(), -player.getY(), -player.getZ())
                    .offset(x, y, z);

            if (mod.outline.getValue()) {
                Box glowBox = box.expand(0.3);
                Color outlineColor = new Color(mod.getOutlineColor());
                int glowColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), 80).getRGB();
                RenderUtils.drawBox(matrices, glowBox, glowColor, true);
            }

            if (mod.fill.getValue()) {
                RenderUtils.drawBox(matrices, box, mod.getFillColor(), false);
            }
            if (mod.outline.getValue()) {
                RenderUtils.drawBox(matrices, box, mod.getOutlineColor(), true);
            }
            if (mod.tracers.getValue()) {

                Vec3d targetPos = new Vec3d(x, y + player.getHeight() / 2.0, z);
                RenderUtils.renderLine(matrices, new Color(mod.getOutlineColor()), centerScreenPos, targetPos);
            }
        }
    }
}
