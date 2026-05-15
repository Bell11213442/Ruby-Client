package com.radium.client.modules.misc;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.gui.RenderUtils;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.Character.CenterCharacter;
import com.radium.client.utils.Character.RotateCharacter;
import com.radium.client.utils.ChatUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

import static com.radium.client.client.RadiumClient.eventManager;

public class BaseDigger extends Module implements GameRenderListener {

    public StringSetting pos1Coords = new StringSetting("Pos 1 Coords", "0,0,0");
    public BooleanSetting setPos1 = new BooleanSetting("Set Pos 1", false);
    public StringSetting pos2Coords = new StringSetting("Pos 2 Coords", "0,0,0");
    public BooleanSetting setPos2 = new BooleanSetting("Set Pos 2", false);
    public BooleanSetting startMining = new BooleanSetting("Start Mining", false);
    public BooleanSetting stopMining = new BooleanSetting("Stop Mining", false);
    public BooleanSetting totemPopDisconnect = new BooleanSetting("Totem Pop Disconnect", true);
    public BooleanSetting autoEat = new BooleanSetting("Auto Eat", true);
    public BooleanSetting autoXP = new BooleanSetting("Auto XP Repair", true);
    public CenterCharacter centerCharacter;
    public DiggingState diggingState = DiggingState.NONE;
    float currentLayersYaw;
    boolean finishMDownCentering = false, finishMDownRotating = false;
    int playerY;
    boolean finishBDownRotating = false;
    BlockPos blockPos, originalPlayerPos;
    boolean finishCentering = false, finishRotating = false;
    private BlockPos pos1;
    private BlockPos pos2;
    private RotateCharacter rotateCharacter;
    private int minX, maxX, minZ, maxZ, minY, maxY;
    private boolean movingPositiveDirection = true;
    private boolean miningAlongX = true;
    private ClearLayerSubState clearSubState = ClearLayerSubState.INIT;
    private int currentLayerY;
    private boolean finishTurnRotating = false;
    private boolean finishFirstTurnCentering = false;
    private boolean finishSidestepCentering = false;
    private boolean finishFinalRotating = false;
    private BlockPos sidestepOriginalPos;
    private int rowsMined = 0, totalRows = 0, lastRowCoord = -999, lastTurnDirection = 0;
    private boolean isEating = false;
    private long eatStartTime = 0;
    private int previousSlot = -1;
    private DiggingState stateBeforeEating = DiggingState.NONE;
    private ClearLayerSubState subStateBeforeEating = ClearLayerSubState.INIT;

    private boolean isRepairing = false;
    private XPRepairState xpRepairState = XPRepairState.NONE;
    private ItemStack previousOffhandItem = ItemStack.EMPTY;
    private int xpBottleSlot = -1;
    private long xpThrowStartTime = 0;
    private long lastThrowTime = 0;
    private DiggingState stateBeforeRepair = DiggingState.NONE;
    private ClearLayerSubState subStateBeforeRepair = ClearLayerSubState.INIT;
    private boolean finishXPRotateDown = false;

    public BaseDigger() {
        super("Base Digger", "Digs your base out", Category.MISC);
        addSettings(pos1Coords, setPos1, pos2Coords, setPos2, startMining, stopMining, totemPopDisconnect, autoEat, autoXP);
    }

    @Override
    public void onEnable() {
        eventManager.add(GameRenderListener.class, this);
        centerCharacter = new CenterCharacter(RadiumClient.mc);
        rotateCharacter = new RotateCharacter(RadiumClient.mc);

        if (totemPopDisconnect.getValue()) {
            if (!hasTotemInOffhand()) {
                int newTotemSlot = findTotemInInventory();
                if (newTotemSlot != -1) moveTotemToOffhand(newTotemSlot);
                else {
                    disconnect("You don't have a TOTEM");
                }
            }
        }

        diggingState = DiggingState.NONE;
        currentLayersYaw = 0;
        isEating = false;
        isRepairing = false;
    }

    @Override
    public void onDisable() {
        eventManager.remove(GameRenderListener.class, this);
        mc.options.forwardKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
        isEating = false;
        isRepairing = false;
    }

    private boolean hasTotemInOffhand() {
        if (mc.player == null) return false;
        return mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
    }

    private int findTotemInInventory() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }

    private void moveTotemToOffhand(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        int totemSlot = slot < 9 ? slot + 36 : slot;

        mc.interactionManager.clickSlot(syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    void disconnect(String str) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getConnection().disconnect(
                    net.minecraft.text.Text.literal("Base Digger | " + str)
            );
            toggle();
        }
    }

    private void checkTotemPop() {
        if (isRepairing) return;

        if (!hasTotemInOffhand()) {
            disconnect("Totem POPPED");
        }
    }

    private int shouldEat() {
        if (!autoEat.getValue()) return 0;
        if (mc.player == null || mc.player.getAbilities().creativeMode) return 0;
        int food = mc.player.getHungerManager().getFoodLevel();
        if (food > 6) return 0;
        return hasFood() ? 1 : -1;
    }

    private boolean hasFood() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.contains(DataComponentTypes.FOOD))
                return true;
        }
        return false;
    }

    private int findBestFoodSlot() {
        int bestSlot = -1;
        float bestScore = -1f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) continue;

            float score = food.nutrition() + food.saturation() * 4f;
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private void startEating() {
        if (isEating || mc.player == null) return;
        int foodSlot = findBestFoodSlot();
        if (foodSlot == -1) return;

        previousSlot = mc.player.getInventory().selectedSlot;
        stateBeforeEating = diggingState;
        subStateBeforeEating = clearSubState;

        mc.options.attackKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);

        mc.player.getInventory().selectedSlot = foodSlot;
        mc.options.useKey.setPressed(true);
        isEating = true;
        eatStartTime = System.currentTimeMillis();
    }

    private void stopEatingAndResume() {
        if (!isEating) return;
        mc.options.useKey.setPressed(false);
        isEating = false;
        selectBestPickaxe();
        diggingState = stateBeforeEating;
        clearSubState = subStateBeforeEating;
    }

    private boolean shouldRepairPickaxe() {
        if (!autoXP.getValue() || mc.player == null) return false;

        ItemStack heldItem = mc.player.getMainHandStack();
        if (!(heldItem.getItem() instanceof PickaxeItem)) return false;

        if (!heldItem.isDamaged()) return false;

        int maxDamage = heldItem.getMaxDamage();
        int currentDamage = heldItem.getDamage();
        int durabilityLeft = maxDamage - currentDamage;
        return durabilityLeft < (maxDamage * 0.1);
    }

    private int findMostXPBottles() {
        if (mc.player == null) return -1;

        int bestSlot = -1;
        int maxBottles = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                if (stack.getCount() > maxBottles) {
                    maxBottles = stack.getCount();
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private void moveXPToOffhand(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        int xpSlot = slot < 9 ? slot + 36 : slot;

        mc.interactionManager.clickSlot(syncId, xpSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, xpSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private void startXPRepair() {
        if (isRepairing || mc.player == null) return;

        xpBottleSlot = findMostXPBottles();
        if (xpBottleSlot == -1) {
            disconnect("No XP bottles found!");
            return;
        }
        stateBeforeRepair = diggingState;
        subStateBeforeRepair = clearSubState;
        previousOffhandItem = mc.player.getOffHandStack().copy();
        mc.options.attackKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.useKey.setPressed(false);

        isRepairing = true;
        xpRepairState = XPRepairState.MOVE_XP_TO_OFFHAND;
    }

    private void handleXPRepair() {
        if (mc.player == null) return;

        switch (xpRepairState) {
            case MOVE_XP_TO_OFFHAND -> {
                moveXPToOffhand(xpBottleSlot);
                xpRepairState = XPRepairState.ROTATE_DOWN;
                finishXPRotateDown = false;
            }

            case ROTATE_DOWN -> {
                if (!finishXPRotateDown) {
                    if (!rotateCharacter.isActive()) {
                        rotateCharacter.rotate(mc.player.getYaw(), 90, () -> finishXPRotateDown = true);
                    }
                } else {
                    finishXPRotateDown = false;
                    xpRepairState = XPRepairState.THROW_XP;
                    xpThrowStartTime = System.currentTimeMillis();
                    lastThrowTime = 0;
                    mc.options.useKey.setPressed(false);
                }
            }

            case THROW_XP -> {
                ItemStack offhandStack = mc.player.getOffHandStack();
                boolean noMoreXP = offhandStack.isEmpty() || offhandStack.getItem() != Items.EXPERIENCE_BOTTLE;
                boolean timeout = System.currentTimeMillis() - xpThrowStartTime > 10000;

                ItemStack pickaxe = mc.player.getMainHandStack();
                boolean pickaxeRepaired = pickaxe.getItem() instanceof PickaxeItem && !pickaxe.isDamaged();

                if (noMoreXP || timeout || pickaxeRepaired) {
                    mc.options.useKey.setPressed(false);
                    lastThrowTime = 0;
                    xpRepairState = XPRepairState.RESTORE_OFFHAND;
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastThrowTime >= 100) {
                        mc.options.useKey.setPressed(false);
                        if (currentTime - lastThrowTime >= 150) {
                            mc.options.useKey.setPressed(true);
                            lastThrowTime = currentTime;
                        }
                    }
                }
            }

            case RESTORE_OFFHAND -> {
                if (previousOffhandItem.getItem() == Items.TOTEM_OF_UNDYING) {
                    int totemSlot = findTotemInInventory();
                    if (totemSlot != -1) {
                        moveTotemToOffhand(totemSlot);
                    } else if (totemPopDisconnect.getValue()) {
                        disconnect("Lost totem during XP repair!");
                        return;
                    }
                } else if (!previousOffhandItem.isEmpty()) {
                    for (int i = 0; i < 36; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (ItemStack.areEqual(stack, previousOffhandItem)) {
                            moveItemToOffhand(i);
                            break;
                        }
                    }
                }
                xpRepairState = XPRepairState.ROTATE_BACK;
                finishXPRotateDown = false;
            }

            case ROTATE_BACK -> {
                if (!finishXPRotateDown) {
                    if (!rotateCharacter.isActive()) {
                        rotateCharacter.rotate(mc.player.getYaw(), 0, () -> finishXPRotateDown = true);
                    }
                } else {
                    finishXPRotateDown = false;
                    xpRepairState = XPRepairState.RESUME;
                }
            }

            case RESUME -> {
                selectBestPickaxe();
                diggingState = stateBeforeRepair;
                clearSubState = subStateBeforeRepair;
                isRepairing = false;
                xpRepairState = XPRepairState.NONE;
            }
        }
    }

    private void moveItemToOffhand(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        int itemSlot = slot < 9 ? slot + 36 : slot;

        mc.interactionManager.clickSlot(syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private BlockPos parseCoordinates(String coords) {
        try {
            String[] parts = coords.trim().split(",");
            if (parts.length != 3) return null;

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());

            return new BlockPos(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private void updatePos1String() {
        if (pos1 != null) {
            pos1Coords.setValue(pos1.getX() + "," + pos1.getY() + "," + pos1.getZ());
        }
    }

    private void updatePos2String() {
        if (pos2 != null) {
            pos2Coords.setValue(pos2.getX() + "," + pos2.getY() + "," + pos2.getZ());
        }
    }

    public int getHighestBlockPos() {
        if (pos1.getY() > pos2.getY()) return 1;
        else if (pos1.getY() < pos2.getY()) return 2;
        else return 3;
    }

    public boolean isStandingOnHighestBlock() {
        int HighestBlockPos = getHighestBlockPos();
        BlockPos p = mc.player.getBlockPos();
        if (HighestBlockPos == 1) return p.equals(pos1.up());
        if (HighestBlockPos == 2) return p.equals(pos2.up());
        return p.equals(pos1.up()) || p.equals(pos2.up());
    }

    @Override
    public void onGameRender(GameRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (pos1 == null && pos2 == null) return;

        if (rotateCharacter != null && rotateCharacter.isActive()) {
            rotateCharacter.update(true, false);
        }

        MatrixStack matrices = event.matrices;
        Camera cam = mc.getBlockEntityRenderDispatcher().camera;
        if (cam == null) return;
        Vec3d camPos = cam.getPos();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180F));
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        if (pos1 != null) renderPosBox(matrices, pos1, new Color(0, 255, 0, 80));
        if (pos2 != null) renderPosBox(matrices, pos2, new Color(255, 255, 0, 80));
        matrices.pop();
    }

    private void renderPosBox(MatrixStack matrices, BlockPos pos, Color color) {
        float x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ();
        float x2 = x1 + 1, y2 = y1 + 1, z2 = z1 + 1;
        RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, color);
        int argb = (255 << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        RenderUtils.drawBox(matrices, x1, y1, z1, x2, y2, z2, argb, true);
    }

    private float getAlignedYaw() {
        if (pos1 == null || pos2 == null) return mc.player.getYaw();

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int px = mc.player.getBlockPos().getX();
        int pz = mc.player.getBlockPos().getZ();

        boolean nearMinX = (px == minX);
        boolean nearMaxX = (px == maxX);
        boolean nearMinZ = (pz == minZ);
        boolean nearMaxZ = (pz == maxZ);

        if (nearMinX && !nearMaxX) return -90f;
        if (nearMaxX && !nearMinX) return 90f;
        if (nearMinZ && !nearMaxZ) return 0f;
        if (nearMaxZ && !nearMinZ) return 180f;

        int lengthX = maxX - minX;
        int lengthZ = maxZ - minZ;

        if (lengthX >= lengthZ)
            return px <= (minX + maxX) / 2 ? -90f : 90f;
        else
            return pz <= (minZ + maxZ) / 2 ? 0f : 180f;
    }

    private void selectBestPickaxe() {
        int bestSlot = -1;
        float bestSpeed = -1f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof PickaxeItem pickaxe) {
                float speed = pickaxe.getMaterial().getMiningSpeedMultiplier();
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) mc.player.getInventory().selectedSlot = bestSlot;
    }

    void handleStarting() {
        if (mc.currentScreen != null)
            mc.execute(() -> mc.currentScreen.close());

        selectBestPickaxe();
        diggingState = DiggingState.MININGDOWN;
    }

    void handleMiningDown() {
        if (!finishMDownCentering) {
            if (!centerCharacter.isCentering()) {
                boolean needs = centerCharacter.initiate();
                if (!needs) {
                    finishMDownCentering = true;
                    playerY = mc.player.getBlockPos().getY();
                }
            } else {
                boolean still = centerCharacter.update();
                if (!still) {
                    finishMDownCentering = true;
                    playerY = mc.player.getBlockPos().getY();
                }
            }
            return;
        }

        if (!finishMDownRotating) {
            if (!rotateCharacter.isActive()) {
                currentLayersYaw = getAlignedYaw();
                rotateCharacter.rotate(getAlignedYaw(), 90, () -> finishMDownRotating = true);
            }
            return;
        }

        if (mc.player.getBlockPos().getY() > playerY - 2) {
            mc.options.attackKey.setPressed(true);
        } else {
            mc.options.attackKey.setPressed(false);
            diggingState = DiggingState.BREAKINGDOWNBLOCK;
            finishMDownCentering = false;
            finishMDownRotating = false;
        }
    }

    void handleBreaking() {
        if (!finishBDownRotating) {
            if (!rotateCharacter.isActive()) {
                rotateCharacter.rotate(mc.player.getYaw(), 67, () -> finishBDownRotating = true);
            }
            return;
        }

        if (blockPos == null) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK)
                blockPos = ((BlockHitResult) hit).getBlockPos();
        } else {
            if (mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR) {
                mc.options.attackKey.setPressed(true);
            } else {
                diggingState = DiggingState.WALKFORWARD;
                originalPlayerPos = mc.player.getBlockPos();
                mc.options.attackKey.setPressed(false);
                blockPos = null;
                finishBDownRotating = false;
            }
        }
    }

    void handleWalkForward() {
        BlockPos cur = mc.player.getBlockPos();

        if (cur.getX() == originalPlayerPos.getX() && cur.getZ() == originalPlayerPos.getZ()) {
            mc.options.forwardKey.setPressed(true);
        } else {
            mc.options.forwardKey.setPressed(false);
            diggingState = DiggingState.CENTERINGANDROTATING;
        }
    }

    void handleCentering() {
        if (!finishCentering) {
            if (!centerCharacter.isCentering()) {
                if (!centerCharacter.initiate()) {
                    finishCentering = true;
                    playerY = mc.player.getBlockPos().getY();
                }
            } else {
                if (!centerCharacter.update()) {
                    finishCentering = true;
                    playerY = mc.player.getBlockPos().getY();
                }
            }
            return;
        }

        if (!finishRotating) {
            if (!rotateCharacter.isActive()) {
                rotateCharacter.rotate(mc.player.getYaw(), 0, () -> finishRotating = true);
            }
            return;
        }

        finishCentering = false;
        finishRotating = false;
        diggingState = DiggingState.CLEARLAYER;
        clearSubState = ClearLayerSubState.INIT;
    }

    void initClearLayer() {
        minX = Math.min(pos1.getX(), pos2.getX());
        maxX = Math.max(pos1.getX(), pos2.getX());
        minZ = Math.min(pos1.getZ(), pos2.getZ());
        maxZ = Math.max(pos1.getZ(), pos2.getZ());
        minY = Math.min(pos1.getY(), pos2.getY());
        maxY = Math.max(pos1.getY(), pos2.getY());

        currentLayerY = mc.player.getBlockPos().getY();
        currentLayersYaw = normalizeYaw(currentLayersYaw);

        if (Math.abs(currentLayersYaw) < 45f) {
            miningAlongX = false;
            movingPositiveDirection = true;
        } else if (Math.abs(currentLayersYaw - 180f) < 45f || Math.abs(currentLayersYaw + 180f) < 45f) {
            miningAlongX = false;
            movingPositiveDirection = false;
        } else if (currentLayersYaw > 0) {
            miningAlongX = true;
            movingPositiveDirection = false;
        } else {
            miningAlongX = true;
            movingPositiveDirection = true;
        }

        if (miningAlongX) {
            int zRange = maxZ - minZ;
            int regularRows = (zRange / 3);
            int remainder = zRange % 3;
            totalRows = regularRows + 1 + (remainder >= 2 ? 1 : 0);
        } else {
            int xRange = maxX - minX;
            int regularRows = (xRange / 3);
            int remainder = xRange % 3;
            totalRows = regularRows + 1 + (remainder >= 2 ? 1 : 0);
        }

        rowsMined = 0;
        lastRowCoord = -999;
        lastTurnDirection = 0;
        clearSubState = ClearLayerSubState.MINING_FORWARD;

        finishTurnRotating = false;
        finishFirstTurnCentering = false;
        finishSidestepCentering = false;
        finishFinalRotating = false;
    }

    private float normalizeYaw(float yaw) {
        while (yaw > 180f) yaw -= 360f;
        while (yaw < -180f) yaw += 360f;
        return yaw;
    }

    boolean isAtBoundary() {
        int px = mc.player.getBlockPos().getX();
        int pz = mc.player.getBlockPos().getZ();
        if (miningAlongX) {
            return movingPositiveDirection ? px >= maxX : px <= minX;
        } else {
            return movingPositiveDirection ? pz >= maxZ : pz <= minZ;
        }
    }

    boolean isLayerComplete() {
        return rowsMined >= totalRows;
    }

    boolean isFullyDone() {
        return mc.player.getBlockPos().getY() <= minY && isLayerComplete();
    }

    float getFirstTurnYaw() {
        int turn;
        if (lastTurnDirection == 0) {
            int px = mc.player.getBlockPos().getX();
            int pz = mc.player.getBlockPos().getZ();

            if (miningAlongX) {
                int distMinZ = pz - minZ;
                int distMaxZ = maxZ - pz;
                boolean facingEast = Math.abs(currentLayersYaw + 90f) < 45f;
                turn = distMaxZ > distMinZ ? (facingEast ? 1 : -1) : (facingEast ? -1 : 1);
            } else {
                int distMinX = px - minX;
                int distMaxX = maxX - px;
                boolean facingSouth = Math.abs(currentLayersYaw) < 45f;
                turn = distMaxX > distMinX ? (facingSouth ? -1 : 1) : (facingSouth ? 1 : -1);
            }
        } else {
            turn = -lastTurnDirection;
        }
        lastTurnDirection = turn;
        return normalizeYaw(currentLayersYaw + (turn * 90f));
    }

    float getSecondTurnYaw() {
        return normalizeYaw(currentLayersYaw + 180f);
    }

    void handleClearingLayer() {
        switch (clearSubState) {
            case INIT -> initClearLayer();

            case MINING_FORWARD -> {
                if (!isAtBoundary()) {
                    mc.options.forwardKey.setPressed(true);
                    mc.options.attackKey.setPressed(true);
                } else {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.attackKey.setPressed(false);
                    rowsMined++;
                    lastRowCoord = miningAlongX ? mc.player.getBlockPos().getZ() : mc.player.getBlockPos().getX();
                    clearSubState = isLayerComplete() ? ClearLayerSubState.CHECK_LAYER_COMPLETE : ClearLayerSubState.TURNING_FIRST;
                }
            }

            case TURNING_FIRST -> {
                if (!finishTurnRotating) {
                    if (!rotateCharacter.isActive()) {
                        rotateCharacter.rotate(getFirstTurnYaw(), 9, () -> finishTurnRotating = true);
                    }
                } else {
                    finishTurnRotating = false;
                    clearSubState = ClearLayerSubState.FIRST_TURN_CENTER;
                }
            }

            case FIRST_TURN_CENTER -> {
                if (!finishFirstTurnCentering) {
                    if (!centerCharacter.isCentering()) {
                        if (!centerCharacter.initiate()) finishFirstTurnCentering = true;
                    } else {
                        if (!centerCharacter.update()) finishFirstTurnCentering = true;
                    }
                } else {
                    finishFirstTurnCentering = false;
                    sidestepOriginalPos = mc.player.getBlockPos();
                    clearSubState = ClearLayerSubState.SIDESTEP;
                }
            }

            case SIDESTEP -> {
                int dx = Math.abs(mc.player.getBlockPos().getX() - sidestepOriginalPos.getX());
                int dz = Math.abs(mc.player.getBlockPos().getZ() - sidestepOriginalPos.getZ());
                if (dx + dz < 4) {
                    mc.options.forwardKey.setPressed(true);
                    mc.options.attackKey.setPressed(true);
                } else {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.attackKey.setPressed(false);
                    clearSubState = ClearLayerSubState.SIDESTEP_BACK;
                }
            }

            case SIDESTEP_BACK -> {
                int dx = Math.abs(mc.player.getBlockPos().getX() - sidestepOriginalPos.getX());
                int dz = Math.abs(mc.player.getBlockPos().getZ() - sidestepOriginalPos.getZ());
                if (dx + dz > 3) {
                    mc.options.backKey.setPressed(true);
                } else {
                    mc.options.backKey.setPressed(false);
                    clearSubState = ClearLayerSubState.SIDESTEP_CENTER;
                }
            }

            case SIDESTEP_CENTER -> {
                if (!finishSidestepCentering) {
                    if (!centerCharacter.isCentering()) {
                        if (!centerCharacter.initiate()) finishSidestepCentering = true;
                    } else {
                        if (!centerCharacter.update()) finishSidestepCentering = true;
                    }
                } else {
                    finishSidestepCentering = false;
                    clearSubState = ClearLayerSubState.TURNING_SECOND;
                }
            }

            case TURNING_SECOND -> {
                if (!finishFinalRotating) {
                    if (!rotateCharacter.isActive()) {
                        rotateCharacter.rotate(getSecondTurnYaw(), 0, () -> finishFinalRotating = true);
                    }
                } else {
                    finishFinalRotating = false;
                    clearSubState = ClearLayerSubState.SECOND_TURN_CENTER;
                }
            }

            case SECOND_TURN_CENTER -> {
                if (!finishSidestepCentering) {
                    if (!centerCharacter.isCentering()) {
                        if (!centerCharacter.initiate()) finishSidestepCentering = true;
                    } else {
                        if (!centerCharacter.update()) finishSidestepCentering = true;
                    }
                } else {
                    finishSidestepCentering = false;
                    movingPositiveDirection = !movingPositiveDirection;
                    currentLayersYaw = getSecondTurnYaw();
                    clearSubState = ClearLayerSubState.MINING_FORWARD;
                }
            }

            case CHECK_LAYER_COMPLETE -> {
                mc.options.forwardKey.setPressed(false);
                mc.options.attackKey.setPressed(false);

                if (isFullyDone()) {
                    ChatUtils.m("Mining complete!");
                    disconnect("Mining Complete");
                    diggingState = DiggingState.NONE;
                } else {
                    clearSubState = ClearLayerSubState.INIT;
                    finishMDownCentering = false;
                    finishMDownRotating = false;
                    finishBDownRotating = false;
                    blockPos = null;
                    rowsMined = 0;
                    lastRowCoord = -999;
                    lastTurnDirection = 0;
                    diggingState = DiggingState.MININGDOWN;
                }
            }
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (mc.world == null || mc.player == null) return;

        handleButtons();

        if (totemPopDisconnect.getValue())
            checkTotemPop();
        if (isRepairing) {
            handleXPRepair();
            return;
        }
        if (!isEating && diggingState != DiggingState.NONE && shouldRepairPickaxe()) {
            startXPRepair();
            return;
        }
        if (!isEating && diggingState != DiggingState.NONE) {
            if (shouldEat() == 1) {
                startEating();
                return;
            }
            if (shouldEat() == -1) {
                disconnect("No Food!");
                return;
            }
        }

        if (isEating) {
            boolean isFull = mc.player.getHungerManager().getFoodLevel() >= 20;
            boolean noFoodLeft = mc.player.getMainHandStack().get(DataComponentTypes.FOOD) == null;
            boolean timeout = System.currentTimeMillis() - eatStartTime > 12000;

            if (isFull || noFoodLeft || timeout) {
                stopEatingAndResume();
            }
            return;
        }
        switch (diggingState) {
            case STARTING -> handleStarting();
            case MININGDOWN -> handleMiningDown();
            case BREAKINGDOWNBLOCK -> handleBreaking();
            case WALKFORWARD -> handleWalkForward();
            case CENTERINGANDROTATING -> handleCentering();
            case CLEARLAYER -> handleClearingLayer();
        }
    }

    public void handleButtons() {
        if (pos1 != null) {
            String currentPos1String = pos1.getX() + "," + pos1.getY() + "," + pos1.getZ();
            if (!pos1Coords.getValue().equals(currentPos1String)) {
                BlockPos parsedPos = parseCoordinates(pos1Coords.getValue());
                if (parsedPos != null) {
                    pos1 = parsedPos;
                    ChatUtils.m("Position 1 updated to " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ());
                } else {
                    ChatUtils.e("Invalid coordinates format for Pos 1. Use: x,y,z");
                    updatePos1String();
                }
            }
        } else {
            BlockPos parsedPos = parseCoordinates(pos1Coords.getValue());
            if (parsedPos != null && !pos1Coords.getValue().equals("0,0,0")) {
                pos1 = parsedPos;
                ChatUtils.m("Position 1 set to " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ());
            }
        }
        if (pos2 != null) {
            String currentPos2String = pos2.getX() + "," + pos2.getY() + "," + pos2.getZ();
            if (!pos2Coords.getValue().equals(currentPos2String)) {
                BlockPos parsedPos = parseCoordinates(pos2Coords.getValue());
                if (parsedPos != null) {
                    pos2 = parsedPos;
                    ChatUtils.m("Position 2 updated to " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ());
                } else {
                    ChatUtils.e("Invalid coordinates format for Pos 2. Use: x,y,z");
                    updatePos2String();
                }
            }
        } else {
            BlockPos parsedPos = parseCoordinates(pos2Coords.getValue());
            if (parsedPos != null && !pos2Coords.getValue().equals("0,0,0")) {
                pos2 = parsedPos;
                ChatUtils.m("Position 2 set to " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ());
            }
        }
        if (setPos1.getValue()) {
            setPos1.setValue(false);
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                pos1 = ((BlockHitResult) hit).getBlockPos();
                updatePos1String();
                ChatUtils.m("Position 1 set to " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ());
            } else {
                ChatUtils.e("Look at a block to set position 1");
            }
        }

        if (setPos2.getValue()) {
            setPos2.setValue(false);
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                pos2 = ((BlockHitResult) hit).getBlockPos();
                updatePos2String();
                ChatUtils.m("Position 2 set to " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ());
            } else {
                ChatUtils.e("Look at a block to set position 2");
            }
        }

        if (startMining.getValue()) {
            startMining.setValue(false);

            if (diggingState != DiggingState.NONE) {
                ChatUtils.e("Already mining!");
                return;
            }

            if (pos1 == null || pos2 == null) {
                ChatUtils.e("Please set both positions before starting");
                return;
            }

            if (isStandingOnHighestBlock()) {
                ChatUtils.m("Mining started!");
                diggingState = DiggingState.STARTING;
            } else {
                ChatUtils.e("Stand on the highest corner block to start");
            }
        }

        if (stopMining.getValue()) {
            stopMining.setValue(false);

            if (diggingState == DiggingState.NONE) {
                ChatUtils.e("Not currently mining");
                return;
            }

            mc.options.forwardKey.setPressed(false);
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            diggingState = DiggingState.NONE;
            isEating = false;
            isRepairing = false;

            ChatUtils.m("Mining stopped");
        }
    }

    public enum DiggingState {
        NONE,
        STARTING,
        MININGDOWN,
        BREAKINGDOWNBLOCK,
        WALKFORWARD,
        CENTERINGANDROTATING,
        CLEARLAYER
    }

    public enum ClearLayerSubState {
        INIT,
        MINING_FORWARD,
        TURNING_FIRST,
        FIRST_TURN_CENTER,
        SIDESTEP,
        SIDESTEP_BACK,
        SIDESTEP_CENTER,
        TURNING_SECOND,
        SECOND_TURN_CENTER,
        CHECK_LAYER_COMPLETE
    }

    public enum XPRepairState {
        NONE,
        MOVE_XP_TO_OFFHAND,
        ROTATE_DOWN,
        THROW_XP,
        RESTORE_OFFHAND,
        ROTATE_BACK,
        RESUME
    }
}