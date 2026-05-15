package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.EventManager;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.CustomFOV;
import com.radium.client.modules.visual.NoRender;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Camera camera;

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double fov);

    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(CallbackInfo ci) {
        NoRender noRender = RadiumClient.getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled() && !noRender.shouldRenderHurtCam()) {
            ci.cancel();
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        CustomFOV module = CustomFOV.get();
        if (module != null && module.isEnabled()) {
            cir.setReturnValue(module.fov.getValue());
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 1))
    private void onWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
        double d = getFov(camera, tickCounter.getTickDelta(true), true);
        Matrix4f matrix4f = getBasicProjectionMatrix(d);
        MatrixStack matrixStack = new MatrixStack();
        EventManager.fire(new GameRenderListener.GameRenderEvent(matrixStack, tickCounter.getTickDelta(true)));
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        NoRender noRender = RadiumClient.getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled() && !noRender.shouldRenderTotemAnimation()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (RadiumClient.getModuleManager().getModule(Freecam.class).isEnabled())
            cir.setReturnValue(false);
    }
}
