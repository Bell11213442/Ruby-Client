package com.radium.client.mixins;
// radium client

import com.radium.client.gui.AccountManagerScreen;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static final Identifier ACCOUNTS_ICON = Identifier.of("radium", "textures/icon.png");

    @Unique
    private float hoverProgress = 0f;

    @Unique
    private Identifier headTextureId = null;

    @Unique
    private String cachedUsername = "";

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        updateHeadTexture();
    }

    @Unique
    private void updateHeadTexture() {
        if (this.client == null || this.client.getSession() == null) {
            return;
        }

        String username = this.client.getSession().getUsername();
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        if (username.equals(cachedUsername) && headTextureId != null) {
            return;
        }

        cachedUsername = username;
        headTextureId = null;
        fetchHeadTexture(username);
    }

    @Unique
    private void fetchHeadTexture(String username) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://minotar.net/helm/" + username + "/64.png";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "RadiumClient/1.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    return;
                }

                InputStream inputStream = connection.getInputStream();
                NativeImage image = NativeImage.read(inputStream);
                inputStream.close();

                Identifier textureId = Identifier.of("radium", "account/head/" + username.toLowerCase());

                this.client.execute(() -> {
                    try {
                        this.client.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
                        headTextureId = textureId;
                    } catch (Exception e) {
                    }
                });
            } catch (Exception e) {
            }
        });
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        updateHeadTexture();

        int iconSize = 24;
        int padding = 10;
        int x = context.getScaledWindowWidth() - iconSize - padding;
        int y = padding;

        boolean hovered = mouseX >= x && mouseX <= x + iconSize
                && mouseY >= y && mouseY <= y + iconSize;

        float target = hovered ? 1f : 0f;
        hoverProgress = MathHelper.lerp(delta * 0.2f, hoverProgress, target);

        context.getMatrices().push();
        context.getMatrices().translate(x + iconSize / 2.0, y + iconSize / 2.0, 0);

        float scale = 1.0f + (hoverProgress * 0.15f);
        context.getMatrices().scale(scale, scale, 1.0f);

        if (hoverProgress > 0) {
            int glowAlpha = (int) (hoverProgress * 100);
            int glowColor = glowAlpha << 24 | 0xFF0000;
            RenderUtils.fillRadialGradient(context, 0, 0, iconSize, glowColor, 0x00FF0000);
        }

        Identifier textureToRender = headTextureId != null && this.client.getTextureManager().getTexture(headTextureId) != null ? headTextureId : ACCOUNTS_ICON;


        try {
            if (this.client.getTextureManager().getTexture(textureToRender) != null) {
                context.drawTexture(textureToRender, -iconSize / 2, -iconSize / 2, 0, 0, iconSize, iconSize, iconSize, iconSize);
            }
        } catch (Exception e) {

        }

        context.getMatrices().pop();

        if (hovered) {
            context.drawTooltip(this.textRenderer, Text.literal("§cManage Accounts"), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        int iconSize = 24;
        int padding = 10;
        int x = this.width - iconSize - padding;
        int y = padding;

        if (mouseX >= x && mouseX <= x + iconSize
                && mouseY >= y && mouseY <= y + iconSize) {
            this.client.setScreen(new AccountManagerScreen((TitleScreen) (Object) this));
            cir.setReturnValue(true);
        }
    }
}
