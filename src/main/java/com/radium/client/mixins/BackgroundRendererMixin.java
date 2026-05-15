package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.visual.NoRender;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        NoRender noRender = RadiumClient.getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled() && !noRender.shouldRenderFog()) {
            ci.cancel();
        }
    }
}
