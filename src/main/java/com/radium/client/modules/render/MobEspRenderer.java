package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.MobESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public final class MobEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final MobEspRenderer INSTANCE = new MobEspRenderer();

    private MobEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        MobESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(MobESP.class)
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

        for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(mod.range.getValue()), e -> !(e instanceof PlayerEntity))) {


            if (mc.player.distanceTo(entity) > mod.range.getValue()) continue;


            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            Box box = entity.getBoundingBox()
                    .offset(-entity.getX(), -entity.getY(), -entity.getZ())
                    .offset(x, y, z);

            if (mod.fill.getValue()) {
                RenderUtils.drawBox(matrices, box, mod.getFillColor(), false);
            }
            if (mod.outline.getValue()) {
                RenderUtils.drawBox(matrices, box, mod.getOutlineColor(), true);
            }

            if (mod.tracers.getValue()) {

                Vec3d targetPos = new Vec3d(x, y + entity.getHeight() / 2.0, z);
                RenderUtils.renderLine(matrices, new Color(mod.getOutlineColor()), centerScreenPos, targetPos);
            }
        }

        matrices.pop();
    }
}

