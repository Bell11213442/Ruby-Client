package com.radium.client.mixins;
// radium client

import com.radium.client.modules.render.BaltaggerRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.render.entity.LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void cancelHasLabel(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BaltaggerRenderer.getInstance().shouldCancelNametagRendering(entity)) {
            cir.setReturnValue(false);
        }
    }
}
