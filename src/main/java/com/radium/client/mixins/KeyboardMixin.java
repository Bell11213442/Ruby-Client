package com.radium.client.mixins;
// radium client

import com.radium.client.events.EventManager;
import com.radium.client.events.event.KeyListener;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        EventManager.fire(new KeyListener.KeyEvent(key, action, window));
    }
}


