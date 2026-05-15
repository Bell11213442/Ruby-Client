package com.radium.client.mixins;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.Cape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            if (RadiumClient.moduleManager == null) return;
            Cape capeModule = RadiumClient.moduleManager.getModule(Cape.class);
            if (capeModule != null && capeModule.shouldShowCape()) {
                Identifier cape = capeModule.getCapeTexture();
                if (cape != null) {
                    SkinTextures old = cir.getReturnValue();
                    SkinTextures newTex = new SkinTextures(
                            old.texture(),
                            old.textureUrl(),
                            cape,
                            old.elytraTexture(),
                            old.model(),
                            old.secure()
                    );
                    cir.setReturnValue(newTex);
                }
            }
        }
    }
}
