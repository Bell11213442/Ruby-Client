package com.radium.client.mixins;
// radium client

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.radium.client.client.RadiumClient;
import com.radium.client.events.EventManager;
import com.radium.client.events.event.*;
import com.radium.client.interfaces.IMinecraftClient;
import com.radium.client.modules.client.ClickGUI;
import com.radium.client.modules.combat.PlacementOptimizer;
import com.radium.client.modules.donut.RTPBaseFinder;
import com.radium.client.modules.donut.TunnelBaseFinder;
import com.radium.client.modules.misc.BaseDigger;
import com.radium.client.modules.misc.NoPause;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements IMinecraftClient {

    @Shadow
    @Nullable
    public ClientWorld world;
    @Shadow
    @Nullable
    public net.minecraft.client.network.ClientPlayerEntity player;
    @Shadow
    @Nullable
    public Entity targetedEntity;
    @Shadow
    private int itemUseCooldown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (world != null) {
            TickListener.TickEvent event = new TickListener.TickEvent();
            EventManager.fire(event);

            if (RadiumClient.moduleManager == null) return;
            PlacementOptimizer optimizer = RadiumClient.moduleManager.getModule(PlacementOptimizer.class);
            if (optimizer != null && optimizer.isEnabled() && player != null) {
                net.minecraft.item.Item held = player.getMainHandStack().getItem();
                boolean excludeAnchors = optimizer.shouldExcludeAnchors();
                if (excludeAnchors && (held == net.minecraft.item.Items.RESPAWN_ANCHOR || held == net.minecraft.item.Items.GLOWSTONE)) {

                } else {
                    int desiredDelay = -1;
                    if (held instanceof net.minecraft.item.BlockItem) {
                        desiredDelay = optimizer.getBlockDelay();
                    } else if (held == net.minecraft.item.Items.END_CRYSTAL) {
                        desiredDelay = optimizer.getCrystalDelay();
                    }

                    if (desiredDelay >= 0) {
                        if (desiredDelay == 0) {
                            this.itemUseCooldown = 0;
                        } else if (this.itemUseCooldown > desiredDelay) {
                            this.itemUseCooldown = desiredDelay - 1;
                        }
                    }
                }
            }
        }
        ClickGUI cGUI = RadiumClient.getClickGui();
        cGUI.setEnabled(true);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onItemUse(CallbackInfo ci) {
        ItemUseListener.ItemUseEvent event = new ItemUseListener.ItemUseEvent();

        EventManager.fire(event);
        if (event.isCancelled()) ci.cancel();
    }

    // This event doesn't need to be cancelled so
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    public void radium$handleInputEvents(CallbackInfo ci) {
        EventManager.fire(new HandleInputListener.HandleInput());
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        AttackListener.AttackEvent event = new AttackListener.AttackEvent();

        EventManager.fire(event);
        if (event.isCancelled()) cir.cancel();

        if (targetedEntity != null) {
            AttackListener2.AttackEvent2 event2 = new AttackListener2.AttackEvent2(targetedEntity);
            EventManager.fire(event2);
            if (event2.isCancelled()) cir.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
        BlockBreakingListener.BlockBreakingEvent event = new BlockBreakingListener.BlockBreakingEvent();

        EventManager.fire(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void radium$setScreen(Screen screen, CallbackInfo ci) {
        OpenScreenListener.OpenScreenEvent event = new OpenScreenListener.OpenScreenEvent(screen);

        EventManager.fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }


    @ModifyReturnValue(
            method = "isWindowFocused",
            at = @At("RETURN")
    )
    private boolean alwaysFocused(boolean original) {
        if (RadiumClient.moduleManager == null) return original;
        BaseDigger baseDigger = RadiumClient.moduleManager.getModule(BaseDigger.class);
        TunnelBaseFinder tunnelBaseFinder = RadiumClient.moduleManager.getModule(TunnelBaseFinder.class);
        RTPBaseFinder rtpBaseFinder = RadiumClient.moduleManager.getModule(RTPBaseFinder.class);
        NoPause noPause = NoPause.get();
        if (baseDigger.isEnabled() || noPause.isEnabled() || rtpBaseFinder.isEnabled() || tunnelBaseFinder.isEnabled()) {
            return true;
        }
        return original;
    }

    @Override
    public void setItemUseCooldown(int cooldown) {
        this.itemUseCooldown = cooldown;
    }
}
