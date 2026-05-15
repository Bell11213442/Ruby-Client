package com.radium.client.gui.settings;
// radium client

import net.minecraft.item.Item;

public class ItemSetting extends Setting<Item> {
    public ItemSetting(String name, Item defaultValue) {
        super(name, defaultValue);
    }

    public Item getItem() {
        return getValue();
    }
}
