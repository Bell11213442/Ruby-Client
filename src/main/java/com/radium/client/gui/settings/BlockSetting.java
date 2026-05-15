package com.radium.client.gui.settings;
// radium client

import net.minecraft.block.Block;

import java.util.HashSet;
import java.util.Set;

public class BlockSetting extends Setting<Set<Block>> {
    public BlockSetting(String name, Set<Block> defaultValue) {
        super(name, defaultValue != null ? defaultValue : new HashSet<>());
    }

    public Set<Block> getBlocks() {
        return getValue();
    }

    public void addBlock(Block block) {
        getValue().add(block);
    }

    public void removeBlock(Block block) {
        getValue().remove(block);
    }

    public boolean containsBlock(Block block) {
        return getValue().contains(block);
    }
}


