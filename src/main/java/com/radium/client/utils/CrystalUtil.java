package com.radium.client.utils;
// radium client

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class CrystalUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean canPlaceCrystalClient(BlockPos block) {
        net.minecraft.block.BlockState blockState = mc.world.getBlockState(block);
        if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK)) {
            return false;
        }
        return canPlaceCrystalClientAssumeObsidian(block);
    }

    public static boolean canPlaceCrystalClientAssumeObsidian(BlockPos block) {
        BlockPos blockPos2 = block.up();
        if (!mc.world.isAir(blockPos2)) {
            return false;
        }
        double d = blockPos2.getX();
        double e = blockPos2.getY();
        double f = blockPos2.getZ();
        List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0, e + 2.0, f + 1.0));
        return list.isEmpty();
    }

    public static boolean canPlaceCrystalServer(BlockPos pos) {
        net.minecraft.block.BlockState blockState = mc.world.getBlockState(pos);
        if (blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.BEDROCK)) {
            BlockPos blockPos = pos.up();
            if (!mc.world.isAir(blockPos)) {
                return false;
            }
            double d = blockPos.getX();
            double e = blockPos.getY();
            double f = blockPos.getZ();
            List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0, e + 2.0, f + 1.0));
            return list.isEmpty();
        }
        return false;
    }
}

