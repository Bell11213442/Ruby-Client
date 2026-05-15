package com.radium.client.modules.visual;
// radium client

import com.radium.client.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Set;

public final class AntiBlockRotate extends Module {


    private static final Set<Block> REPLACED_BLOCKS = Set.of(

            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE,
            Blocks.BEDROCK,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.TUFF,
            Blocks.CALCITE,
            Blocks.SMOOTH_BASALT,
            Blocks.GRAVEL,
            Blocks.DIRT,
            Blocks.CLAY,
            Blocks.DRIPSTONE_BLOCK,
            Blocks.POINTED_DRIPSTONE,
            Blocks.MOSS_BLOCK,
            Blocks.GLOW_LICHEN,
            Blocks.AZALEA,
            Blocks.SCULK,
            Blocks.SCULK_VEIN,
            Blocks.SCULK_CATALYST,
            Blocks.SCULK_SENSOR,
            Blocks.SCULK_SHRIEKER,
            Blocks.AMETHYST_BLOCK,
            Blocks.BUDDING_AMETHYST,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.OBSIDIAN,
            Blocks.MAGMA_BLOCK
    );

    public AntiBlockRotate() {
        super("AntiBlockRotate", "Replaces specified blocks with netherite block textures", Category.VISUAL);
    }

    @Override
    public void onEnable() {
        invalidateChunks();
    }

    @Override
    public void onDisable() {
        invalidateChunks();
    }

    private void invalidateChunks() {
        if (mc.world == null || mc.player == null || mc.worldRenderer == null) {
            return;
        }

        mc.execute(() -> {
            if (mc.world == null || mc.player == null || mc.worldRenderer == null) return;

            int viewDist = mc.options.getViewDistance().getValue();
            int playerChunkX = mc.player.getChunkPos().x;
            int playerChunkZ = mc.player.getChunkPos().z;

            for (int x = playerChunkX - viewDist; x <= playerChunkX + viewDist; x++) {
                for (int z = playerChunkZ - viewDist; z <= playerChunkZ + viewDist; z++) {
                    if (mc.world.isChunkLoaded(x, z)) {
                        WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(x, z);
                        if (chunk != null) {
                            int playerY = mc.player.getBlockY();
                            int minY = Math.max(mc.world.getBottomY(), playerY - 32);
                            int maxY = Math.min(mc.world.getTopY() - 1, playerY + 32);

                            for (int offsetX = 0; offsetX < 16; offsetX += 2) {
                                for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
                                    for (int y = minY; y <= maxY; y += 8) {
                                        BlockPos updatePos = new BlockPos(
                                                x * 16 + offsetX,
                                                y,
                                                z * 16 + offsetZ
                                        );

                                        BlockState state = mc.world.getBlockState(updatePos);
                                        if (state != null && !state.isAir()) {
                                            mc.world.updateListeners(updatePos, state, state, 3);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }


    public boolean shouldReplaceBlock(Block block) {
        if (!this.isEnabled() || block == null) {
            return false;
        }
        return REPLACED_BLOCKS.contains(block);
    }
}

