package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.AttackListener2;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.MouseMoveListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.MathUtils;
import com.radium.client.utils.RotationUtil;
import com.radium.client.utils.TimerUtil;
import com.radium.client.utils.WorldUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AimAssist extends Module implements GameRenderListener, MouseMoveListener, AttackListener2 {
    private final BooleanSetting stickyAim = new BooleanSetting("Sticky Aim", false);
    private final ModeSetting<WeaponMode> weaponMode = new ModeSetting<>("Filter", WeaponMode.WEAPONS_ONLY, WeaponMode.class);
    private final BooleanSetting onLeftClick = new BooleanSetting("On Left Click", false);
    private final ModeSetting<AimMode> aimAt = new ModeSetting<>("Aim At", AimMode.Head, AimMode.class);
    private final BooleanSetting stopAtTargetVertical = new BooleanSetting("Stop at Target Vert", true);
    private final BooleanSetting stopAtTargetHorizontal = new BooleanSetting("Stop at Target Horiz", false);
    private final NumberSetting radius = new NumberSetting("Radius", 5.0, 0.1, 6.0, 0.1);
    private final BooleanSetting seeOnly = new BooleanSetting("See Only", true);
    private final BooleanSetting lookAtNearest = new BooleanSetting("Look at Nearest", false);
    private final NumberSetting fov = new NumberSetting("FOV", 100.0, 5.0, 360.0, 1.0);

    private final NumberSetting minPitchSpeed = new NumberSetting("Min Vert Speed", 2.0, 0.0, 10.0, 0.1);
    private final NumberSetting maxPitchSpeed = new NumberSetting("Max Vert Speed", 4.0, 0.0, 10.0, 0.1);
    private final NumberSetting minYawSpeed = new NumberSetting("Min Horiz Speed", 2.0, 0.0, 10.0, 0.1);
    private final NumberSetting maxYawSpeed = new NumberSetting("Max Horiz Speed", 4.0, 0.0, 10.0, 0.1);

    private final NumberSetting speedChange = new NumberSetting("Speed Delay", 250.0, 0.0, 1000.0, 1.0);
    private final NumberSetting randomization = new NumberSetting("Chance", 50.0, 0.0, 100.0, 1.0);
    private final BooleanSetting yawAssist = new BooleanSetting("Horizontal", true);
    private final BooleanSetting pitchAssist = new BooleanSetting("Vertical", true);
    private final NumberSetting waitFor = new NumberSetting("Wait on Move", 0.0, 0.0, 1000.0, 1.0);
    private final ModeSetting<LerpMode> lerp = new ModeSetting<>("Lerp", LerpMode.Normal, LerpMode.class);

    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil resetSpeed = new TimerUtil();
    private boolean move;
    private float pitch;
    private float yaw;
    private PlayerEntity lastAttackedPlayer = null;

    public AimAssist() {
        super("Aim Assist", "Automatically aims at players for you", Category.COMBAT);
        this.addSettings(stickyAim, weaponMode, onLeftClick, aimAt, stopAtTargetVertical, stopAtTargetHorizontal, radius, seeOnly, lookAtNearest, fov,
                minPitchSpeed, maxPitchSpeed, minYawSpeed, maxYawSpeed, speedChange, randomization, yawAssist, pitchAssist, waitFor, lerp);
    }

    @Override
    public void onEnable() {
        this.move = true;
        this.pitch = getRandom(minPitchSpeed.getValue(), maxPitchSpeed.getValue());
        this.yaw = getRandom(minYawSpeed.getValue(), maxYawSpeed.getValue());
        RadiumClient.getEventManager().add(GameRenderListener.class, this);
        RadiumClient.getEventManager().add(MouseMoveListener.class, this);
        RadiumClient.getEventManager().add(com.radium.client.events.event.AttackListener2.class, this);
        this.timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(GameRenderListener.class, this);
        RadiumClient.getEventManager().remove(MouseMoveListener.class, this);
        RadiumClient.getEventManager().remove(com.radium.client.events.event.AttackListener2.class, this);
        super.onDisable();
    }

    @Override
    public void onAttack(AttackListener2.AttackEvent2 event) {
        if (event.entity instanceof PlayerEntity player) {
            lastAttackedPlayer = player;
        }
    }

    @Override
    public void onGameRender(GameRenderListener.GameRenderEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;

        if (this.timer.delay((float) this.waitFor.getValue().doubleValue()) && !this.move) {
            this.move = true;
            this.timer.reset();
        }

        Item heldItem = mc.player.getMainHandStack().getItem();
        switch (weaponMode.getValue()) {
            case MACE_ONLY -> {
                if (!(heldItem instanceof MaceItem)) return;
            }
            case WEAPONS_ONLY -> {
                if (!(heldItem instanceof SwordItem) && !(heldItem instanceof AxeItem)) return;
            }
            case MACE_AND_WEAPONS -> {
                if (!(heldItem instanceof SwordItem) && !(heldItem instanceof AxeItem) && !(heldItem instanceof MaceItem))
                    return;
            }
        }

        if (!this.onLeftClick.getValue() || GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            PlayerEntity target = WorldUtil.findNearestPlayer(mc.player, radius.getValue().floatValue(), seeOnly.getValue(), true);

            if (this.stickyAim.getValue() && lastAttackedPlayer != null) {
                if (mc.player.distanceTo(lastAttackedPlayer) < this.radius.getValue()) {
                    target = lastAttackedPlayer;
                }
                if (lastAttackedPlayer.isDead()) lastAttackedPlayer = null;
            }

            if (target != null) {
                if (this.resetSpeed.delay(this.speedChange.getValue().floatValue())) {
                    this.pitch = getRandom(minPitchSpeed.getValue(), maxPitchSpeed.getValue());
                    this.yaw = getRandom(minYawSpeed.getValue(), maxYawSpeed.getValue());
                    this.resetSpeed.reset();
                }

                Vec3d targetPos = target.getPos();
                if (this.aimAt.getValue() == AimMode.Head) {
                    targetPos = targetPos.add(0, target.getEyeHeight(target.getPose()), 0);
                } else if (this.aimAt.getValue() == AimMode.Chest) {
                    targetPos = targetPos.add(0, target.getHeight() * 0.7, 0);
                } else if (this.aimAt.getValue() == AimMode.Legs) {
                    targetPos = targetPos.add(0, target.getHeight() * 0.3, 0);
                }

                if (this.lookAtNearest.getValue()) {
                    double offsetX = mc.player.getX() - target.getX() > 0 ? 0.29 : -0.29;
                    double offsetZ = mc.player.getZ() - target.getZ() > 0 ? 0.29 : -0.29;
                    targetPos = targetPos.add(offsetX, 0.0, offsetZ);
                }

                float[] rotations = RotationUtil.getRotationsTo(targetPos);
                double angleToRotation = RotationUtil.getRotationDistance(targetPos);

                if (!(angleToRotation > fov.getValue() / 2.0)) {
                    float yawStrength = this.yaw / 50.0f;
                    float pitchStrength = this.pitch / 50.0f;

                    float currentYaw = mc.player.getYaw();
                    float currentPitch = mc.player.getPitch();

                    float targetYaw = rotations[0];
                    float targetPitch = rotations[1];

                    float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
                    float pitchDiff = MathHelper.wrapDegrees(targetPitch - currentPitch);

                    float fixedTargetYaw = currentYaw + yawDiff;
                    float fixedTargetPitch = currentPitch + pitchDiff;

                    if (this.lerp.getValue() == LerpMode.Smoothstep) {
                        currentYaw = (float) MathUtils.smoothStepLerp(yawStrength, currentYaw, fixedTargetYaw);
                        currentPitch = (float) MathUtils.smoothStepLerp(pitchStrength, currentPitch, fixedTargetPitch);
                    } else if (this.lerp.getValue() == LerpMode.Normal) {
                        currentYaw = MathUtils.lerp(currentYaw, fixedTargetYaw, yawStrength);
                        currentPitch = MathUtils.lerp(currentPitch, fixedTargetPitch, pitchStrength);
                    } else if (this.lerp.getValue() == LerpMode.EaseOut) {
                        currentYaw = MathUtils.lerp(currentYaw, fixedTargetYaw, yawStrength * 1.5f);
                        currentPitch = MathUtils.lerp(currentPitch, fixedTargetPitch, pitchStrength * 1.5f);
                    }

                    if (MathUtils.randomInt(1, 100) <= randomization.getValue().intValue() && this.move) {
                        if (this.yawAssist.getValue()) {
                            if (this.stopAtTargetHorizontal.getValue()) {
                                HitResult hit = WorldUtil.getHitResult(this.radius.getValue());
                                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target) {
                                    return;
                                }
                            }
                            mc.player.setYaw(currentYaw);
                        }

                        if (this.pitchAssist.getValue()) {
                            if (this.stopAtTargetVertical.getValue()) {
                                HitResult hit = WorldUtil.getHitResult(this.radius.getValue());
                                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target) {
                                    return;
                                }
                            }
                            mc.player.setPitch(currentPitch);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        this.move = false;
        this.timer.reset();
    }

    private float getRandom(double min, double max) {
        if (min >= max) return (float) min;
        return (float) (min + (max - min) * MathUtils.random.nextDouble());
    }

    public enum WeaponMode {
        MACE_ONLY("Mace Only"),
        WEAPONS_ONLY("Weapons Only"),
        MACE_AND_WEAPONS("Mace and Weapons"),
        ALL("All Items");

        private final String name;

        WeaponMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AimMode {Head, Chest, Legs}

    public enum LerpMode {Normal, Smoothstep, EaseOut}
}

