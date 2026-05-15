package com.radium.client.modules.misc;
// radium client

import com.radium.client.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {

    public AutoTool() {
        super("AutoTool", "Automatically switches to the best tool for the job.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }


        if (mc.interactionManager.isBreakingBlock()) {
            HitResult crosshairTarget = mc.crosshairTarget;
            if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) crosshairTarget;
                BlockState blockState = mc.world.getBlockState(blockHitResult.getBlockPos());
                switchToBestTool(blockState);
            }
        }


        if (mc.options.attackKey.isPressed() && mc.targetedEntity != null) {
            switchToBestWeapon();
        }
    }

    private void switchToBestTool(BlockState blockState) {
        PlayerInventory inventory = mc.player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(blockState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            inventory.selectedSlot = bestSlot;
        }
    }

    private void switchToBestWeapon() {
        PlayerInventory inventory = mc.player.getInventory();
        int bestSlot = -1;
        double bestDamage = 0.0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;

            AttributeModifiersComponent modifiers = stack.get(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS);
            double damage = 0.0;

            if (modifiers != null) {
                for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                    RegistryEntry<?> attribute = entry.attribute();
                    if (attribute.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                        damage += entry.modifier().value();
                    }
                }
            }


            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            inventory.selectedSlot = bestSlot;
        }
    }
}
