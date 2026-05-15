package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.ItemESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class ItemEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final ItemEspRenderer INSTANCE = new ItemEspRenderer();

    private ItemEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        ItemESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(ItemESP.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d camPos = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);

        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            double dist = mc.player.distanceTo(itemEntity);
            if (dist > mod.range.getValue()) continue;

            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty()) continue;

            double x = MathHelper.lerp(tickDelta, itemEntity.lastRenderX, itemEntity.getX()) - camPos.x;
            double y = MathHelper.lerp(tickDelta, itemEntity.lastRenderY, itemEntity.getY()) - camPos.y + 1.0;
            double z = MathHelper.lerp(tickDelta, itemEntity.lastRenderZ, itemEntity.getZ()) - camPos.z;

            renderItemLabel(mod, mc, matrices, context.consumers(), context.camera(), x, y, z, stack);
        }
    }

    private void renderItemLabel(ItemESP mod, MinecraftClient mc, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, net.minecraft.client.render.Camera camera,
                                 double x, double y, double z, ItemStack stack) {

        matrices.push();
        matrices.translate(x, y, z);


        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        float scale = mod.scale.getValue().floatValue() * 0.025f;
        matrices.scale(-scale, -scale, scale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        TextRenderer tr = mc.textRenderer;
        ItemRenderer ir = mc.getItemRenderer();

        int yOffset = 0;


        matrices.push();
        matrices.scale(16f, 16f, 16f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        ir.renderItem(stack, ModelTransformationMode.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, mc.world, 0);
        matrices.pop();

        yOffset += 10;


        if (mod.showName.getValue()) {
            String name = stack.getName().getString();
            int width = tr.getWidth(name);
            tr.draw(Text.literal(name), -width / 2f, yOffset, 0xFFFFFFFF, false, matrix, vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            yOffset += 10;
        }


        if (mod.showCount.getValue() && stack.getCount() > 1) {
            String count = String.valueOf(stack.getCount());
            int width = tr.getWidth(count);
            tr.draw(Text.literal(count), -width / 2f, yOffset, 0xFFFFFFFF, false, matrix, vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        }

        matrices.pop();
    }
}

