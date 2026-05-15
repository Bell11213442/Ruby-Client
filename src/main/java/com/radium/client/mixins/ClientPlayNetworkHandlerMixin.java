package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.combat.AutoDoubleHand;
import com.radium.client.modules.combat.AutoLog;
import com.radium.client.modules.combat.HoverTotem;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        HoverTotem hoverTotem = RadiumClient.getModuleManager().getModule(HoverTotem.class);
        if (hoverTotem != null && hoverTotem.isEnabled()) {
            hoverTotem.onPacketReceive(packet);
        }

        AutoLog autoLog = RadiumClient.getModuleManager().getModule(AutoLog.class);
        if (autoLog != null && autoLog.isEnabled()) {
            autoLog.onPacketReceive(packet);
        }

        AutoDoubleHand autodoublehand = RadiumClient.getModuleManager().getModule(AutoDoubleHand.class);
        if (autodoublehand != null && autodoublehand.isEnabled()) {
            autodoublehand.onPacketReceive(packet);
        }

        if (packet.getStatus() == 35) {
            net.minecraft.entity.Entity entity = packet.getEntity(RadiumClient.mc.world);
            if (entity instanceof net.minecraft.entity.player.PlayerEntity player && player != RadiumClient.mc.player) {
                com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                    com.radium.client.utils.ToastNotificationManager.getInstance().show(
                            "Totem Pop",
                            player.getName().getString() + " popped a totem",
                            com.radium.client.utils.ToastNotification.ToastType.WARNING
                    );
                }
            }
        }
    }
}
