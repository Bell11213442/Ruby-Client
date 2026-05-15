package com.radium.client.modules.donut;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.Character.RotateCharacter;
import com.radium.client.utils.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static com.radium.client.client.RadiumClient.eventManager;

public class AutoTreeFarmer extends Module implements TickListener, GameRenderListener {

    private final BooleanSetting enableHotbarRefillSetting = new BooleanSetting("Hotbar Refill", true);
    private final BooleanSetting enableOrderRefillSetting = new BooleanSetting("Order Refill", true);

    private final int HOTBAR_REFILL_MIN_BONE_MEAL = 5;
    private final int HOTBAR_REFILL_MIN_SAPLING = 4;
    private final int ORDER_REFILL_AMOUNT = 3;
    private final Random random = new Random();
    private final List<BlockPos> saplingPositions = new ArrayList<>();
    private final int searchRadius = 16;
    private final int orderDelay = 400;
    private final int maxRefillRetries = 3;
    private FarmState state = FarmState.SEARCHING;
    private BlockPos targetPodzol = null;
    private int currentSaplingIndex = 0;
    private RotateCharacter rotateChar;
    private int waitTicks = 0;
    private int previousSlot = -1;
    private boolean isPlacing = false;
    private boolean logMined = false;
    private RefillState refillState = RefillState.NONE;
    private long refillStageStart = 0;
    private net.minecraft.item.Item currentRefillItem = null;
    private int stacksCollected = 0;
    private int refillRetryCount = 0;
    private boolean isRefilling = false;
    private int refillStep = 0;
    private int refillFromSlot = -1;
    private int refillToSlot = -1;
    private long refillStepStart = 0;
    private net.minecraft.item.Item refillTargetItem = null;
    private int refillMinAmount = 0;

    public AutoTreeFarmer() {
        super("AutoTreeFarmer", "AFK farms 2x2 spruce podzol patches with auto refill", Category.DONUT);
        this.addSettings(enableHotbarRefillSetting, enableOrderRefillSetting);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(GameRenderListener.class, this);
        reset();
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (rotateChar.isActive()) {
            rotateChar.update(true, true);
        }
    }

    public void reset() {
        if (mc.options == null) return;

        saplingPositions.clear();
        targetPodzol = null;
        currentSaplingIndex = 0;
        waitTicks = 0;
        previousSlot = -1;
        isPlacing = false;
        logMined = false;
        state = FarmState.SEARCHING;
        rotateChar = new RotateCharacter(RadiumClient.mc);

        refillState = RefillState.NONE;
        refillStageStart = 0;
        currentRefillItem = null;
        stacksCollected = 0;
        refillRetryCount = 0;

        isRefilling = false;
        refillStep = 0;
        refillFromSlot = -1;
        refillToSlot = -1;
        refillStepStart = 0;
        refillTargetItem = null;
        refillMinAmount = 0;

        if (mc.player != null) mc.player.getInventory().selectedSlot = previousSlot != -1 ? previousSlot : 0;
        mc.options.useKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(GameRenderListener.class, this);
        if (previousSlot != -1 && mc.player != null) mc.player.getInventory().selectedSlot = previousSlot;
        mc.options.useKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        refillState = RefillState.NONE;
        if (isRefilling) cancelHotbarRefill();
    }

    private void sendMessage(String msg) {
        ChatUtils.m(msg);
    }

    private void autoRefillHotbar(net.minecraft.item.Item targetItem, int minAmount) {
        if (!enableHotbarRefillSetting.getValue() || isRefilling) return;

        int totalInHotbar = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) totalInHotbar += stack.getCount();
        }
        if (totalInHotbar >= minAmount) return;

        int invSlot = findInInventory(targetItem);
        if (invSlot != -1 && invSlot >= 9) {
            int targetHotbarSlot = findBestHotbarSlot(targetItem);
            if (targetHotbarSlot != -1) startHotbarRefill(invSlot, targetHotbarSlot, targetItem, minAmount);
        }
    }

    private int findBestHotbarSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) return i;
        }
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        return -1;
    }

    private int findInInventory(net.minecraft.item.Item item) {
        for (int i = 9; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) return i;
        }
        return -1;
    }

    private void startHotbarRefill(int fromSlot, int toSlot, net.minecraft.item.Item item, int minAmount) {
        isRefilling = true;
        refillStep = 0;
        refillFromSlot = fromSlot;
        refillToSlot = toSlot;
        refillTargetItem = item;
        refillMinAmount = minAmount;
        refillStepStart = System.currentTimeMillis();
    }

    private void handleHotbarRefill() {
        if (!isRefilling) return;
        long now = System.currentTimeMillis();
        long timeSinceStep = now - refillStepStart;
        int stepDelay = 150 + random.nextInt(100);

        switch (refillStep) {
            case 0 -> {
                if (timeSinceStep < 50) return;
                mc.execute(() -> mc.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(mc.player)));
                refillStep = 1;
                refillStepStart = now;
            }
            case 1 -> {
                if (timeSinceStep < stepDelay) return;
                if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
                    refillStep = 2;
                    refillStepStart = now;
                } else if (timeSinceStep > 2000) cancelHotbarRefill();
            }
            case 2 -> {
                if (timeSinceStep < stepDelay) return;
                mc.execute(() -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, refillFromSlot, 0, SlotActionType.PICKUP, mc.player));
                refillStep = 3;
                refillStepStart = now;
            }
            case 3 -> {
                if (timeSinceStep < stepDelay) return;
                mc.execute(() -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, refillToSlot, 0, SlotActionType.PICKUP, mc.player));
                refillStep = 4;
                refillStepStart = now;
            }
            case 4 -> {
                if (timeSinceStep < stepDelay) return;
                mc.execute(() -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, refillFromSlot, 0, SlotActionType.PICKUP, mc.player));
                refillStep = 5;
                refillStepStart = now;
            }
            case 5 -> {
                if (timeSinceStep < stepDelay) return;
                mc.execute(() -> {
                    if (mc.currentScreen != null) mc.currentScreen.close();
                });
                refillStep = 6;
                refillStepStart = now;
            }
            case 6 -> {
                if (timeSinceStep < 100) return;
                finishHotbarRefill();
            }
        }
    }

    private void cancelHotbarRefill() {
        mc.execute(() -> {
            if (mc.currentScreen != null) mc.currentScreen.close();
        });
        finishHotbarRefill();
    }

    private void finishHotbarRefill() {
        isRefilling = false;
        refillStep = 0;
        refillFromSlot = -1;
        refillToSlot = -1;
        refillTargetItem = null;
        refillMinAmount = 0;
        refillStepStart = 0;
    }

    private void checkAndStartOrderRefill() {
        if (!enableOrderRefillSetting.getValue() || isRefilling || refillState != RefillState.NONE) return;
        if (countItemInInventory(net.minecraft.item.Items.BONE_MEAL) == 0)
            startOrderRefill(net.minecraft.item.Items.BONE_MEAL);
        else if (countItemInInventory(net.minecraft.item.Items.SPRUCE_SAPLING) == 0)
            startOrderRefill(net.minecraft.item.Items.SPRUCE_SAPLING);
    }

    private void startOrderRefill(net.minecraft.item.Item item) {
        if (refillRetryCount >= maxRefillRetries) {
            sendMessage("Max refill retries reached for " + item.getName().getString());
            refillRetryCount = 0;
            return;
        }
        currentRefillItem = item;
        refillState = RefillState.OPEN_ORDERS;
        refillStageStart = System.currentTimeMillis();
        stacksCollected = 0;
        sendMessage("Starting order refill for " + item.getName().getString());
    }

    private int countItemInInventory(net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    @Override
    public void onTick2() {
        if (mc.world == null || mc.player == null) return;

        if (isRefilling) {
            handleHotbarRefill();
            return;
        }
        if (refillState != RefillState.NONE) {
            handleOrderRefill();
            return;
        }

        autoRefillHotbar(net.minecraft.item.Items.BONE_MEAL, HOTBAR_REFILL_MIN_BONE_MEAL);
        autoRefillHotbar(net.minecraft.item.Items.SPRUCE_SAPLING, HOTBAR_REFILL_MIN_SAPLING);

        checkAndStartOrderRefill();

        switch (state) {
            case SEARCHING -> findPodzol();
            case PLANTING -> plantSaplings();
            case BONEMEALING -> boneMealSaplings();
            case MINING -> mineLog();
            case WAIT -> waitAfterMining();
        }
    }

    private void findPodzol() {
        BlockPos playerPos = mc.player.getBlockPos();
        double nearestDist = Double.MAX_VALUE;
        targetPodzol = null;

        for (int y = playerPos.getY() - 2; y <= playerPos.getY(); y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z);
                    BlockPos corner = find2x2PodzolAt(pos);
                    if (corner != null) {
                        double dist = playerPos.getSquaredDistance(corner);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            targetPodzol = corner;
                        }
                    }
                }
            }
        }

        if (targetPodzol == null) return;

        saplingPositions.clear();
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) saplingPositions.add(targetPodzol.add(dx, 1, dz));
        }
        saplingPositions.sort(Comparator.comparingDouble(pos -> -mc.player.getBlockPos().getSquaredDistance(pos)));
        currentSaplingIndex = 0;
        logMined = false;
        state = FarmState.PLANTING;
        sendMessage("Found 2x2 podzol at " + targetPodzol.toShortString());
    }

    private BlockPos find2x2PodzolAt(BlockPos pos) {
        for (int offsetX = -1; offsetX <= 0; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 0; offsetZ++) {
                boolean valid = true;
                BlockPos corner = pos.add(offsetX, 0, offsetZ);
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        if (mc.world.getBlockState(corner.add(x, 0, z)).getBlock() != Blocks.PODZOL) {
                            valid = false;
                            break;
                        }
                    }
                    if (!valid) break;
                }
                if (valid) return corner;
            }
        }
        return null;
    }

    private int findSpruceSaplingSlot() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).getItem() == Blocks.SPRUCE_SAPLING.asItem()) return i;
        return -1;
    }

    private int findBoneMealSlot() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).getItem() instanceof BoneMealItem) return i;
        return -1;
    }

    private int findBestAxeSlot() {
        int best = -1, dmg = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() instanceof AxeItem a) {
                int d = (int) a.getMaterial().getAttackDamage();
                if (d > dmg) {
                    dmg = d;
                    best = i;
                }
            }
        }
        return best;
    }

    private void rotateTo(BlockPos pos) {
        Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
        Vec3d dir = target.subtract(mc.player.getEyePos()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.asin(dir.y));
        if (!rotateChar.isActive()) rotateChar.rotate(yaw, pitch, () -> {
        });
        rotateChar.update(true, true);
    }

    private void plantSaplings() {
        if (currentSaplingIndex >= saplingPositions.size()) {
            state = FarmState.BONEMEALING;
            isPlacing = false;
            return;
        }
        BlockPos pos = saplingPositions.get(currentSaplingIndex);
        if (!mc.world.getBlockState(pos).isAir()) {
            currentSaplingIndex++;
            isPlacing = false;
            return;
        }
        int slot = findSpruceSaplingSlot();
        if (slot == -1) {
            sendMessage("No spruce saplings!");
            state = FarmState.SEARCHING;
            isPlacing = false;
            return;
        }
        if (previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        rotateTo(pos);
        if (!rotateChar.isActive()) {
            mc.options.useKey.setPressed(true);
            isPlacing = true;
        } else mc.options.useKey.setPressed(false);
        if (isPlacing && mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
            currentSaplingIndex++;
            isPlacing = false;
            mc.options.useKey.setPressed(false);
        }
    }

    private void boneMealSaplings() {
        int slot = findBoneMealSlot();
        if (slot == -1) {
            sendMessage("No bone meal!");
            state = FarmState.SEARCHING;
            return;
        }
        if (previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        BlockPos pos = saplingPositions.get(0);
        rotateTo(pos);

        boolean grown = false;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                Block block = mc.world.getBlockState(targetPodzol.add(dx, 1, dz)).getBlock();
                if (block != Blocks.SPRUCE_SAPLING && block != Blocks.AIR) {
                    grown = true;
                    break;
                }
            }
            if (grown) break;
        }

        if (grown) {
            mc.options.useKey.setPressed(false);
            state = FarmState.MINING;
            sendMessage("Tree grown! Starting to mine...");
            return;
        }

        mc.options.useKey.setPressed(!rotateChar.isActive());
    }

    private void mineLog() {
        mc.options.useKey.setPressed(false);
        if (logMined) {
            mc.options.attackKey.setPressed(false);
            waitTicks = 10;
            state = FarmState.WAIT;
            return;
        }
        BlockPos pos = targetPodzol.add(0, 1, 0);
        if (mc.world.getBlockState(pos).getBlock() == Blocks.SPRUCE_LOG) {
            rotateTo(pos);
            int slot = findBestAxeSlot();
            if (slot == -1) {
                sendMessage("No axe found!");
                state = FarmState.SEARCHING;
                return;
            }
            if (previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            mc.options.attackKey.setPressed(!rotateChar.isActive());
        } else {
            mc.options.attackKey.setPressed(false);
            logMined = true;
            waitTicks = 10;
            state = FarmState.WAIT;
        }
    }

    private void waitAfterMining() {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        state = FarmState.SEARCHING;
    }

    private void handleOrderRefill() {
        long now = System.currentTimeMillis();
        long timeInState = now - refillStageStart;
        int fastOrderDelay = orderDelay + random.nextInt(100);

        if (timeInState > 5000) {
            sendMessage("Order refill stage timeout. Resetting module...");
            if (mc.currentScreen != null) mc.currentScreen.close();
            reset();
            return;
        }

        switch (refillState) {
            case OPEN_ORDERS -> {
                if (timeInState < 100) return;
                mc.player.networkHandler.sendChatCommand("order");
                refillState = RefillState.WAIT_ORDERS_GUI;
                refillStageStart = now;
            }
            case WAIT_ORDERS_GUI -> {
                if (timeInState < fastOrderDelay) return;
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    refillState = RefillState.CLICK_SLOT_51;
                    refillStageStart = now;
                }
            }
            case CLICK_SLOT_51 -> {
                if (timeInState < fastOrderDelay) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
                ScreenHandler handler = screen.getScreenHandler();
                if (handler.slots.size() > 51) {
                    mc.interactionManager.clickSlot(handler.syncId, 51, 0, SlotActionType.PICKUP, mc.player);
                    refillState = RefillState.WAIT_SECOND_GUI;
                    refillStageStart = now;
                }
            }
            case WAIT_SECOND_GUI -> {
                if (timeInState < fastOrderDelay) return;
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    refillState = RefillState.CLICK_TARGET_ITEM;
                    refillStageStart = now;
                }
            }
            case CLICK_TARGET_ITEM -> {
                if (timeInState < fastOrderDelay) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
                if (findAndClickTargetItem(screen.getScreenHandler())) {
                    refillState = RefillState.WAIT_THIRD_GUI;
                    refillStageStart = now;
                }
            }
            case WAIT_THIRD_GUI -> {
                if (timeInState < fastOrderDelay) return;
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    refillState = RefillState.CLICK_CHEST_SLOT;
                    refillStageStart = now;
                }
            }
            case CLICK_CHEST_SLOT -> {
                if (timeInState < fastOrderDelay) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
                if (clickChestSlot(screen.getScreenHandler())) {
                    refillState = RefillState.WAIT_ITEMS_GUI;
                    refillStageStart = now;
                }
            }
            case WAIT_ITEMS_GUI -> {
                if (timeInState < fastOrderDelay) return;
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    refillState = RefillState.COLLECT_ITEMS;
                    refillStageStart = now;
                }
            }
            case COLLECT_ITEMS -> {
                if (timeInState < fastOrderDelay) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
                if (collectItems(screen.getScreenHandler())) {
                    refillStageStart = now;
                    if (stacksCollected >= ORDER_REFILL_AMOUNT) finishRefill(true);
                } else {
                    if (stacksCollected > 0) finishRefill(true);
                    else retryRefill("No items found to collect");
                }
            }
            case CLOSE_GUI -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                finishRefill(false);
            }
        }
    }

    private boolean findAndClickTargetItem(ScreenHandler handler) {
        for (int i = 0; i < Math.min(handler.slots.size(), 54); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.hasStack() && slot.getStack().getItem() == currentRefillItem) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }

    private boolean clickChestSlot(ScreenHandler handler) {
        for (int i : new int[]{11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24}) {
            if (handler.slots.size() > i) {
                Slot slot = handler.slots.get(i);
                if (slot.hasStack() && slot.getStack().getItem() == net.minecraft.item.Items.CHEST) {
                    mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean collectItems(ScreenHandler handler) {
        for (int i = 0; i < Math.min(handler.slots.size(), 54); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.hasStack() && slot.getStack().getItem() == currentRefillItem) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                stacksCollected++;
                return true;
            }
        }
        return false;
    }

    private void retryRefill(String reason) {
        refillRetryCount++;
        if (refillRetryCount < maxRefillRetries) {
            sendMessage("Refill failed (" + reason + "), retrying... (" + refillRetryCount + "/" + maxRefillRetries + ")");
            if (mc.currentScreen != null) mc.currentScreen.close();
            refillState = RefillState.OPEN_ORDERS;
            refillStageStart = System.currentTimeMillis() + 800 + random.nextInt(400);
        } else {
            sendMessage("Refill failed after max retries: " + reason);
            finishRefill(false);
        }
    }

    private void finishRefill(boolean success) {
        if (mc.currentScreen != null) mc.currentScreen.close();
        if (success) sendMessage("Refill completed! Collected " + stacksCollected + " stacks of " +
                (currentRefillItem != null ? currentRefillItem.getName().getString() : "items"));
        refillState = RefillState.NONE;
        currentRefillItem = null;
        stacksCollected = 0;
        refillRetryCount = 0;
    }

    enum FarmState {SEARCHING, PLANTING, BONEMEALING, MINING, WAIT}

    enum RefillState {NONE, OPEN_ORDERS, WAIT_ORDERS_GUI, CLICK_SLOT_51, WAIT_SECOND_GUI, CLICK_TARGET_ITEM, WAIT_THIRD_GUI, CLICK_CHEST_SLOT, WAIT_ITEMS_GUI, COLLECT_ITEMS, CLOSE_GUI}


}

