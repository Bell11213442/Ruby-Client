package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.FakeElytra;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(
            method = "getModel(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Lnet/minecraft/client/render/model/BakedModel;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void radium$swapElytraModel(ItemStack stack,
                                        World world,
                                        LivingEntity entity,
                                        int seed,
                                        CallbackInfoReturnable<BakedModel> cir) {
        FakeElytra module = RadiumClient.moduleManager != null
                ? RadiumClient.moduleManager.getModule(FakeElytra.class)
                : null;

        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack fake = module.getDisplayStack(stack);
        if (fake == null) {
            return;
        }

        ItemRenderer self = (ItemRenderer) (Object) this;
        cir.setReturnValue(self.getModel(fake, world, entity, seed));
    }
}


