package com.radium.client.mixins;
// radium client

import com.radium.client.events.EventManager;
import com.radium.client.events.event.MouseMoveListener;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        MouseMoveListener.MouseMoveEvent event = new MouseMoveListener.MouseMoveEvent(x, y);
        EventManager.fire(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}

