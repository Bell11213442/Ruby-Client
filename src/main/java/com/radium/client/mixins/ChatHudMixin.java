package com.radium.client.mixins;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.donut.Baltagger;
import com.radium.client.modules.donut.SilentHome;
import com.radium.client.modules.misc.ChatUtils;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (RadiumClient.moduleManager == null) return;
        SilentHome silentHome = RadiumClient.moduleManager.getModule(SilentHome.class);

        if (silentHome != null && silentHome.isEnabled() && silentHome.shouldSuppressChatMessages()) {
            if (message.getString().toLowerCase().contains("home")) {
                ci.cancel();
            }
        }

        Baltagger baltagger = RadiumClient.moduleManager.getModule(Baltagger.class);
        if (baltagger != null && baltagger.isEnabled()) {
            String msg = message.getString();
            if (msg.contains("API key") || msg.contains("Your API key") || msg.contains("Legacy API key") || msg.contains("API Token")) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        if (RadiumClient.moduleManager == null) return;
        if (RadiumClient.moduleManager.getModule(ChatUtils.class).isEnabled()) {
            ci.cancel();
        }
    }
}

