package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ItemSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.interfaces.IMinecraftClient;
import com.radium.client.modules.Module;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FastPlace extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 0.0, 0.0, 4.0, 1.0);
    private final BooleanSetting onlyXp = new BooleanSetting("Only XP", false);
    private final BooleanSetting onlyBlocks = new BooleanSetting("Only Blocks", false);
    private final ItemSetting blacklistItem1 = new ItemSetting("Blacklist 1", Items.AIR);
    private final ItemSetting blacklistItem2 = new ItemSetting("Blacklist 2", Items.AIR);
    private final ItemSetting blacklistItem3 = new ItemSetting("Blacklist 3", Items.AIR);
    private final ItemSetting blacklistItem4 = new ItemSetting("Blacklist 4", Items.AIR);
    private final ItemSetting blacklistItem5 = new ItemSetting("Blacklist 5", Items.AIR);

    public FastPlace() {
        super("FastUse", "Allows you to use items like the flash", Category.MISC);
        Setting<?>[] settingArray = new Setting<?>[]{
                this.delay,
                this.onlyXp,
                this.onlyBlocks,
                this.blacklistItem1,
                this.blacklistItem2,
                this.blacklistItem3,
                this.blacklistItem4,
                this.blacklistItem5
        };
        this.addSettings(settingArray);
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }

        ItemStack heldItem = mc.player.getMainHandStack();
        if (heldItem.isEmpty()) {
            return;
        }

        if (this.shouldFastUse(heldItem)) {
            ((IMinecraftClient) mc).setItemUseCooldown(this.delay.getValue().intValue());
        }
    }

    private boolean shouldFastUse(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        Item item = itemStack.getItem();

        if (item == this.blacklistItem1.getItem() && item != Items.AIR) {
            return false;
        }
        if (item == this.blacklistItem2.getItem() && item != Items.AIR) {
            return false;
        }
        if (item == this.blacklistItem3.getItem() && item != Items.AIR) {
            return false;
        }
        if (item == this.blacklistItem4.getItem() && item != Items.AIR) {
            return false;
        }
        if (item == this.blacklistItem5.getItem() && item != Items.AIR) {
            return false;
        }

        if (this.onlyXp.getValue()) {
            return itemStack.isOf(Items.EXPERIENCE_BOTTLE);
        }

        if (this.onlyBlocks.getValue()) {
            return itemStack.getItem() instanceof BlockItem;
        }

        return true;
    }
}
