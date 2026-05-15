package com.radium.client.mixins;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.SnapTap;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * https://github.com/betterclient/SnapTapMC
 */

@Mixin(KeyBinding.class)
public class KeybindingMixin {
    @Shadow
    @Final
    private InputUtil.Key defaultKey;

    @Shadow
    private boolean pressed;

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    public void onGetPressed(CallbackInfoReturnable<Boolean> cir) {
        if (RadiumClient.moduleManager == null) return;
        if (!RadiumClient.moduleManager.getModule(SnapTap.class).isEnabled()) return;

        if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_A) {
            //Left
            if (this.pressed) {
                if (SnapTap.RIGHT_STRAFE_LAST_PRESS_TIME == 0) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }

                cir.setReturnValue(SnapTap.RIGHT_STRAFE_LAST_PRESS_TIME <= SnapTap.LEFT_STRAFE_LAST_PRESS_TIME);
                cir.cancel();
            }
        } else if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_D) {
            //Right
            if (this.pressed) {
                if (SnapTap.LEFT_STRAFE_LAST_PRESS_TIME == 0) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }

                cir.setReturnValue(SnapTap.LEFT_STRAFE_LAST_PRESS_TIME <= SnapTap.RIGHT_STRAFE_LAST_PRESS_TIME);
                cir.cancel();
            }
        } else if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_W) {
            //Forward
            if (this.pressed) {
                if (SnapTap.BACKWARD_STRAFE_LAST_PRESS_TIME == 0) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }

                cir.setReturnValue(SnapTap.BACKWARD_STRAFE_LAST_PRESS_TIME <= SnapTap.FORWARD_STRAFE_LAST_PRESS_TIME);
                cir.cancel();
            }
        } else if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_S) {
            //Backward
            if (this.pressed) {
                if (SnapTap.FORWARD_STRAFE_LAST_PRESS_TIME == 0) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }

                cir.setReturnValue(SnapTap.FORWARD_STRAFE_LAST_PRESS_TIME <= SnapTap.BACKWARD_STRAFE_LAST_PRESS_TIME);
                cir.cancel();
            }
        }
    }

    @Inject(method = "setPressed", at = @At("HEAD"))
    public void setPressed(boolean pressed, CallbackInfo ci) {
        if (RadiumClient.moduleManager == null) return;
        if (!RadiumClient.moduleManager.getModule(SnapTap.class).isEnabled()) return;

        if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_A) {
            //Left
            if (pressed) {
                SnapTap.LEFT_STRAFE_LAST_PRESS_TIME = System.currentTimeMillis();
            } else {
                SnapTap.LEFT_STRAFE_LAST_PRESS_TIME = 0;
            }
        } else if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_D) {
            //Right
            if (pressed) {
                SnapTap.RIGHT_STRAFE_LAST_PRESS_TIME = System.currentTimeMillis();
            } else {
                SnapTap.RIGHT_STRAFE_LAST_PRESS_TIME = 0;
            }
        } else if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_W) {
            //Forward
            if (pressed) {
                SnapTap.FORWARD_STRAFE_LAST_PRESS_TIME = System.currentTimeMillis();
            } else {
                SnapTap.FORWARD_STRAFE_LAST_PRESS_TIME = 0;
            }
        } else if (this.defaultKey.getCode() == InputUtil.GLFW_KEY_S) {
            //Backward
            if (pressed) {
                SnapTap.BACKWARD_STRAFE_LAST_PRESS_TIME = System.currentTimeMillis();
            } else {
                SnapTap.BACKWARD_STRAFE_LAST_PRESS_TIME = 0;
            }
        }
    }
}