package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.CrystalESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public final class CrystalEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final CrystalEspRenderer INSTANCE = new CrystalEspRenderer();

    private CrystalEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        CrystalESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(CrystalESP.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);
        VertexConsumerProvider vertexConsumers = context.consumers();

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

        renderCrystals(mod, mc, matrices, camPos, tickDelta, camera, vertexConsumers, centerScreenPos);

        matrices.pop();


        if (vertexConsumers instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) vertexConsumers).draw();
        }
    }

    private void renderCrystals(CrystalESP mod, MinecraftClient mc, MatrixStack matrices, Vec3d camPos, float tickDelta, Camera camera, VertexConsumerProvider vertexConsumers, Vec3d centerScreenPos) {
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(mod.range.getValue()), e -> true)) {
            if (mc.player.distanceTo(crystal) > mod.range.getValue()) continue;


            double x = MathHelper.lerp(tickDelta, crystal.lastRenderX, crystal.getX());
            double y = MathHelper.lerp(tickDelta, crystal.lastRenderY, crystal.getY());
            double z = MathHelper.lerp(tickDelta, crystal.lastRenderZ, crystal.getZ());

            Box box = crystal.getBoundingBox()
                    .offset(-crystal.getX(), -crystal.getY(), -crystal.getZ())
                    .offset(x, y, z);


            if (mod.fill.getValue()) {
                RenderUtils.drawBox(matrices, box, mod.getFillColor(), false);
            }
            if (mod.outline.getValue()) {
                RenderUtils.drawBox(matrices, box, mod.getOutlineColor(), true);
            }


            if (mod.tracers.getValue()) {
                Vec3d end = new Vec3d(x, y + crystal.getHeight() / 2.0, z);
                RenderUtils.renderLine(matrices, new Color(mod.getOutlineColor(), true), centerScreenPos, end);
            }


            if (mod.showDamage.getValue()) {
                double playerDistance = mc.player.distanceTo(crystal);
                float playerDamage = calculateCrystalDamage(playerDistance);


                PlayerEntity nearestEnemy = findNearestEnemy(mc, crystal);
                float enemyDamage = 0.0f;
                if (nearestEnemy != null) {
                    double enemyDistance = nearestEnemy.distanceTo(crystal);
                    enemyDamage = calculateCrystalDamage(enemyDistance);
                }

                renderDamageText(matrices, mc, camera, x, y + crystal.getHeight() + 0.5, z, playerDamage, enemyDamage, playerDistance, vertexConsumers);
            }
        }
    }

    private PlayerEntity findNearestEnemy(MinecraftClient mc, EndCrystalEntity crystal) {
        PlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            double distance = player.distanceTo(crystal);
            if (distance < nearestDistance && distance <= 12.0) {
                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private float calculateCrystalDamage(double distance) {


        if (distance >= 12.0) {
            return 0.0f;
        }

        float baseDamage = 6.0f;
        float distanceFactor = (float) (1.0 - distance / 12.0);
        float rawDamage = baseDamage * distanceFactor;


        return Math.max(0.0f, rawDamage);
    }

    private void renderDamageText(MatrixStack matrices, MinecraftClient mc, Camera camera, double x, double y, double z, float playerDamage, float enemyDamage, double playerDistance, VertexConsumerProvider vertexConsumers) {
        TextRenderer textRenderer = mc.textRenderer;
        float textScale = 0.025f;
        float lineSpacing = 12.0f;


        String playerText = String.format("You: %.1f", playerDamage);
        if (playerDistance >= 12.0) {
            playerText = "You: 0.0";
        }

        String enemyText = enemyDamage > 0.0f ? String.format("Enemy: %.1f", enemyDamage) : "Enemy: -";


        int playerColor = getDamageColor(playerDamage);
        int enemyColor = enemyDamage > 0.0f ? getDamageColor(enemyDamage) : 0xFF808080;

        matrices.push();


        matrices.translate(x, y, z);


        matrices.multiply(camera.getRotation());
        matrices.scale(-textScale, -textScale, textScale);


        float playerTextWidth = textRenderer.getWidth(playerText);
        float enemyTextWidth = textRenderer.getWidth(enemyText);
        float maxWidth = Math.max(playerTextWidth, enemyTextWidth);
        matrices.translate(-maxWidth / 2.0f, -lineSpacing / 2.0f, 0);


        Matrix4f matrix = matrices.peek().getPositionMatrix();
        textRenderer.draw(playerText, 0, 0, playerColor | 0xFF000000, false, matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 15728880);


        matrices.translate(0, lineSpacing, 0);
        matrix = matrices.peek().getPositionMatrix();
        textRenderer.draw(enemyText, 0, 0, enemyColor | 0xFF000000, false, matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 15728880);

        matrices.pop();
    }

    private int getDamageColor(float damage) {

        if (damage >= 4.0f) {
            return 0xFFFF0000;
        } else if (damage >= 2.0f) {
            return 0xFFFFFF00;
        } else {
            return 0xFF00FF00;
        }
    }
}

