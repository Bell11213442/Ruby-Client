package com.radium.client.modules.combat;
// radium client

import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.KeybindSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.BlockUtil;
import com.radium.client.utils.InventoryUtil;
import com.radium.client.utils.KeyUtils;
import com.radium.client.utils.RotationUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static com.radium.client.client.RadiumClient.eventManager;

public final class SafeAnchorMacro extends Module implements TickListener, GameRenderListener {
    private final KeybindSetting triggerKey = new KeybindSetting("Trigger Key", 71);
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 2.0, 0.0, 20.0, 1.0);
    private final NumberSetting totemSlot = new NumberSetting("Totem Slot", 9, 1.0, 9.0, 1.0);
    private final NumberSetting range = new NumberSetting("Range", 4.5, 3.0, 6.0, 0.1);
    private final BooleanSetting useSilentRotations = new BooleanSetting("Silent Rotations", false);
    private final BooleanSetting smoothRotations = new BooleanSetting("Smooth Rotations", true);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 180.0, 30.0, 360.0, 10.0);
    private final BooleanSetting useEasing = new BooleanSetting("Use Easing", true);
    private final NumberSetting easingStrength = new NumberSetting("Easing Strength", 3.0, 1.0, 5.0, 0.5);

    private int delayCounter = 0;
    private int step = 0;
    private boolean isActive = false;
    private BlockPos targetAnchorPos = null;
    private BlockPos protectionBlockPos = null;

    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean needsRotation = false;
    private boolean rotationComplete = false;
    private long lastFrameTime = 0;

    private float startYaw = 0.0f;
    private float startPitch = 0.0f;
    private float rotationProgress = 0.0f;
    private float totalRotationDistance = 0.0f;

    private Runnable pendingAction = null;

    public SafeAnchorMacro() {
        super("SafeAnchor", "Places anchor, charges it, protects you with glowstone, and explodes", Category.COMBAT);
        this.addSettings(this.triggerKey, this.switchDelay, this.totemSlot, this.range,
                this.useSilentRotations, this.smoothRotations, this.rotationSpeed, this.useEasing, this.easingStrength);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        eventManager.add(TickListener.class, this);
        eventManager.add(GameRenderListener.class, this);
        lastFrameTime = System.nanoTime();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(TickListener.class, this);
        eventManager.remove(GameRenderListener.class, this);
        reset();
    }

    @Override
    public void onGameRender(final GameRenderEvent event) {
        if (mc.player == null || !this.isActive || !this.smoothRotations.getValue()) {
            return;
        }

        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = currentTime;

        if (needsRotation && !rotationComplete) {
            if (this.useEasing.getValue()) {
                float rotationSpeed = this.rotationSpeed.getValue().floatValue();

                float progressIncrement = (rotationSpeed * deltaTime) / totalRotationDistance;
                rotationProgress += progressIncrement;
                rotationProgress = Math.min(rotationProgress, 1.0f);

                float easingStrength = this.easingStrength.getValue().floatValue();
                float easedProgress = applyEasing(rotationProgress, easingStrength);

                float yawDiff = RotationUtil.normalizeYaw(targetYaw - startYaw);
                currentYaw = RotationUtil.normalizeYaw(startYaw + yawDiff * easedProgress);

                float pitchDiff = targetPitch - startPitch;
                currentPitch = startPitch + pitchDiff * easedProgress;

                currentPitch = Math.max(-90.0f, Math.min(90.0f, currentPitch));

                if (this.useSilentRotations.getValue()) {
                    RotationUtil.setSilentRotation(currentYaw, currentPitch);
                } else {
                    mc.player.setYaw(currentYaw);
                    mc.player.setPitch(currentPitch);
                }

                if (rotationProgress >= 0.98f) {
                    rotationComplete = true;
                    currentYaw = targetYaw;
                    currentPitch = targetPitch;

                    if (pendingAction != null) {
                        pendingAction.run();
                        pendingAction = null;
                    }
                }
            } else {
                float rotationSpeed = this.rotationSpeed.getValue().floatValue();
                float maxRotationThisFrame = rotationSpeed * deltaTime;

                float yawDiff = RotationUtil.normalizeYaw(targetYaw - currentYaw);
                if (Math.abs(yawDiff) <= maxRotationThisFrame) {
                    currentYaw = targetYaw;
                } else {
                    currentYaw = RotationUtil.normalizeYaw(currentYaw + (yawDiff > 0 ? maxRotationThisFrame : -maxRotationThisFrame));
                }

                float pitchDiff = targetPitch - currentPitch;
                if (Math.abs(pitchDiff) <= maxRotationThisFrame) {
                    currentPitch = targetPitch;
                } else {
                    currentPitch += (pitchDiff > 0 ? maxRotationThisFrame : -maxRotationThisFrame);
                }

                currentPitch = Math.max(-90.0f, Math.min(90.0f, currentPitch));

                if (this.useSilentRotations.getValue()) {
                    RotationUtil.setSilentRotation(currentYaw, currentPitch);
                } else {
                    mc.player.setYaw(currentYaw);
                    mc.player.setPitch(currentPitch);
                }

                float yawError = Math.abs(RotationUtil.normalizeYaw(targetYaw - currentYaw));
                float pitchError = Math.abs(targetPitch - currentPitch);

                if (yawError < 1.0f && pitchError < 1.0f) {
                    rotationComplete = true;
                    currentYaw = targetYaw;
                    currentPitch = targetPitch;

                    if (pendingAction != null) {
                        pendingAction.run();
                        pendingAction = null;
                    }
                }
            }
        }
    }

    private float applyEasing(float t, float strength) {
        float exponent = strength;
        return 1.0f - (float) Math.pow(1.0f - t, exponent);
    }

    private float easeOutCubic(float t) {
        float f = t - 1.0f;
        return f * f * f + 1.0f;
    }

    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        } else {
            float f = 2.0f * t - 2.0f;
            return 0.5f * f * f * f + 1.0f;
        }
    }

    @Override
    public void onTick2() {
        if (mc.currentScreen != null || mc.player == null) {
            return;
        }

        if (!this.hasRequiredItems()) {
            if (this.isActive) {
                this.reset();
            }
            return;
        }

        if (!this.isActive && !this.checkTriggerKey()) {
            return;
        }

        if (needsRotation && !rotationComplete && this.smoothRotations.getValue()) {
            return;
        }

        if (this.delayCounter < this.switchDelay.getValue().intValue()) {
            ++this.delayCounter;
            return;
        }
        this.delayCounter = 0;

        switch (this.step) {
            case 0:
                if (!findTargetPosition()) {
                    this.reset();
                    return;
                }
                this.step++;
                break;

            case 1:
                if (!placeBlock(this.targetAnchorPos, Items.RESPAWN_ANCHOR)) {
                    this.reset();
                    return;
                }
                this.step++;
                break;

            case 2:
                if (!chargeAnchor(this.targetAnchorPos)) {
                    this.reset();
                    return;
                }
                this.step++;
                break;

            case 3:
                if (!isReplaceable(this.protectionBlockPos)) {
                    this.step++;
                    break;
                }

                BlockPos below = this.protectionBlockPos.down();
                if (isReplaceable(below)) {
                    this.step++;
                    break;
                }

                placeBlock(this.protectionBlockPos, Items.GLOWSTONE);
                this.step++;
                break;

            case 4:
                InventoryUtil.swap(this.totemSlot.getValue().intValue() - 1);
                this.step++;
                break;

            case 5:

                if (hasLootNearby(this.targetAnchorPos)) {
                    this.reset();
                    return;
                }
                if (!interactWithBlock(this.targetAnchorPos, null)) {
                    this.reset();
                    return;
                }
                this.step++;
                break;

            case 6:
                this.reset();
                break;
        }
    }

    private void setTargetRotation(Vec3d targetPos, Runnable action) {
        Vec3d playerPos = mc.player.getEyePos();
        float[] rotation = RotationUtil.getRotationsTo(playerPos, targetPos);

        targetYaw = rotation[0];
        targetPitch = rotation[1];

        if (this.smoothRotations.getValue()) {
            if (!needsRotation) {
                if (this.useSilentRotations.getValue()) {
                    currentYaw = RotationUtil.getSilentYaw();
                    currentPitch = RotationUtil.getSilentPitch();
                } else {
                    currentYaw = mc.player.getYaw();
                    currentPitch = mc.player.getPitch();
                }
            } else {
                startYaw = currentYaw;
                startPitch = currentPitch;
            }

            if (!needsRotation) {
                startYaw = currentYaw;
                startPitch = currentPitch;
            }

            rotationProgress = 0.0f;

            float yawDiff = Math.abs(RotationUtil.normalizeYaw(targetYaw - startYaw));
            float pitchDiff = Math.abs(targetPitch - startPitch);
            totalRotationDistance = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

            if (totalRotationDistance < 0.1f) {
                totalRotationDistance = 0.1f;
            }

            needsRotation = true;
            rotationComplete = false;
            pendingAction = action;
        } else {
            currentYaw = targetYaw;
            currentPitch = targetPitch;

            if (this.useSilentRotations.getValue()) {
                RotationUtil.setSilentRotation(targetYaw, targetPitch);
            } else {
                mc.player.setYaw(targetYaw);
                mc.player.setPitch(targetPitch);
            }

            if (action != null) {
                action.run();
            }
        }
    }

    private boolean findTargetPosition() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hitResult)) {
            return false;
        }

        BlockPos hitPos = hitResult.getBlockPos();

        if (isReplaceable(hitPos)) {
            this.targetAnchorPos = hitPos;
        } else {
            Direction side = hitResult.getSide();
            this.targetAnchorPos = hitPos.offset(side);
        }

        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(this.targetAnchorPos));
        if (distance > this.range.getValue()) {
            return false;
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d anchorPos = Vec3d.ofCenter(this.targetAnchorPos);

        Vec3d midpoint = playerPos.add(anchorPos).multiply(0.5);

        BlockPos protectionPos = new BlockPos(
                (int) Math.floor(midpoint.x),
                mc.player.getBlockY(),
                (int) Math.floor(midpoint.z)
        );

        if (protectionPos.equals(this.targetAnchorPos) || protectionPos.equals(mc.player.getBlockPos())) {
            Direction facing = Direction.fromRotation(mc.player.getYaw());
            protectionPos = mc.player.getBlockPos().offset(facing);

            if (protectionPos.equals(this.targetAnchorPos)) {
                protectionPos = mc.player.getBlockPos();
            }
        }

        this.protectionBlockPos = protectionPos;

        return true;
    }

    private boolean isReplaceable(BlockPos pos) {
        if (mc.world == null) return false;

        var blockState = mc.world.getBlockState(pos);
        var block = blockState.getBlock();

        if (blockState.isAir()) return true;

        return block == Blocks.SHORT_GRASS ||
                block == Blocks.TALL_GRASS ||
                block == Blocks.FERN ||
                block == Blocks.LARGE_FERN ||
                block == Blocks.DEAD_BUSH ||
                block == Blocks.VINE ||
                block == Blocks.FIRE ||
                block == Blocks.SOUL_FIRE ||
                block == Blocks.WATER ||
                block == Blocks.LAVA ||
                block == Blocks.SNOW ||
                block == Blocks.SEAGRASS ||
                block == Blocks.TALL_SEAGRASS ||
                block == Blocks.KELP ||
                block == Blocks.KELP_PLANT ||
                blockState.isReplaceable();
    }

    private boolean placeBlock(BlockPos pos, net.minecraft.item.Item item) {
        if (pos == null || !isReplaceable(pos)) {
            return false;
        }

        InventoryUtil.swap(item);

        BlockHitResult hitResult = findPlacementSide(pos);
        if (hitResult == null) {
            return false;
        }

        if (item == Items.RESPAWN_ANCHOR) {
            BlockUtil.interactWithBlock(hitResult, true);
        } else {
            setTargetRotation(hitResult.getPos(), () -> {
                BlockUtil.interactWithBlock(hitResult, true);
            });
        }

        return true;
    }

    private BlockHitResult findPlacementSide(BlockPos pos) {
        Direction[] directions = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

        for (Direction dir : directions) {
            BlockPos neighbor = pos.offset(dir);

            if (!isReplaceable(neighbor)) {
                return new BlockHitResult(
                        Vec3d.ofCenter(neighbor).add(dir.getOpposite().getVector().getX() * 0.5,
                                dir.getOpposite().getVector().getY() * 0.5,
                                dir.getOpposite().getVector().getZ() * 0.5),
                        dir.getOpposite(),
                        neighbor,
                        false
                );
            }
        }

        return null;
    }

    private boolean interactWithBlock(BlockPos pos, net.minecraft.item.Item item) {
        if (pos == null) {
            return false;
        }

        if (item != null) {
            InventoryUtil.swap(item);
        }

        Direction targetFace = Direction.UP;
        Vec3d targetPos = Vec3d.ofCenter(pos).add(
                targetFace.getOffsetX() * 0.5,
                targetFace.getOffsetY() * 0.5,
                targetFace.getOffsetZ() * 0.5
        );

        Vec3d eyePos = mc.player.getEyePos();
        if (targetPos.y > eyePos.y) {
            targetPos = Vec3d.ofCenter(pos);
        }

        setTargetRotation(targetPos, () -> {
            BlockHitResult hitResult = new BlockHitResult(
                    Vec3d.ofCenter(pos),
                    Direction.UP,
                    pos,
                    false
            );

            BlockUtil.interactWithBlock(hitResult, true);
        });

        return true;
    }

    private boolean chargeAnchor(BlockPos pos) {
        if (pos == null || isReplaceable(pos)) {
            return false;
        }

        InventoryUtil.swap(Items.GLOWSTONE);

        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(pos),
                Direction.UP,
                pos,
                false
        );

        BlockUtil.interactWithBlock(hitResult, false);

        return true;
    }

    private boolean hasRequiredItems() {
        boolean hasAnchor = false;
        boolean hasGlowstone = false;

        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.RESPAWN_ANCHOR)) {
                hasAnchor = true;
            }
            if (stack.getItem().equals(Items.GLOWSTONE)) {
                hasGlowstone = true;
            }
        }

        return hasAnchor && hasGlowstone;
    }

    private boolean checkTriggerKey() {
        int key = this.triggerKey.getValue();
        if (key == -1 || !KeyUtils.isKeyPressed(key)) {
            return false;
        }
        this.isActive = true;
        return true;
    }

    private void reset() {
        this.isActive = false;
        this.step = 0;
        this.delayCounter = 0;
        this.targetAnchorPos = null;
        this.protectionBlockPos = null;
        this.needsRotation = false;
        this.rotationComplete = false;
        this.pendingAction = null;
        this.rotationProgress = 0.0f;

        if (this.useSilentRotations.getValue()) {
            RotationUtil.resetSilentRotation();
        }
    }

    public boolean isActive() {
        return this.isActive;
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

