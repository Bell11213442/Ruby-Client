package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.HandleInputListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.mixins.MinecraftClientAccessor;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;

public class AutoFirework extends Module implements HandleInputListener {

    private final BooleanSetting onlyElytra = new BooleanSetting("Only Elytra", true);
    private final NumberSetting delay = new NumberSetting("Delay", 10.0, 1.0, 40.0, 1.0);
    private final NumberSetting durabilityWarningThreshold = new NumberSetting("Durability Warning %", 10.0, 1.0, 50.0, 1.0);

    private int cooldown = 0;
    private int previousSlot = -1;
    private boolean durabilityAlerted = false;

    public AutoFirework() {
        super("AutoFirework", "Automatically uses fireworks for elytra flying.", Category.COMBAT);
        addSettings(onlyElytra, delay, durabilityWarningThreshold);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(HandleInputListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(HandleInputListener.class, this);
        super.onDisable();
    }

    @Override
    public void onHandleInput() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) {
            return;
        }

        checkElytraDurability();

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (onlyElytra.getValue() && !mc.player.isFallFlying()) {
            return;
        }

        int fireworkSlot = findFireworkInHotbar();

        if (fireworkSlot != -1) {
            if (previousSlot == -1) {
                previousSlot = mc.player.getInventory().selectedSlot;
            }

            if (mc.player.getInventory().selectedSlot != fireworkSlot) {
                mc.player.getInventory().selectedSlot = fireworkSlot;
            } else {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }

            cooldown = delay.getValue().intValue();
        }
    }


    private void checkElytraDurability() {
        ItemStack chestplate = mc.player.getInventory().getArmorStack(2);

        if (chestplate.getItem() == Items.ELYTRA) {
            int maxDurability = chestplate.getMaxDamage();
            int currentDamage = chestplate.getDamage();
            int durabilityLeft = maxDurability - currentDamage;
            double durabilityPercent = (durabilityLeft / (double) maxDurability) * 100;

            if (durabilityPercent <= durabilityWarningThreshold.getValue() && !durabilityAlerted) {
                ChatUtils.w("Elytra Durability at " + String.format("%.1f", durabilityPercent) + "%");
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ANVIL_PLACE, 1.0f));
                durabilityAlerted = true;
            } else if (durabilityPercent > durabilityWarningThreshold.getValue()) {
                durabilityAlerted = false;
            }
        }
    }

    private int findFireworkInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
}

