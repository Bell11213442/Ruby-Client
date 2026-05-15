package com.radium.client.modules.visual;

import com.radium.client.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

public final class SeeThroughWalls extends Module {

    private static SeeThroughWalls INSTANCE;
    private static final Set<Block> TARGET_BLOCKS = Set.of(
            Blocks.DIRT,
            Blocks.STONE,
            Blocks.DEEPSLATE
    );

    public SeeThroughWalls() {
        super("SeeThroughWalls", "Makes blocks translucent.", Category.VISUAL);
        INSTANCE = this;
    }

    public static SeeThroughWalls get() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        reloadChunks();
    }

    @Override
    public void onDisable() {
        reloadChunks();
    }

    private void reloadChunks() {
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    public boolean shouldMakeInvisible(BlockState state, BlockPos pos) {
        if (!isEnabled()) return false;
        return isTargetBlock(state.getBlock());
    }

    public boolean isTargetBlock(Block block) {
        return TARGET_BLOCKS.contains(block);
    }

    public double getOpacity() {
        return 0.0;
    }

    public float getOpacityFloat() {
        return 0.0f;
    }

    public float getOpacityForPos(BlockPos pos) {
        return getOpacityFloat();
    }
    
    public boolean shouldOccludeChunks() {
        return false;
    }
}

