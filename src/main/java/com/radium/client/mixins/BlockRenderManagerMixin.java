package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.AntiBlockRotate;
import com.radium.client.modules.visual.SeeThroughWalls;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {

    @Inject(
            method = "getModel(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/model/BakedModel;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void radium$replaceBlockTexture(BlockState state, CallbackInfoReturnable<BakedModel> cir) {
        AntiBlockRotate module = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(AntiBlockRotate.class)
                : null;

        if (module == null || !module.isEnabled()) {
            return;
        }
        Block block = state.getBlock();
        if (module.shouldReplaceBlock(block)) {
            BlockRenderManager self = (BlockRenderManager) (Object) this;
            BlockState netheriteState = Blocks.NETHERITE_BLOCK.getDefaultState();
            BakedModel netheriteModel = self.getModel(netheriteState);
            if (netheriteModel != null) {
                cir.setReturnValue(netheriteModel);
            }
        }
    }

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void onRenderBlock(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci) {
        SeeThroughWalls module = RadiumClient.getModuleManager().getModule(SeeThroughWalls.class);
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible(state, pos)) {
                if (module.getOpacity() <= 0.0) {
                    ci.cancel();
                }
            }
        }
    }
}

