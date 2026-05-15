package com.radium.client.mixins;

import com.mojang.authlib.GameProfile;
import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.SkinChanger;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Unique
    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);
    @Shadow
    @Final
    private GameProfile profile;

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (isProcessing.get()) {
            return;
        }

        if (RadiumClient.mc == null || RadiumClient.moduleManager == null || RadiumClient.mc.player == null) {
            return;
        }

        if (!this.profile.getId().equals(RadiumClient.mc.player.getGameProfile().getId())) {
            return;
        }

        SkinChanger skinChanger = RadiumClient.moduleManager.getModule(SkinChanger.class);
        if (skinChanger != null && skinChanger.isProtecting()) {
            String targetUsername = skinChanger.getTargetUsername();

            if (targetUsername != null && !targetUsername.isEmpty()) {
                isProcessing.set(true);
                try {
                    PlayerListEntry targetEntry = RadiumClient.mc.getNetworkHandler().getPlayerListEntry(targetUsername);

                    if (targetEntry != null) {
                        SkinTextures targetSkin = targetEntry.getSkinTextures();
                        if (targetSkin != null) {
                            SkinTextures replaced = new SkinTextures(
                                    targetSkin.texture(),
                                    targetSkin.textureUrl(),
                                    targetSkin.capeTexture(),
                                    targetSkin.elytraTexture(),
                                    targetSkin.model(),
                                    targetSkin.secure()
                            );
                            cir.setReturnValue(replaced);
                            return;
                        }
                    }

                    Identifier customSkin = skinChanger.getCachedSkinTexture();

                    if (customSkin != null) {
                        SkinTextures replaced = new SkinTextures(
                                customSkin,
                                null,
                                null,
                                null,
                                SkinTextures.Model.WIDE,
                                false
                        );
                        cir.setReturnValue(replaced);
                    } else {
                        SkinTextures defaultSkin = DefaultSkinHelper.getSkinTextures(this.profile);
                        SkinTextures replaced = new SkinTextures(
                                defaultSkin.texture(),
                                defaultSkin.textureUrl(),
                                defaultSkin.capeTexture(),
                                defaultSkin.elytraTexture(),
                                defaultSkin.model(),
                                defaultSkin.secure()
                        );
                        cir.setReturnValue(replaced);
                    }
                } finally {
                    isProcessing.set(false);
                }
            }
        }
    }
}


