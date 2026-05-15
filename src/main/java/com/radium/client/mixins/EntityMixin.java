package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.misc.Freelook;
import com.radium.client.modules.misc.NameProtect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class EntityMixin {

    @Inject(method = {"changeLookDirection"}, at = {@At("HEAD")}, cancellable = true)
    private void updateChangeLookDirection(final double cursorDeltaX, final double cursorDeltaY, final CallbackInfo ci) {
        if (Entity.class.cast(this) != RadiumClient.mc.player) return;
        if (RadiumClient.moduleManager == null) return;
        final Freecam freecam = RadiumClient.moduleManager.getModule(Freecam.class);
        final Freelook freelook = RadiumClient.moduleManager.getModule(Freelook.class);
        if (freecam.isEnabled()) {
            freecam.updateRotation(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
        } else if (freelook != null && freelook.isActive()) {
            freelook.updateRotation(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
        }
    }

    @Inject(method = {"getDisplayName"}, at = {@At("RETURN")}, cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        Entity entity = (Entity) (Object) this;

        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        if (RadiumClient.moduleManager == null) return;
        NameProtect nameProtect = RadiumClient.moduleManager.getModule(NameProtect.class);
        if (nameProtect == null || !nameProtect.isEnabled()) {
            return;
        }

        String playerName = player.getGameProfile().getName();
        if (playerName == null || playerName.isEmpty()) {
            return;
        }

        String fakeName = nameProtect.getFakeNameFor(playerName);
        if (!fakeName.equals(playerName)) {
            Text originalText = cir.getReturnValue();
            if (originalText != null) {
                String textString = originalText.getString();
                if (textString.contains(playerName)) {
                    Text newText = Text.literal(textString.replace(playerName, fakeName));
                    cir.setReturnValue(newText);
                }
            }
        }
    }
}

