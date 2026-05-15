package com.radium.client.modules.misc;
// radium client

import com.radium.client.mixins.KeyBindingAccessor;
import com.radium.client.modules.Module;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class InventoryWalk extends Module {

    public InventoryWalk() {
        super("InventoryWalk", "Allows you to move around and jump while you have your inventory or a chest open.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen == null) {
            return;
        }

        // Don't allow movement in chat or sign editing screens
        if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof AbstractSignEditScreen) {
            return;
        }

        // Get the movement keys
        KeyBinding[] keys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey,
                mc.options.sneakKey
        };

        // Update the pressed state of each key
        for (KeyBinding key : keys) {
            key.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                    ((KeyBindingAccessor) key).getBoundKey().getCode()));
        }
    }
}
