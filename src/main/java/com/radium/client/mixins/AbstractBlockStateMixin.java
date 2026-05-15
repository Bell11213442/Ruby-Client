package com.radium.client.mixins;
// radium client

import com.radium.client.modules.visual.SeeThroughWalls;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Inject(method = "isOpaqueFullCube", at = @At("HEAD"), cancellable = true)
    private void onIsOpaqueFullCube(BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        SeeThroughWalls module = SeeThroughWalls.get();
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible((BlockState) (Object) this, pos)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(CallbackInfoReturnable<BlockRenderType> cir) {
        SeeThroughWalls module = SeeThroughWalls.get();
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible((BlockState) (Object) this, (BlockPos) null)) { // Modified check
                if (module.getOpacity() <= 0.0) {
                    cir.setReturnValue(BlockRenderType.INVISIBLE);
                }
            }
        }
    }

    @Inject(method = "getLuminance", at = @At("HEAD"), cancellable = true)
    private void onGetLuminance(CallbackInfoReturnable<Integer> cir) {
        SeeThroughWalls module = SeeThroughWalls.get();
        if (module != null && module.isEnabled()) {
            if (module.isTargetBlock(((BlockState) (Object) this).getBlock())) {
                cir.setReturnValue(0);
            }
        }
    }

    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        SeeThroughWalls module = SeeThroughWalls.get();
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible((BlockState) (Object) this, pos)) {
                cir.setReturnValue(1.0f);
            }
        }
    }
}

