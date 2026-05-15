package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.misc.Freelook;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public class CameraMixin {
    @Unique
    private float tickDelta;

    @Inject(method = {"update"}, at = {@At("HEAD")})
    private void onUpdateHead(final BlockView area, final Entity focusedEntity, final boolean thirdPerson, final boolean inverseView, final float tickDelta, final CallbackInfo ci) {
        this.tickDelta = tickDelta;
    }

    @ModifyArgs(method = {"update"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void update(final Args args) {
        if (RadiumClient.moduleManager == null) return;
        final Freecam freecam = RadiumClient.moduleManager.getModule(Freecam.class);
        if (freecam.isEnabled()) {
            args.set(0, (Object) freecam.getInterpolatedX(this.tickDelta));
            args.set(1, (Object) freecam.getInterpolatedY(this.tickDelta));
            args.set(2, (Object) freecam.getInterpolatedZ(this.tickDelta));
        }
    }

    @ModifyArgs(method = {"update"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void onUpdateSetRotationArgs(final Args args) {
        if (RadiumClient.moduleManager == null) return;
        final Freecam freecam = RadiumClient.moduleManager.getModule(Freecam.class);
        if (freecam.isEnabled()) {
            args.set(0, (Object) (float) freecam.getInterpolatedYaw(this.tickDelta));
            args.set(1, (Object) (float) freecam.getInterpolatedPitch(this.tickDelta));
        } else {
            final Freelook freelook = RadiumClient.moduleManager.getModule(Freelook.class);
            if (freelook != null && freelook.isActive()) {
                args.set(0, (Object) (float) freelook.getInterpolatedYaw(this.tickDelta));
                args.set(1, (Object) (float) freelook.getInterpolatedPitch(this.tickDelta));
            }
        }
    }

    @ModifyVariable(at = @At("HEAD"), method = "clipToSpace(F)F", argsOnly = true)
    private float forceFreecamDistance(float desiredCameraDistance) {
        if (RadiumClient.moduleManager == null) return desiredCameraDistance;
        final Freecam freecam = RadiumClient.moduleManager.getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            return 0.0f;
        }
        return desiredCameraDistance;
    }

    @Inject(method = "clipToSpace(F)F", at = @At("HEAD"), cancellable = true)
    private void freelookNoClip(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
        if (RadiumClient.moduleManager == null) return;
        final Freelook freelook = RadiumClient.moduleManager.getModule(Freelook.class);
        if (freelook != null && freelook.isEnabled() && freelook.allowCameraNoClip()) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }
}

