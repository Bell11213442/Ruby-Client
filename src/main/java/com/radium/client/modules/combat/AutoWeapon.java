package com.radium.client.modules.combat;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.AttackListener2;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.InventoryUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;

public class AutoWeapon extends Module implements AttackListener2 {
    private final BooleanSetting preferMace = new BooleanSetting("Prefer Mace", true);

    public AutoWeapon() {
        super("Auto Weapon", "Switches to the best weapon when attacking", Category.COMBAT);
        addSettings(preferMace);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(AttackListener2.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(AttackListener2.class, this);
        super.onDisable();
    }

    @Override
    public void onAttack(AttackEvent2 event) {
        if (mc.player == null) return;

        int bestSlot = -1;
        double maxDamage = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof MaceItem) {
                double damage = getAttackDamage(stack);
                
                if (preferMace.getValue() && stack.getItem() instanceof MaceItem) {
                    damage += 100; // Arbitrary high value to prefer mace
                }

                if (damage > maxDamage) {
                    maxDamage = damage;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1 && bestSlot != mc.player.getInventory().selectedSlot) {
            InventoryUtil.swap(bestSlot);
        }
    }

    private double getAttackDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0;
        
        double damage = 0;
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && 
                (entry.slot() == AttributeModifierSlot.MAINHAND || entry.slot() == AttributeModifierSlot.ANY)) {
                damage += entry.modifier().value();
            }
        }
        return damage;
    }
}
