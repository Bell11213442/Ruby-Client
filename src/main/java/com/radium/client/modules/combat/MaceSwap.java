package com.radium.client.modules.combat;
// radium client

import com.radium.client.events.event.AttackListener2;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static com.radium.client.client.RadiumClient.eventManager;

public class MaceSwap extends Module implements AttackListener2 {
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);
    private final NumberSetting switchBackDelay = new NumberSetting("Switch Back Delay", 1.0, 0.0, 20.0, 1.0);
    private final BooleanSetting ignoreEndCrystals = new BooleanSetting("Ignore End Crystals", false);

    private int previousSlot = -1;
    private int delayCounter = 0;

    public MaceSwap() {
        super("MaceSwap", "Switches to mace when hitting", Category.COMBAT);
        Setting<?>[] settingArray = new Setting<?>[]{
                this.switchBack,
                this.switchBackDelay,
                this.ignoreEndCrystals
        };
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        eventManager.add(AttackListener2.class, this);
        super.onEnable();
        previousSlot = -1;
        delayCounter = 0;
    }

    @Override
    public void onDisable() {
        eventManager.remove(AttackListener2.class, this);
        super.onDisable();
    }

    @Override
    public void onAttack(AttackEvent2 event) {
        if (mc.player == null || mc.world == null) return;

        if (ignoreEndCrystals.getValue() && event.entity instanceof EndCrystalEntity) {
            return;
        }

        int maceSlot = findMaceInHotbar();
        if (maceSlot == -1) return;

        if (switchBack.getValue()) {
            previousSlot = mc.player.getInventory().selectedSlot;
        }

        mc.player.getInventory().selectedSlot = maceSlot;

        if (switchBack.getValue() && previousSlot != -1) {
            delayCounter = switchBackDelay.getValue().intValue();
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (delayCounter > 0) {
            delayCounter--;
            if (delayCounter == 0 && previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
                previousSlot = -1;
            }
        }
    }

    private int findMaceInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }
}

