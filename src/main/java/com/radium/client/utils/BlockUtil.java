package com.radium.client.utils;
// radium client

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Objects;
import java.util.stream.Stream;

import static com.radium.client.client.RadiumClient.mc;


public final class BlockUtil {
    public static Stream<WorldChunk> getLoadedChunks() {
        int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
        int diameter = radius * 2 + 1;

        ChunkPos center = mc.player.getChunkPos();
        ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
        ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);

        return Stream.iterate(min, pos -> {
                    int x = pos.x;
                    int z = pos.z;
                    x++;
                    if (x > max.x) {
                        x = min.x;
                        z++;
                    }
                    if (z > max.z)
                        throw new IllegalStateException("Stream limit didn't work.");

                    return new ChunkPos(x, z);

                }).limit((long) diameter * diameter)
                .filter(c -> mc.world.isChunkLoaded(c.x, c.z))
                .map(c -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
    }

    public static boolean isAnchorCharged(BlockPos pos) {
        if (isBlockAtPosition(pos, Blocks.RESPAWN_ANCHOR)) {
            return mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) != 0;
        }
        return false;
    }

    public static boolean isAnchorNotCharged(BlockPos pos) {
        if (isBlockAtPosition(pos, Blocks.RESPAWN_ANCHOR)) {
            return mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0;
        }
        return false;
    }

    public static boolean isBlockAtPosition(final BlockPos blockPos, final Block block) {
        return mc.world.getBlockState(blockPos).getBlock() == block;
    }

    public static void interactWithBlock(final BlockHitResult blockHitResult, final boolean shouldSwingHand) {
        final ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
        if (result.isAccepted() && result.shouldSwingHand() && shouldSwingHand) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
