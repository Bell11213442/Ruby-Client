package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Random;

public class CrystalMacro extends Module implements TickListener {
    private final NumberSetting placeDelay = new NumberSetting("Place Delay", 0.0, 0.0, 20.0, 1.0);
    private final NumberSetting minCPS = new NumberSetting("Min CPS", 8.0, 1.0, 20.0, 0.1);
    private final NumberSetting maxCPS = new NumberSetting("Max CPS", 12.0, 1.0, 20.0, 0.1);
    private final BooleanSetting placeObi = new BooleanSetting("Place Obsidian", false);
    private final NumberSetting obiSwitchDelay = new NumberSetting("Obi Switch Delay", 0.0, 0.0, 20.0, 1.0);
    private final NumberSetting obiPlaceCooldown = new NumberSetting("Obi Place Cooldown", 1.0, 0.0, 10.0, 0.1);
    private final Random random = new Random();
    private int placeDelayCounter;
    private int breakDelayCounter;
    private int obiSwitchDelayCounter;
    private int obiPlaceCooldownCounter;
    private boolean isPlacingObi;
    private BlockHitResult pendingObiPlacement;
    private int nextBreakDelay;
    private boolean wasUseKeyPressed = false;

    public CrystalMacro() {
        super("Crystal Macro", "Automatically places and breaks crystals", Category.COMBAT);
        Setting<?>[] settingArray = new Setting<?>[]{
                this.placeDelay, this.minCPS, this.maxCPS, this.placeObi,
                this.obiSwitchDelay, this.obiPlaceCooldown
        };
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
        this.resetCounters();
        this.isPlacingObi = false;
        this.pendingObiPlacement = null;
        this.obiPlaceCooldownCounter = 0;
        this.wasUseKeyPressed = false;
        this.calculateNextBreakDelay();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
        this.resetCounters();
        this.isPlacingObi = false;
        this.pendingObiPlacement = null;
        this.obiPlaceCooldownCounter = 0;
        this.wasUseKeyPressed = false;
    }

    private void resetCounters() {
        this.placeDelayCounter = 0;
        this.breakDelayCounter = 0;
        this.obiSwitchDelayCounter = 0;
    }

    private void calculateNextBreakDelay() {
        double minCpsValue = this.minCPS.getValue();
        double maxCpsValue = this.maxCPS.getValue();

        if (minCpsValue > maxCpsValue) {
            double temp = minCpsValue;
            minCpsValue = maxCpsValue;
            maxCpsValue = temp;
        }

        double randomCPS = minCpsValue + (maxCpsValue - minCpsValue) * this.random.nextDouble();
        this.nextBreakDelay = Math.max(1, (int) (20.0 / randomCPS));
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }

        this.updateCounters();

        if (mc.player.isUsingItem()) {
            return;
        }

        boolean isUseKeyPressed = mc.options.useKey.isPressed();

        if (!isUseKeyPressed) {
            this.wasUseKeyPressed = false;
            this.obiPlaceCooldownCounter = 0;
            return;
        }

        if (!this.wasUseKeyPressed) {
            this.wasUseKeyPressed = true;
            this.obiPlaceCooldownCounter = 0;
        }

        if (this.isPlacingObi && this.obiSwitchDelayCounter <= 0 && this.pendingObiPlacement != null) {
            this.finishObsidianPlacement();
            return;
        }

        if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
            return;
        }

        this.handleInteraction();
    }

    private void updateCounters() {
        if (this.placeDelayCounter > 0) {
            --this.placeDelayCounter;
        }
        if (this.breakDelayCounter > 0) {
            --this.breakDelayCounter;
        }
        if (this.obiSwitchDelayCounter > 0) {
            --this.obiSwitchDelayCounter;
        }
        if (this.obiPlaceCooldownCounter > 0) {
            --this.obiPlaceCooldownCounter;
        }
    }

    private void handleInteraction() {
        final HitResult crosshairTarget = mc.crosshairTarget;
        if (crosshairTarget == null) return;
        if (mc.crosshairTarget instanceof BlockHitResult) {
            this.handleBlockInteraction((BlockHitResult) crosshairTarget);
        } else if (mc.crosshairTarget instanceof EntityHitResult) {
            this.handleEntityInteraction((EntityHitResult) crosshairTarget);
        }
    }

    private void handleBlockInteraction(final BlockHitResult blockHitResult) {
        if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (this.placeDelayCounter > 0 || this.isPlacingObi) {
            return;
        }

        final BlockPos blockPos = blockHitResult.getBlockPos();

        if (this.placeObi.getValue() &&
                this.obiPlaceCooldownCounter <= 0 &&
                !this.isBlockAtPosition(blockPos, Blocks.OBSIDIAN) &&
                !this.isBlockAtPosition(blockPos, Blocks.BEDROCK)) {
            if (this.startObsidianPlacement(blockHitResult)) {
            }
        } else if ((this.isBlockAtPosition(blockPos, Blocks.OBSIDIAN) ||
                this.isBlockAtPosition(blockPos, Blocks.BEDROCK)) &&
                this.isValidCrystalPlacement(blockPos)) {
            BlockPos crystalPos = blockPos.up();
            if (this.hasLootNearby(crystalPos)) {
                return;
            }
            this.interactWithBlock(blockHitResult, true);
            this.placeDelayCounter = this.placeDelay.getValue().intValue();
        }
    }

    private boolean startObsidianPlacement(final BlockHitResult blockHitResult) {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        int obiSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.OBSIDIAN) {
                obiSlot = i;
                break;
            }
        }

        if (obiSlot == -1) {
            return false;
        }

        mc.player.getInventory().selectedSlot = obiSlot;

        this.isPlacingObi = true;
        this.pendingObiPlacement = blockHitResult;
        this.obiSwitchDelayCounter = this.obiSwitchDelay.getValue().intValue();

        return true;
    }

    private void finishObsidianPlacement() {
        if (this.pendingObiPlacement == null) {
            this.isPlacingObi = false;
            return;
        }

        this.interactWithBlock(this.pendingObiPlacement, true);

        int crystalSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.END_CRYSTAL) {
                crystalSlot = i;
                break;
            }
        }

        if (crystalSlot != -1) {
            mc.player.getInventory().selectedSlot = crystalSlot;
        }

        final BlockPos blockPos = this.pendingObiPlacement.getBlockPos();
        if (this.isValidCrystalPlacement(blockPos)) {
            this.interactWithBlock(this.pendingObiPlacement, true);
            this.placeDelayCounter = this.placeDelay.getValue().intValue();
        }

        this.obiPlaceCooldownCounter = (int) (this.obiPlaceCooldown.getValue() * 20);

        this.isPlacingObi = false;
        this.pendingObiPlacement = null;
    }

    private void handleEntityInteraction(final EntityHitResult entityHitResult) {
        if (entityHitResult == null || this.breakDelayCounter > 0 || this.isPlacingObi) return;
        final Entity entity = entityHitResult.getEntity();
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        if (!(entity instanceof EndCrystalEntity) && !(entity instanceof SlimeEntity)) return;

        if (this.hasLootNearby(entity.getBlockPos())) {
            return;
        }

        if (mc.player != null && mc.interactionManager != null) {
            try {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                this.breakDelayCounter = this.nextBreakDelay;
                this.calculateNextBreakDelay();
            } catch (Exception e) {
            }
        }
    }

    private boolean isValidCrystalPlacement(final BlockPos blockPos) {
        final BlockPos up = blockPos.up();
        if (!mc.world.isAir(up)) {
            return false;
        }
        final int x = up.getX();
        final int y = up.getY();
        final int z = up.getZ();
        return mc.world.getOtherEntities(null, new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
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

                if (stack.getItem() == Items.ELYTRA) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isBlockAtPosition(final BlockPos blockPos, final net.minecraft.block.Block block) {
        return mc.world.getBlockState(blockPos).getBlock() == block;
    }

    private void interactWithBlock(final BlockHitResult blockHitResult, final boolean shouldSwingHand) {
        if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult).isAccepted() && shouldSwingHand) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}

