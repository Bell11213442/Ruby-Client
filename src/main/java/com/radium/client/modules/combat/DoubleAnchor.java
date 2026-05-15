package com.radium.client.modules.combat;
// radium client

import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.KeybindSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.BlockUtil;
import com.radium.client.utils.InventoryUtil;
import com.radium.client.utils.KeyUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static com.radium.client.client.RadiumClient.eventManager;

public final class DoubleAnchor extends Module implements TickListener {
    private final KeybindSetting triggerKey = new KeybindSetting("Trigger Key", 71);
    private int delayCounter = 0;
    private int step = 0;
    private boolean isAnchoring = false;

    public DoubleAnchor() {
        super("DoubleAnchor", "Automatically Places 2 anchors", Category.COMBAT);
        this.addSettings(this.triggerKey);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        eventManager.add(TickListener.class, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(TickListener.class, this);
    }

    @Override
    public void onTick2() {
        if (mc.currentScreen != null) {
            return;
        }
        if (mc.player == null) {
            return;
        }
        if (!this.hasRequiredItems()) {
            return;
        }
        if (!this.isAnchoring && !this.checkTriggerKey()) {
            return;
        }
        int selectedSlot = 0;
        final HitResult crosshairTarget = mc.crosshairTarget;
        if (!(mc.crosshairTarget instanceof BlockHitResult) || BlockUtil.isBlockAtPosition(((BlockHitResult) crosshairTarget).getBlockPos(), Blocks.AIR)) {
            this.isAnchoring = false;
            this.resetState();
            return;
        }
        if (this.delayCounter < 0) {
            ++this.delayCounter;
            return;
        }
        if (this.step == 0) {
            InventoryUtil.swap(Items.RESPAWN_ANCHOR);
            selectedSlot = mc.player.getInventory().selectedSlot;
        } else if (this.step == 1) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 2) {
            InventoryUtil.swap(Items.GLOWSTONE);
        } else if (this.step == 3) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 4) {
            InventoryUtil.swap(Items.RESPAWN_ANCHOR);
        } else if (this.step == 5) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 6) {
            InventoryUtil.swap(Items.GLOWSTONE);
        } else if (this.step == 7) {
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 8) {
            InventoryUtil.swap(Items.TOTEM_OF_UNDYING);
        } else if (this.step == 9) {
            BlockPos anchorPos = ((BlockHitResult) crosshairTarget).getBlockPos();
            if (hasLootNearby(anchorPos)) {
                this.isAnchoring = false;
                this.resetState();
                return;
            }
            BlockUtil.interactWithBlock((BlockHitResult) crosshairTarget, true);
        } else if (this.step == 10) {
            mc.player.getInventory().selectedSlot = selectedSlot;
        } else if (this.step == 11) {
            this.isAnchoring = false;
            this.step = 0;
            this.resetState();
            return;
        }
        ++this.step;
    }

    private boolean hasRequiredItems() {
        boolean b = false;
        boolean b2 = false;
        for (int i = 0; i < 9; ++i) {
            final ItemStack getStack = mc.player.getInventory().getStack(i);
            if (getStack.getItem().equals(Items.RESPAWN_ANCHOR)) {
                b = true;
            }
            if (getStack.getItem().equals(Items.GLOWSTONE)) {
                b2 = true;
            }
        }
        return b && b2;
    }

    private boolean checkTriggerKey() {
        final int d = this.triggerKey.getValue();
        if (d == -1 || !KeyUtils.isKeyPressed(d)) {
            this.resetState();
            return false;
        }
        return this.isAnchoring = true;
    }

    private void resetState() {
        this.delayCounter = 0;
    }

    public boolean isAnchoringActive() {
        return this.isAnchoring;
    }

    private boolean hasLootNearby(BlockPos pos) {
        if (mc.world == null || pos == null) return false;


        double searchRadius = 10.0;
        Box searchBox = new Box(
                pos.getX() - searchRadius, pos.getY() - searchRadius, pos.getZ() - searchRadius,
                pos.getX() + searchRadius, pos.getY() + searchRadius, pos.getZ() + searchRadius
        );

        for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();
                if (stack.isEmpty()) continue;


                if (stack.getItem() instanceof ArmorItem) {
                    return true;
                }


                if (stack.getItem() instanceof SwordItem) {
                    return true;
                }


                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return true;
                }
            }
        }

        return false;
    }
}

