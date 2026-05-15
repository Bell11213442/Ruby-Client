package com.radium.client.modules.combat;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.FakeInvScreen;
import com.radium.client.utils.InventoryUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InvTotem extends Module implements GameRenderListener {

    private final ModeSetting<Mode> mode = new ModeSetting<>("Mode", Mode.Blatant, Mode.class);
    private final BooleanSetting autoOpen = new BooleanSetting("Auto Open", true);
    private final NumberSetting stayOpenFor = new NumberSetting("Stay Open For", 2.0, 0.0, 20.0, 1.0);
    private final List<Long> recentIntervals = new ArrayList<>();
    private final Random random = new Random();
    private long nextActionTime = 0;
    private int closeClock = -1;
    private boolean justOpenedInventory = false;

    public InvTotem() {
        super("Inv Totem", "Automatically equips totems from your inventory", Category.COMBAT);
        this.addSettings(mode, autoOpen, stayOpenFor);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(GameRenderListener.class, this);
        nextActionTime = System.currentTimeMillis();
        closeClock = -1;
        justOpenedInventory = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(GameRenderListener.class, this);
        super.onDisable();
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        if (shouldOpenScreen() && autoOpen.getValue() && !(mc.currentScreen instanceof InventoryScreen)) {
            mc.setScreen(new FakeInvScreen(mc.player));
            justOpenedInventory = true;
            nextActionTime = currentTime + 150 + random.nextInt(100);
            return;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            nextActionTime = currentTime;
            closeClock = -1;
            return;
        }

        if (closeClock == -1)
            closeClock = stayOpenFor.getValue().intValue() + random.nextInt(3);

        if (currentTime < nextActionTime) return;

        if (justOpenedInventory) {
            justOpenedInventory = false;
            nextActionTime = currentTime + generateHumanDelay();
            return;
        }

        PlayerInventory inventory = mc.player.getInventory();

        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            int slot = chooseTotemSlot();
            if (slot != -1) {
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, remapSlot(slot), 40, SlotActionType.SWAP, mc.player);
                nextActionTime = currentTime + generateHumanDelay();
                return;
            }
        }

        if (shouldCloseScreen() && autoOpen.getValue()) {
            if (closeClock > 0) {
                closeClock--;
                return;
            }
            mc.setScreen(null);
            closeClock = stayOpenFor.getValue().intValue() + random.nextInt(3);
        }
    }

    private boolean shouldCloseScreen() {
        return mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING &&
                mc.currentScreen instanceof FakeInvScreen;
    }

    private boolean shouldOpenScreen() {
        return mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING &&
                InventoryUtil.countItemExceptHotbar(Items.TOTEM_OF_UNDYING) != 0;
    }

    private int remapSlot(int slot) {
        return slot < 9 ? 36 + slot : slot;
    }

    private int chooseTotemSlot() {
        if (mode.getValue() == Mode.Blatant) {
            return random.nextDouble() < 0.7 ? InventoryUtil.findTotemSlot() : InventoryUtil.findRandomTotemSlot();
        } else {
            return InventoryUtil.findRandomTotemSlot();
        }
    }

    private long generateHumanDelay() {
        long lastDelay = recentIntervals.isEmpty() ? 120 : recentIntervals.get(recentIntervals.size() - 1);

        long base = 100 + random.nextInt(100);

        long correlatedJitter = (long) ((random.nextDouble() - 0.5) * (50 + random.nextInt(50)));

        long longPause = 0;
        if (random.nextDouble() < 0.1) {
            longPause = 200 + random.nextInt(300);
        }

        long delay = base + correlatedJitter + longPause;

        delay = (delay + lastDelay) / 2;

        delay = Math.max(50, Math.min(600, delay));

        recentIntervals.add(delay);
        if (recentIntervals.size() > 50) recentIntervals.remove(0);

        return delay;
    }

    public enum Mode {
        Blatant, Random
    }
}
