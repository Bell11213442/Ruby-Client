package com.radium.client.modules.donut;
// radium client

import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

public class LegitTridentFly extends Module {
    private boolean isCharging = false;
    private boolean isReleasing = false;
    private int previousSlot = -1;
    private int releaseCounter = 0;
    private long lastTime;

    public LegitTridentFly() {
        super("LegitTridentFly", "Undetectable Trident Fly. Requires Rain, RipTide", Module.Category.DONUT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.isCharging = false;
        this.isReleasing = false;
        this.previousSlot = -1;
        this.releaseCounter = 0;

        if (mc.player != null) {
            if (!this.selectTrident()) {

                ChatUtils.e("No riptide trident found in hotbar!");
                this.toggle();
                return;
            }
        }
        lastTime = System.nanoTime();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.options.useKey.isPressed()) {
            mc.options.useKey.setPressed(false);
        }
        if (this.previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = this.previousSlot;
            this.previousSlot = -1;
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        long now = System.nanoTime();
        float deltaSeconds = (now - lastTime) / 1_000_000_000f;
        lastTime = now;
        if (mc.world.getRainGradient(deltaSeconds) == 0) {
            ChatUtils.e("It needs to be raining to use this!");
            toggle();
            return;
        }

        ItemStack heldItem = mc.player.getMainHandStack();
        if (!this.selectTrident()) {
            ChatUtils.e("No riptide trident found in hotbar!");
            this.toggle();
            return;
        }

        if (heldItem.isDamageable()) {
            int maxDamage = heldItem.getMaxDamage();
            int currentDamage = heldItem.getDamage();
            int remainingDurability = maxDamage - currentDamage;
            double durabilityPercent = (double) remainingDurability / maxDamage * 100;

            if (durabilityPercent <= 20.0) {
                ChatUtils.e("Trident durability too low!");
                this.toggle();
                return;
            }
        }

        RegistryEntry<Enchantment> riptideEntry = mc.getNetworkHandler()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.RIPTIDE)
                .orElseThrow();
        int riptideLevel = EnchantmentHelper.getLevel(riptideEntry, heldItem);

        if (riptideLevel == 0) {
            ChatUtils.e("No riptide trident found in hotbar!");
            this.toggle();
            return;
        }

        int maxUseTime = heldItem.getMaxUseTime(mc.player);
        int useTimeLeft = mc.player.getItemUseTimeLeft();
        int chargeTime = maxUseTime - useTimeLeft;

        if (isReleasing) {
            releaseCounter++;
            if (releaseCounter >= 1) {
                isReleasing = false;
                isCharging = true;
                mc.options.useKey.setPressed(true);
            }
        } else if (isCharging) {
            if (mc.player.isUsingItem() && chargeTime >= 10) {
                mc.options.useKey.setPressed(false);
                isCharging = false;
                isReleasing = true;
                releaseCounter = 0;
            } else if (!mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(true);
            }
        } else {
            isCharging = true;
            mc.options.useKey.setPressed(true);
        }
    }

    private boolean selectTrident() {
        if (mc.player == null) {
            return false;
        }
        if (this.previousSlot == -1) {
            this.previousSlot = mc.player.getInventory().selectedSlot;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.TRIDENT)) {
                RegistryEntry<Enchantment> riptideEntry = mc.getNetworkHandler()
                        .getRegistryManager()
                        .get(RegistryKeys.ENCHANTMENT)
                        .getEntry(Enchantments.RIPTIDE)
                        .orElseThrow();

                int riptideLevel = EnchantmentHelper.getLevel(riptideEntry, stack);
                if (riptideLevel > 0) {
                    mc.player.getInventory().selectedSlot = i;
                    return true;
                }
            }
        }
        return false;
    }
}
