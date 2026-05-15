package com.radium.client.mixins;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.SeeThroughWalls;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayers.class)
public class RenderLayersMixin {
    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private static void onGetBlockLayer(BlockState state, CallbackInfoReturnable<RenderLayer> cir) {
        if (RadiumClient.getModuleManager() == null) return;
        SeeThroughWalls module = RadiumClient.getModuleManager().getModule(SeeThroughWalls.class);
        if (module != null && module.isEnabled()) {
            if (module.shouldMakeInvisible(state, null)) { // Use shouldMakeInvisible without position
                if (module.getOpacityFloat() < 1.0f) {
                    cir.setReturnValue(RenderLayer.getTranslucent());
                }
            }
        }
    }
}
