package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.ItemSetting;
import com.radium.client.modules.Module;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FakeElytra extends Module {
    private final ItemSetting appearanceItem = new ItemSetting("Swapped Item", Items.DIAMOND);
    private EquipStage equipStage = EquipStage.NONE;
    private int elytraSlotToEquip = -1;
    private int equipWaitTicks = 0;

    public FakeElytra() {
        super("FakeElytra", "Shows Elytra as another item client-side", Category.MISC);
        this.addSettings(appearanceItem);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }

        Item targetItem = appearanceItem.getItem();
        if (targetItem == null || targetItem == Items.AIR) {
            equipStage = EquipStage.NONE;
            return;
        }
        boolean isChestplate = targetItem instanceof ArmorItem armorItem &&
                armorItem.getSlotType() == net.minecraft.entity.EquipmentSlot.CHEST &&
                targetItem != Items.ELYTRA;

        if (!isChestplate) {
            equipStage = EquipStage.NONE;
            return;
        }

        ItemStack chestplateStack = mc.player.getInventory().getArmorStack(2);
        if (chestplateStack.getItem() == Items.ELYTRA) {
            equipStage = EquipStage.NONE;
            return;
        }
        switch (equipStage) {
            case NONE -> {
                int elytraSlot = findElytraSlot();
                if (elytraSlot == -1) {
                    return;
                }

                elytraSlotToEquip = elytraSlot;
                equipStage = EquipStage.PICKUP_ELYTRA;
                equipWaitTicks = 0;
            }
            case PICKUP_ELYTRA -> {
                equipWaitTicks++;
                if (equipWaitTicks >= 1) {
                    int convertedSlot = convertSlotIndex(elytraSlotToEquip);
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId,
                            convertedSlot,
                            0,
                            SlotActionType.PICKUP,
                            mc.player
                    );
                    equipStage = EquipStage.PLACE_ON_CHESTPLATE;
                    equipWaitTicks = 0;
                }
            }
            case PLACE_ON_CHESTPLATE -> {
                equipWaitTicks++;
                if (equipWaitTicks >= 1) {

                    int chestplateSlotIndex = 38;
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId,
                            chestplateSlotIndex,
                            0,
                            SlotActionType.PICKUP,
                            mc.player
                    );
                    equipStage = EquipStage.NONE;
                    equipWaitTicks = 0;
                }
            }
        }
    }

    private int findElytraSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    private int convertSlotIndex(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 9) {
            return 36 + slotIndex;
        }
        if (slotIndex >= 9 && slotIndex < 36) {
            return slotIndex;
        }
        return slotIndex;
    }

    public ItemStack getDisplayStack(ItemStack original) {
        if (original == null || original.isEmpty()) {
            return null;
        }

        Item target = appearanceItem.getItem();
        if (target == null || target == Items.AIR) {
            return null;
        }


        if (!original.isOf(target)) {
            return null;
        }

        ItemStack fake = new ItemStack(Items.ELYTRA, original.getCount());


        MutableText yellowName = Text.literal(Items.ELYTRA.getName().getString())
                .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withItalic(false));
        fake.set(DataComponentTypes.CUSTOM_NAME, yellowName);


        if (mc != null && mc.world != null) {
            mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).ifPresent(registry -> {
                RegistryEntry<net.minecraft.enchantment.Enchantment> unbreaking =
                        registry.getEntry(Enchantments.UNBREAKING).orElse(null);
                RegistryEntry<net.minecraft.enchantment.Enchantment> mending =
                        registry.getEntry(Enchantments.MENDING).orElse(null);

                if (unbreaking != null) {
                    fake.addEnchantment(unbreaking, 3);
                }
                if (mending != null) {
                    fake.addEnchantment(mending, 1);
                }
            });
        }

        return fake;
    }

    private enum EquipStage {
        NONE, PICKUP_ELYTRA, PLACE_ON_CHESTPLATE
    }
}


