package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.SliderSetting;
import com.radium.client.modules.Module;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.stream.StreamSupport;

public class SpawnerDropper extends Module {

    private final SliderSetting delay = new SliderSetting("Delay (ticks)", 10, 1, 40, 1);
    private final BooleanSetting boneOnly = new BooleanSetting("Bone Only", false);
    private State currentState = State.IDLE;
    private BlockPos spawnerPos;
    private int waitTicks = 0;

    public SpawnerDropper() {
        super("SpawnerDropper", "Automates dropping items from a spawner.", Category.DONUT);
        addSettings(delay, boneOnly);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        currentState = State.FINDING_SPAWNER;
        spawnerPos = null;
        waitTicks = 0;
    }

    @Override
    public void onDisable() {
        currentState = State.IDLE;
        spawnerPos = null;
        waitTicks = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            toggle();
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        switch (currentState) {
            case FINDING_SPAWNER:
                findSpawner();
                break;
            case OPENING_SPAWNER:
                openSpawner();
                break;
            case WAITING_FOR_GUI:
                waitForGui();
                break;
            case CLICKING_SLOT_46:
                clickSlot46();
                break;
            case WAITING_DELAY:
                waitDelay();
                break;
            case CLICKING_SLOT_50:
                clickSlot50();
                break;
            case CHECKING_SLOT_50:
                checkSlot50();
                break;
            case CHECKING_SLOTS_FOR_ARROWS:
                checkSlotsForArrows();
                break;
            case CLICKING_DROP_ALL:
                clickDropAll();
                break;
            case CLICKING_NEXT_PAGE:
                clickNextPage();
                break;
            case RE_CHECKING_SLOTS:
                reCheckSlotsForArrows();
                break;
            case IDLE:
                break;
        }
    }

    private void findSpawner() {
        spawnerPos = StreamSupport.stream(BlockPos.iterate(mc.player.getBlockPos().add(-8, -8, -8), mc.player.getBlockPos().add(8, 8, 8)).spliterator(), false)
                .filter(pos -> mc.world.getBlockState(pos).isOf(Blocks.SPAWNER))
                .min(Comparator.comparingDouble(pos -> mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))))
                .map(BlockPos::toImmutable)
                .orElse(null);

        if (spawnerPos != null) {
            currentState = State.OPENING_SPAWNER;
        } else {
            toggle();
        }
    }

    private void openSpawner() {
        if (spawnerPos == null) {
            currentState = State.FINDING_SPAWNER;
            return;
        }
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(spawnerPos), Direction.UP, spawnerPos, false));
        currentState = State.WAITING_FOR_GUI;
        waitTicks = 600;
    }

    private void waitForGui() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            if (boneOnly.getValue()) {
                currentState = State.CHECKING_SLOTS_FOR_ARROWS;
                waitTicks = 2;
            } else {
                currentState = State.CLICKING_SLOT_46;
                waitTicks = 2;
            }
        } else {
            if (waitTicks <= 1) {
                toggle();
            }
        }
    }

    private void clickSlot46() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 46, 0, SlotActionType.PICKUP, mc.player);
        currentState = State.WAITING_DELAY;
    }

    private void waitDelay() {
        waitTicks = delay.getValue().intValue();
        currentState = State.CLICKING_SLOT_50;
    }

    private void clickSlot50() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 50, 0, SlotActionType.PICKUP, mc.player);
        currentState = State.CHECKING_SLOT_50;
        waitTicks = 2;
    }

    private void checkSlot50() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        if (mc.player.currentScreenHandler.getSlot(50).getStack().getItem() != Items.ARROW) {
            mc.player.closeHandledScreen();
            toggle();
        } else {
            currentState = State.CLICKING_SLOT_46;
        }
    }

    private void checkSlotsForArrows() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        for (int slot = 0; slot <= 44; slot++) {
            if (mc.player.currentScreenHandler.getSlot(slot).getStack().getItem() == Items.ARROW) {
                mc.player.closeHandledScreen();
                toggle();
                return;
            }
        }

        currentState = State.CLICKING_DROP_ALL;
        waitTicks = 2;
    }

    private void clickDropAll() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 50, 0, SlotActionType.PICKUP, mc.player);
        currentState = State.CLICKING_NEXT_PAGE;
        waitTicks = 2;
    }

    private void clickNextPage() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 53, 0, SlotActionType.PICKUP, mc.player);
        currentState = State.RE_CHECKING_SLOTS;
        waitTicks = 2;
    }

    private void reCheckSlotsForArrows() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            toggle();
            return;
        }

        for (int slot = 0; slot <= 44; slot++) {
            if (mc.player.currentScreenHandler.getSlot(slot).getStack().getItem() == Items.ARROW) {
                mc.player.closeHandledScreen();
                toggle();
                return;
            }
        }

        currentState = State.CLICKING_DROP_ALL;
        waitTicks = 2;
    }

    private enum State {
        IDLE,
        FINDING_SPAWNER,
        OPENING_SPAWNER,
        WAITING_FOR_GUI,
        CLICKING_SLOT_46,
        WAITING_DELAY,
        CLICKING_SLOT_50,
        CHECKING_SLOT_50,
        CHECKING_SLOTS_FOR_ARROWS,
        CLICKING_DROP_ALL,
        CLICKING_NEXT_PAGE,
        RE_CHECKING_SLOTS
    }
}
