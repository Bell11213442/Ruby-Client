package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.NoRender;
import com.radium.client.modules.visual.SwingSpeed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    private void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        NoRender noRender = RadiumClient.getModuleManager().getModule(NoRender.class);
        LivingEntity self = (LivingEntity) (Object) this;

        if (noRender != null && noRender.isEnabled() && !noRender.shouldRenderSwing()) {
            if (self == MinecraftClient.getInstance().player) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if ((Object) this instanceof AbstractClientPlayerEntity player) {
            if (!player.isMainPlayer()) return;

            SwingSpeed module = RadiumClient.moduleManager
                    .getModule(SwingSpeed.class);

            if (module != null && module.isEnabled()) {
                float speedMultiplier = module.getSwingSpeedMultiplier();
                int originalDuration = cir.getReturnValue();


                int modifiedDuration = Math.round(originalDuration / speedMultiplier);


                modifiedDuration = Math.max(1, Math.min(60, modifiedDuration));

                cir.setReturnValue(modifiedDuration);
            }
        }
    }

}
