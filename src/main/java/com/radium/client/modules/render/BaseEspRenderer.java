package com.radium.client.modules.render;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RenderUtils;
import com.radium.client.modules.visual.BaseESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;

public final class BaseEspRenderer implements WorldRenderEvents.AfterEntities {
    private static final BaseEspRenderer INSTANCE = new BaseEspRenderer();

    private BaseEspRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> INSTANCE.afterEntities(ctx));
        WorldRenderEvents.LAST.register(ctx -> INSTANCE.afterEntities(ctx));
    }

    private static boolean isWithinRadius(BlockPos pos, BlockPos center, int radius) {
        return Math.abs(pos.getX() - center.getX()) <= radius &&
                Math.abs(pos.getY() - center.getY()) <= radius &&
                Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    private static boolean isInterestingBlock(BlockEntity blockEntity) {
        return blockEntity instanceof PistonBlockEntity ||
                blockEntity instanceof SmokerBlockEntity ||
                blockEntity instanceof CrafterBlockEntity ||
                blockEntity instanceof HopperBlockEntity ||
                blockEntity instanceof BeehiveBlockEntity ||
                blockEntity instanceof BannerBlockEntity;
    }

    private static boolean isInterestingItem(ItemStack stack) {
        return stack.isOf(Items.KELP) ||
                stack.isOf(Items.PINK_PETALS) ||
                stack.isOf(Items.CRIMSON_FUNGUS) ||
                stack.isOf(Items.WARPED_FUNGUS) ||
                stack.isOf(Items.SEA_PICKLE) ||
                stack.isOf(Items.NETHERITE_SWORD) ||
                stack.isOf(Items.NETHERITE_PICKAXE) ||
                stack.isOf(Items.NETHERITE_AXE) ||
                stack.isOf(Items.NETHERITE_SHOVEL) ||
                stack.isOf(Items.NETHERITE_HELMET) ||
                stack.isOf(Items.NETHERITE_CHESTPLATE) ||
                stack.isOf(Items.NETHERITE_LEGGINGS) ||
                stack.isOf(Items.NETHERITE_BOOTS) ||
                stack.isOf(Items.BONE) ||
                stack.isOf(Items.BONE_BLOCK) ||
                stack.isOf(Items.BONE_MEAL) ||
                stack.isOf(Items.BAMBOO) ||
                stack.isOf(Items.CACTUS) ||
                stack.isOf(Items.NETHERITE_INGOT) ||
                stack.isOf(Items.NETHERITE_BLOCK);
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        BaseESP mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(BaseESP.class)
                : null;
        if (mod == null || !mod.isEnabled()) return;
        if (MinecraftClient.getInstance().world == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        ClientWorld world = context.world();
        MinecraftClient mc = MinecraftClient.getInstance();
        int viewDist = mc.options.getViewDistance().getValue();
        int pChunkX = mc.player.getBlockX() >> 4;
        int pChunkZ = mc.player.getBlockZ() >> 4;
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = 64;

        Color color = new Color(0, 255, 0, mod.getAlphaValue().getValue().intValue());
        Color colorOutline = new Color(0, 255, 0, 255);

        for (int cx = pChunkX - viewDist; cx <= pChunkX + viewDist; cx++) {
            for (int cz = pChunkZ - viewDist; cz <= pChunkZ + viewDist; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
                    BlockEntity blockEntity = world.getBlockEntity(blockPos);
                    if (blockEntity != null) {
                        BlockPos pos = blockEntity.getPos();
                        if (pos.getY() > 0) continue;
                        if (isWithinRadius(pos, playerPos, radius) && isInterestingBlock(blockEntity)) {
                            Box box = new Box(pos);
                            int argb = color.getRGB();
                            RenderUtils.drawBox(matrices, box, argb, false);
                            RenderUtils.drawBox(matrices, box, argb, true);
                        }
                    }
                }
            }
        }

        for (Entity entity : world.getEntities()) {
            BlockPos pos = entity.getBlockPos();
            if (pos.getY() > 0 || !isWithinRadius(pos, playerPos, radius)) continue;

            boolean shouldRender = false;
            if (entity instanceof HopperMinecartEntity ||
                    entity instanceof FurnaceMinecartEntity ||
                    entity instanceof ArmorStandEntity ||
                    entity instanceof BeeEntity) {
                shouldRender = true;
            } else if (entity instanceof ItemEntity itemEntity) {
                if (isInterestingItem(itemEntity.getStack())) {
                    shouldRender = true;
                }
            }

            if (shouldRender) {
                Box box = new Box(pos);
                int argb = color.getRGB();
                int argbOutline = colorOutline.getRGB();
                RenderUtils.drawBox(matrices, box, argb, false);
                RenderUtils.drawBox(matrices, box, argbOutline, true);
            }
        }

        matrices.pop();
    }
}







