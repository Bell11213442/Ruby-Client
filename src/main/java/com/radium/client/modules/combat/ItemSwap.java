package com.radium.client.modules.combat;
// radium client

import com.radium.client.events.event.AttackListener2;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import net.minecraft.entity.decoration.EndCrystalEntity;

import static com.radium.client.client.RadiumClient.eventManager;

public class ItemSwap extends Module implements AttackListener2 {
    private final NumberSetting switchToSlot = new NumberSetting("Switch To Slot", 1.0, 1.0, 9.0, 1.0);
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);
    private final ModeSetting<SwitchBackMode> switchBackMode = new ModeSetting<>("Switch Back Mode", SwitchBackMode.PREVIOUS, SwitchBackMode.class);
    private final NumberSetting switchBackSlot = new NumberSetting("Switch Back Slot", 1.0, 1.0, 9.0, 1.0);
    private final NumberSetting switchBackDelay = new NumberSetting("Switch Back Delay", 1.0, 0.0, 20.0, 1.0);
    private final BooleanSetting ignoreEndCrystals = new BooleanSetting("Ignore End Crystals", false);

    private int previousSlot = -1;
    private int delayCounter = 0;

    public ItemSwap() {
        super("ItemSwap", "Switches to specified slot when hitting", Category.COMBAT);
        Setting<?>[] settingArray = new Setting<?>[]{
                this.switchToSlot,
                this.switchBack,
                this.switchBackMode,
                this.switchBackSlot,
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

        int targetSlot = switchToSlot.getValue().intValue() - 1;

        if (switchBack.getValue()) {
            if (switchBackMode.isMode(SwitchBackMode.PREVIOUS)) {
                previousSlot = mc.player.getInventory().selectedSlot;
            } else if (switchBackMode.isMode(SwitchBackMode.SPECIFIED)) {
                previousSlot = switchBackSlot.getValue().intValue() - 1;
            }
        }

        mc.player.getInventory().selectedSlot = targetSlot;

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

    public enum SwitchBackMode {
        PREVIOUS("Previous", 0),
        SPECIFIED("Specified", 1);

        SwitchBackMode(final String name, final int ordinal) {
        }
    }
}

