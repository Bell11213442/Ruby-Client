package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.FakeElytra;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Unique
    private boolean radium$handlingFakeElytra;

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void radium$swapElytraTooltip(Item.TooltipContext tooltipContext,
                                          PlayerEntity player,
                                          TooltipType type,
                                          CallbackInfoReturnable<List<Text>> cir) {
        if (radium$handlingFakeElytra) {
            return;
        }

        FakeElytra module = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(FakeElytra.class)
                : null;

        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        ItemStack fake = module.getDisplayStack(self);
        if (fake == null) {
            return;
        }

        radium$handlingFakeElytra = true;
        try {

            List<Text> tooltip = new java.util.ArrayList<>(fake.getTooltip(tooltipContext, player, type));
            cir.setReturnValue(tooltip);
        } finally {
            radium$handlingFakeElytra = false;
        }
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void radium$swapElytraName(CallbackInfoReturnable<Text> cir) {
        if (radium$handlingFakeElytra) {
            return;
        }

        FakeElytra module = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(FakeElytra.class)
                : null;

        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        ItemStack fake = module.getDisplayStack(self);
        if (fake == null) {
            return;
        }

        radium$handlingFakeElytra = true;
        try {
            cir.setReturnValue(fake.getName());
        } finally {
            radium$handlingFakeElytra = false;
        }
    }
}


