package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.FakeElytra;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Inject(method = "getArmorStack(I)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void radium$swapChestplateForElytra(int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot != 2) {
            return;
        }

        FakeElytra module = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(FakeElytra.class)
                : null;

        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack chestplateStack = cir.getReturnValue();

        if (chestplateStack == null || chestplateStack.isEmpty()) {
            return;
        }

        ItemStack fake = module.getDisplayStack(chestplateStack);
        if (fake == null) {
            return;
        }

        cir.setReturnValue(fake);
    }
}


