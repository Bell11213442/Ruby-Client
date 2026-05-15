package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.AutoReconnect;
import com.radium.client.modules.misc.BaseDigger;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private DirectionalLayoutWidget grid;

    @Unique
    private ButtonWidget reconnectBtn;

    @Unique
    private double time;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V", shift = At.Shift.BEFORE))
    private void addButtons(CallbackInfo ci) {
        AutoReconnect autoReconnect = RadiumClient.getModuleManager().getModule(AutoReconnect.class);

        if (autoReconnect == null) return;
        if (RadiumClient.moduleManager == null) return;
        if (RadiumClient.moduleManager.getModule(BaseDigger.class).isEnabled() && RadiumClient.moduleManager.getModule(BaseDigger.class).diggingState != BaseDigger.DiggingState.NONE) {
            return;
        }

        this.time = autoReconnect.getTime() * 20;

        if (autoReconnect.hasLastServer() && !autoReconnect.shouldHideButtons()) {
            reconnectBtn = ButtonWidget.builder(Text.literal(getText()), button -> tryConnecting())
                    .build();
            grid.add(reconnectBtn);

            grid.add(
                    ButtonWidget.builder(Text.literal("Toggle Auto Reconnect"), button -> {
                        autoReconnect.toggle();
                        if (reconnectBtn != null) {
                            reconnectBtn.setMessage(Text.literal(getText()));
                        }
                        time = autoReconnect.getTime() * 20;
                    }).build()
            );
        }
    }

    @Override
    public void tick() {
        AutoReconnect autoReconnect = RadiumClient.getModuleManager().getModule(AutoReconnect.class);

        if (autoReconnect == null || !autoReconnect.isEnabled() || !autoReconnect.hasLastServer()) {
            return;
        }

        if (time <= 0) {
            tryConnecting();
        } else {
            time--;
            if (reconnectBtn != null) {
                reconnectBtn.setMessage(Text.literal(getText()));
            }
        }
    }

    @Unique
    private String getText() {
        AutoReconnect autoReconnect = RadiumClient.getModuleManager().getModule(AutoReconnect.class);
        String reconnectText = "Reconnect";

        if (autoReconnect != null && autoReconnect.isEnabled()) {
            reconnectText += " " + String.format("(%.1f)", time / 20);
        }

        return reconnectText;
    }

    @Unique
    private void tryConnecting() {
        AutoReconnect autoReconnect = RadiumClient.getModuleManager().getModule(AutoReconnect.class);

        if (autoReconnect == null || !autoReconnect.hasLastServer()) {
            return;
        }

        ConnectScreen.connect(new TitleScreen(), this.client, autoReconnect.lastServerAddress,
                autoReconnect.lastServerInfo, false, null);
    }
}
