package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.mixins.MinecraftClientAccessor;
import com.radium.client.modules.Module;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.state.property.Property;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AnchorMacro extends Module implements TickListener {
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 0.0, 0.0, 20.0, 1.0);
    private final NumberSetting glowstoneDelay = new NumberSetting("Glowstone Delay", 0.0, 0.0, 20.0, 1.0);
    private final NumberSetting explodeDelay = new NumberSetting("Explode Delay", 0.0, 0.0, 20.0, 1.0);
    private final NumberSetting totemSlot = new NumberSetting("Totem Slot", 1.0, 1.0, 9.0, 1.0);
    private final BooleanSetting switchBackToAnchor = new BooleanSetting("Switch Back To Anchor", true);
    private final NumberSetting switchBackDelay = new NumberSetting("Switch Back Delay", 5.0, 0.0, 20.0, 1.0);
    private final BooleanSetting pauseOnKill = new BooleanSetting("Pause On Kill", true);
    private final NumberSetting pauseDelay = new NumberSetting("Pause Delay", 2.0, 0.5, 10.0, 0.5);

    private int keybindCounter;
    private int glowstoneDelayCounter;
    private int explodeDelayCounter;
    private int switchBackDelayCounter;
    private int pauseCounter;
    private boolean hasPlacedGlowstone = false;
    private boolean hasExplodedAnchor = false;
    private boolean shouldSwitchBack = false;
    private BlockHitResult lastBlockHitResult = null;
    private List<PlayerEntity> deadPlayers = new ArrayList<>();

    public AnchorMacro() {
        super("AnchorMacro", "Automatically charges and explodes respawn anchors", Category.COMBAT);
        Setting<?>[] settingArray = new Setting<?>[]{
                this.switchDelay,
                this.glowstoneDelay,
                this.explodeDelay,
                this.totemSlot,
                this.switchBackToAnchor,
                this.switchBackDelay,
                this.pauseOnKill,
                this.pauseDelay
        };
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetCounters();
        hasPlacedGlowstone = false;
        hasExplodedAnchor = false;
        shouldSwitchBack = false;
        lastBlockHitResult = null;
        pauseCounter = 0;
        deadPlayers = new ArrayList<>();
        RadiumClient.getEventManager().add(TickListener.class, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetCounters();
        hasPlacedGlowstone = false;
        hasExplodedAnchor = false;
        shouldSwitchBack = false;
        lastBlockHitResult = null;
        pauseCounter = 0;
        deadPlayers.clear();
        RadiumClient.getEventManager().add(TickListener.class, this);
    }

    private void resetCounters() {
        keybindCounter = 0;
        glowstoneDelayCounter = 0;
        explodeDelayCounter = 0;
        switchBackDelayCounter = 0;
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }

        if (pauseCounter > 0) {
            pauseCounter--;
        }

        if (pauseOnKill.getValue() && checkForDeadPlayers()) {
            pauseCounter = (int) (pauseDelay.getValue() * 20);
        }

        if (pauseCounter > 0) {
            return;
        }

        if (isShieldOrFoodActive()) {
            return;
        }

        if (shouldSwitchBack && switchBackToAnchor.getValue()) {
            handleSwitchBackToAnchor();
            return;
        }

        if (isKeyPressed(1)) {
            handleAnchorInteraction();
        } else {
            hasPlacedGlowstone = false;
            hasExplodedAnchor = false;
            shouldSwitchBack = false;
            lastBlockHitResult = null;
        }
    }

    private boolean checkForDeadPlayers() {
        if (mc.world == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.getHealth() <= 0) {
                if (!deadPlayers.contains(player)) {
                    deadPlayers.add(player);
                    return true;
                }
            }
        }

        deadPlayers.removeIf(player -> player.getHealth() > 0);
        return false;
    }

    private boolean isShieldOrFoodActive() {
        final boolean isFood = mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD) ||
                mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
        final boolean isShield = mc.player.getMainHandStack().getItem() instanceof ShieldItem ||
                mc.player.getOffHandStack().getItem() instanceof ShieldItem;
        final boolean isRightClickPressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1;
        return (isFood || isShield) && isRightClickPressed;
    }

    private void handleSwitchBackToAnchor() {
        if (switchBackDelayCounter < switchBackDelay.getValue().intValue()) {
            ++switchBackDelayCounter;
            return;
        }

        switchBackDelayCounter = 0;
        swapToItem(Items.RESPAWN_ANCHOR);
        shouldSwitchBack = false;
    }

    private void handleAnchorInteraction() {
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHitResult)) {
            return;
        }

        lastBlockHitResult = blockHitResult;

        if (!isBlockAtPosition(blockHitResult.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            return;
        }

        mc.options.useKey.setPressed(false);

        if (isRespawnAnchorUncharged(blockHitResult.getBlockPos()) && !hasPlacedGlowstone) {
            placeGlowstone(blockHitResult);
        } else if (isRespawnAnchorCharged(blockHitResult.getBlockPos()) && !hasExplodedAnchor) {

            if (hasLootNearby(blockHitResult.getBlockPos())) {
                return;
            }
            explodeAnchor(blockHitResult);
        }
    }

    private void placeGlowstone(final BlockHitResult blockHitResult) {
        if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (keybindCounter < switchDelay.getValue().intValue()) {
                ++keybindCounter;
                return;
            }
            keybindCounter = 0;
            swapToItem(Items.GLOWSTONE);
            return;
        }

        if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (glowstoneDelayCounter < glowstoneDelay.getValue().intValue()) {
                ++glowstoneDelayCounter;
                return;
            }
            glowstoneDelayCounter = 0;
            ((MinecraftClientAccessor) mc).invokeDoItemUse();
            hasPlacedGlowstone = true;
        }
    }

    private void explodeAnchor(final BlockHitResult blockHitResult) {
        final int selectedSlot = totemSlot.getValue().intValue() - 1;

        if (mc.player.getInventory().selectedSlot != selectedSlot) {
            if (keybindCounter < switchDelay.getValue().intValue()) {
                ++keybindCounter;
                return;
            }
            keybindCounter = 0;

            mc.player.getInventory().selectedSlot = selectedSlot;
            return;
        }

        if (mc.player.getInventory().selectedSlot == selectedSlot) {
            if (explodeDelayCounter < explodeDelay.getValue().intValue()) {
                ++explodeDelayCounter;
                return;
            }
            explodeDelayCounter = 0;
            ((MinecraftClientAccessor) mc).invokeDoItemUse();
            hasExplodedAnchor = true;

            if (switchBackToAnchor.getValue()) {
                shouldSwitchBack = true;
                switchBackDelayCounter = 0;
            }
        }
    }

    private void swapToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                mc.player.getInventory().selectedSlot = i;
                return;
            }
        }
    }

    private boolean isKeyPressed(final int n) {
        if (n <= 8) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), n) == 1;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), n) == 1;
    }

    private boolean isBlockAtPosition(final BlockPos blockPos, final net.minecraft.block.Block block) {
        return mc.world.getBlockState(blockPos).getBlock() == block;
    }

    private boolean isRespawnAnchorCharged(final BlockPos blockPos) {
        return isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR) &&
                (int) mc.world.getBlockState(blockPos).get((Property) RespawnAnchorBlock.CHARGES) != 0;
    }

    private boolean isRespawnAnchorUncharged(final BlockPos blockPos) {
        return isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR) &&
                (int) mc.world.getBlockState(blockPos).get((Property) RespawnAnchorBlock.CHARGES) == 0;
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

