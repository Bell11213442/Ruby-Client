package com.radium.client.modules.misc;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.EventManager;
import com.radium.client.events.event.GameRenderListener;
import com.radium.client.events.event.KeyListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.KeyUtils;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public final class Freecam extends Module implements TickListener, GameRenderListener, KeyListener {

    public final Vector3d currentPosition = new Vector3d();
    public final Vector3d previousPosition = new Vector3d();
    private final NumberSetting speed = new NumberSetting("Speed", 5, 1, 20, 0.1);
    public float yaw;
    public float pitch;
    public float previousYaw;
    public float previousPitch;
    private Perspective currentPerspective;
    private boolean isMovingForward;
    private boolean isMovingBackward;
    private boolean isMovingRight;
    private boolean isMovingLeft;
    private boolean isMovingUp;
    private boolean isMovingDown;
    private int WaitTicks;

    private long lastFrameTime;

    public Freecam() {
        super("Freecam", "Move freely", Category.MISC);
        addSettings(this.speed);


        RadiumClient.sendKeepAliveIfAllowed();
    }

    @Override
    public void onEnable() {

        RadiumClient.sendKeepAliveIfAllowed();

        EventManager eventManager = RadiumClient.getEventManager();
        eventManager.add(TickListener.class, this);
        eventManager.add(GameRenderListener.class, this);
        eventManager.add(KeyListener.class, this);

        if (mc.player == null || mc.world == null || mc.options == null) {
            this.toggle();
            return;
        }

        mc.options.getFovEffectScale().setValue(0.0);
        mc.options.getBobView().setValue(false);

        this.yaw = mc.player.getYaw();
        this.pitch = mc.player.getPitch();
        this.currentPerspective = mc.options.getPerspective();

        Vec3d eyePos = mc.player.getEyePos();
        this.currentPosition.set(eyePos.x, eyePos.y, eyePos.z);
        this.previousPosition.set(eyePos.x, eyePos.y, eyePos.z);

        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);

        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            this.yaw += 180.0F;
            this.pitch *= -1.0F;
        }

        this.previousYaw = this.yaw;
        this.previousPitch = this.pitch;

        this.isMovingForward = mc.options.forwardKey.isPressed();
        this.isMovingBackward = mc.options.backKey.isPressed();
        this.isMovingRight = mc.options.rightKey.isPressed();
        this.isMovingLeft = mc.options.leftKey.isPressed();
        this.isMovingUp = mc.options.jumpKey.isPressed();
        this.isMovingDown = mc.options.sneakKey.isPressed();

        this.lastFrameTime = System.currentTimeMillis();
        this.resetMovementKeys();

        super.onEnable();
    }

    @Override
    public void onDisable() {
        EventManager eventManager = RadiumClient.getEventManager();
        eventManager.remove(TickListener.class, this);
        eventManager.remove(GameRenderListener.class, this);
        eventManager.remove(KeyListener.class, this);

        this.resetMovementKeys();

        this.previousPosition.set(this.currentPosition);
        this.previousYaw = this.yaw;
        this.previousPitch = this.pitch;

        if (this.currentPerspective != null && mc.options != null) {
            mc.options.setPerspective(this.currentPerspective);
        } else if (mc.options != null) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }

        super.onDisable();
    }

    private void resetMovementKeys() {
        if (mc.options == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    @Override
    public void onTick2() {
        if (mc.player == null) return;

        if (mc.getCameraEntity() != null)
            mc.getCameraEntity().noClip = true;

        resetMovementKeys();
    }

    @Override
    public void onGameRender(final GameRenderEvent event) {
        previousPosition.set(currentPosition);
        previousYaw = yaw;
        previousPitch = pitch;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);
        if (deltaTime < 0.001f) deltaTime = 0.016f;

        final Vec3d forward = Vec3d.fromPolar(0.0f, yaw);
        final Vec3d right = Vec3d.fromPolar(0.0f, yaw + 90.0f);

        double moveX = 0, moveY = 0, moveZ = 0;
        double moveSpeed = this.speed.getValue() * 2 * (mc.options.sprintKey.isPressed() ? 2.0 : 1.0);

        if (isMovingForward) {
            moveX += forward.x * moveSpeed;
            moveZ += forward.z * moveSpeed;
        }
        if (isMovingBackward) {
            moveX -= forward.x * moveSpeed;
            moveZ -= forward.z * moveSpeed;
        }
        if (isMovingRight) {
            moveX += right.x * moveSpeed;
            moveZ += right.z * moveSpeed;
        }
        if (isMovingLeft) {
            moveX -= right.x * moveSpeed;
            moveZ -= right.z * moveSpeed;
        }
        if (isMovingUp) moveY += moveSpeed;
        if (isMovingDown) moveY -= moveSpeed;

        currentPosition.x += moveX * deltaTime * 5.0;
        currentPosition.y += moveY * deltaTime * 5.0;
        currentPosition.z += moveZ * deltaTime * 5.0;
    }

    @Override
    public void onKey(final KeyEvent keyEvent) {
        if (KeyUtils.isKeyPressed(292)) return;

        if (mc.options == null) return;

        boolean handled = true;
        if (mc.options.forwardKey.matchesKey(keyEvent.key, 0)) {
            isMovingForward = keyEvent.mode != 0;
            mc.options.forwardKey.setPressed(false);
        } else if (mc.options.backKey.matchesKey(keyEvent.key, 0)) {
            isMovingBackward = keyEvent.mode != 0;
            mc.options.backKey.setPressed(false);
        } else if (mc.options.rightKey.matchesKey(keyEvent.key, 0)) {
            isMovingRight = keyEvent.mode != 0;
            mc.options.rightKey.setPressed(false);
        } else if (mc.options.leftKey.matchesKey(keyEvent.key, 0)) {
            isMovingLeft = keyEvent.mode != 0;
            mc.options.leftKey.setPressed(false);
        } else if (mc.options.jumpKey.matchesKey(keyEvent.key, 0)) {
            isMovingUp = keyEvent.mode != 0;
            mc.options.jumpKey.setPressed(false);
        } else if (mc.options.sneakKey.matchesKey(keyEvent.key, 0)) {
            isMovingDown = keyEvent.mode != 0;
            mc.options.sneakKey.setPressed(false);
        } else {
            handled = false;
        }

        if (handled) keyEvent.cancel();
    }

    public void updateRotation(double deltaYaw, double deltaPitch) {
        yaw += (float) deltaYaw;
        pitch += (float) deltaPitch;
        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(pitch, -90f, 90f);
    }

    public double getInterpolatedX(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousPosition.x, currentPosition.x);
    }

    public double getInterpolatedY(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousPosition.y, currentPosition.y);
    }

    public double getInterpolatedZ(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousPosition.z, currentPosition.z);
    }

    public double getInterpolatedYaw(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousYaw, yaw);
    }

    public double getInterpolatedPitch(float partialTicks) {
        return MathHelper.lerp(partialTicks, previousPitch, pitch);
    }
}
