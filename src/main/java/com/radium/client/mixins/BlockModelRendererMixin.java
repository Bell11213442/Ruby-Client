package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.SeeThroughWalls;
import com.radium.client.utils.OpacityVertexConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {

    @ModifyVariable(method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V", at = @At("HEAD"), ordinal = 0)
    private VertexConsumer modifyVertexConsumer(VertexConsumer vertexConsumer, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos) {
        SeeThroughWalls module = RadiumClient.getModuleManager().getModule(SeeThroughWalls.class);
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible(state, pos)) {
                double opacity = module.getOpacity();
                if (opacity <= 0.0) {
                    return vertexConsumer;
                }
                return new OpacityVertexConsumer(vertexConsumer, module.getOpacityForPos(pos));
            }
        }
        return vertexConsumer;
    }
}

