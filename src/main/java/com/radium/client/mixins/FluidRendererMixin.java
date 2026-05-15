package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.SeeThroughWalls;
import com.radium.client.utils.OpacityVertexConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0)
    private VertexConsumer modifyVertexConsumer(VertexConsumer vertexConsumer, BlockRenderView world, BlockPos pos, VertexConsumer delegate, BlockState blockState, FluidState fluidState) {
        SeeThroughWalls module = RadiumClient.getModuleManager().getModule(SeeThroughWalls.class);
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible(blockState, pos)) {
                double opacity = module.getOpacity();
                if (opacity <= 0.0) {
                    return vertexConsumer;
                }
                return new OpacityVertexConsumer(vertexConsumer, module.getOpacityForPos(pos));
            }
        }
        return vertexConsumer;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        SeeThroughWalls module = RadiumClient.getModuleManager().getModule(SeeThroughWalls.class);
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible(blockState, pos)) {
                if (module.getOpacity() <= 0.0) {
                    ci.cancel();
                }
            }
        }
    }
}

