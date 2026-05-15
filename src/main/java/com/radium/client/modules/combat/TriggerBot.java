package com.radium.client.modules.combat;

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.AttackListener;
import com.radium.client.events.event.HandleInputListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.MinMaxSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.CombatUtils;
import com.radium.client.utils.TimerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class TriggerBot extends Module implements HandleInputListener, AttackListener {

    private final ModeSetting<Mode> mode = new ModeSetting<>("Mode", Mode.WEAPONS, Mode.class);
    private final BooleanSetting inScreen = new BooleanSetting("Work In Screen", false);
    private final BooleanSetting whileUse = new BooleanSetting("While Use", false);
    private final BooleanSetting onLeftClick = new BooleanSetting("On Left Click", false);
    private final MinMaxSetting swordDelay = new MinMaxSetting("Sword Delay", 0, 1000, 1, 540, 550);
    private final MinMaxSetting axeDelay = new MinMaxSetting("Axe Delay", 0, 1000, 1, 780, 800);
    private final BooleanSetting checkShield = new BooleanSetting("Check Shield", false);
    private final BooleanSetting onlyCritSword = new BooleanSetting("Only Crit Sword", false);
    private final BooleanSetting onlyCritAxe = new BooleanSetting("Only Crit Axe", false);
    private final BooleanSetting swing = new BooleanSetting("Swing Hand", true);
    private final BooleanSetting whileAscend = new BooleanSetting("While Ascending", false);
    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", true);
    private final BooleanSetting strayBypass = new BooleanSetting("Bypass Mode", true);
    private final BooleanSetting allEntities = new BooleanSetting("All Entities", false);
    private final BooleanSetting useShield = new BooleanSetting("Use Shield", false);
    private final NumberSetting shieldTime = new NumberSetting("Shield Time", 350, 100, 1000, 1);
    private final BooleanSetting sticky = new BooleanSetting("Same Player", false);

    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils shieldTimer = new TimerUtils();
    private int currentSwordDelay;
    private int currentAxeDelay;
    private boolean isMaceAttack;

    public TriggerBot() {
        super("TriggerBot", "Automatically hits players for you", Category.COMBAT);
        addSettings(mode, inScreen, whileUse, onLeftClick, swordDelay, axeDelay, checkShield,
                whileAscend, sticky, onlyCritSword, onlyCritAxe, swing, clickSimulation,
                strayBypass, allEntities, useShield, shieldTime);
    }

    @Override
    public void onEnable() {
        currentSwordDelay = swordDelay.getRandomValueInt();
        currentAxeDelay = axeDelay.getRandomValueInt();
        RadiumClient.getEventManager().add(HandleInputListener.class, this);
        RadiumClient.getEventManager().add(AttackListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(HandleInputListener.class, this);
        RadiumClient.getEventManager().remove(AttackListener.class, this);
        super.onDisable();
    }


    @Override
    public void onHandleInput() {
        try {
            if (!canRun()) return;

            Item item = mc.player.getMainHandStack().getItem();
            if (!isItemAllowed(item)) return;

            if (item instanceof MaceItem) handleMace();
            else if (item instanceof SwordItem) handleSword();
            else if (item instanceof AxeItem) handleAxe();
            else handleAllItems();

        } catch (Exception ignored) {
        }
    }

    private boolean canRun() {
        if (!inScreen.getValue() && mc.currentScreen != null) return false;

        if (onLeftClick.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1) return false;

        if (!whileUse.getValue() &&
                GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1) return false;

        return whileAscend.getValue() ||
                mc.player.isOnGround() || !(mc.player.getVelocity().y > 0);
    }

    private boolean isItemAllowed(Item item) {
        return switch (mode.getValue()) {
            case MACE -> item instanceof MaceItem;
            case WEAPONS -> item instanceof SwordItem || item instanceof AxeItem;
            case MACE_AND_WEAPONS -> item instanceof MaceItem || item instanceof SwordItem || item instanceof AxeItem;
            case ALL_ITEMS -> true;
        };
    }

    private Entity getTarget() {
        HitResult result = mc.crosshairTarget;
        if (!(result instanceof EntityHitResult hit)) return null;

        if (sticky.getValue() && hit.getEntity() != mc.targetedEntity) return null;

        return hit.getEntity();
    }

    private boolean isValidTarget(Entity entity, boolean critCheck) {
        if (entity == null || !entity.isAlive()) return false;

        boolean typeValid = entity instanceof PlayerEntity
                || (strayBypass.getValue() && entity instanceof HostileEntity)
                || allEntities.getValue();
        if (!typeValid) return false;

        if (entity instanceof PlayerEntity player) {
            if (checkShield.getValue() && player.isBlocking() && !CombatUtils.isShieldFacingAway(player)) return false;
        }

        if (critCheck) return canCrit();
        return true;
    }

    private boolean canCrit() {
        return !mc.player.isOnGround() &&
                mc.player.getVelocity().y < 0 &&
                !mc.player.isUsingItem() &&
                !mc.player.isInFluid();
    }

    private void handleMace() {
        Entity entity = getTarget();
        if (!isValidTarget(entity, onlyCritSword.getValue())) return;

        isMaceAttack = true;
        disableShieldIfNeeded();
        doAttack(entity);
        isMaceAttack = false;
    }

    private void handleSword() {
        Entity entity = getTarget();
        if (!isValidTarget(entity, onlyCritSword.getValue())) return;

        if (!timer.delay(currentSwordDelay)) return;

        disableShieldIfNeeded();
        doAttack(entity);

        currentSwordDelay = swordDelay.getRandomValueInt();
        timer.reset();
    }

    private void handleAxe() {
        Entity entity = getTarget();
        if (!isValidTarget(entity, onlyCritAxe.getValue())) return;

        if (!timer.delay(currentAxeDelay)) return;

        disableShieldIfNeeded();
        doAttack(entity);

        currentAxeDelay = axeDelay.getRandomValueInt();
        timer.reset();
    }

    private void handleAllItems() {
        Entity entity = getTarget();
        if (!isValidTarget(entity, onlyCritSword.getValue())) return;

        if (!timer.delay(currentSwordDelay)) return;

        disableShieldIfNeeded();
        doAttack(entity);

        currentSwordDelay = swordDelay.getRandomValueInt();
        timer.reset();
    }

    private void doAttack(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);

        if (clickSimulation.getValue()) {
            mc.options.attackKey.setPressed(true);
            mc.options.attackKey.setPressed(false);
        }
    }

    private void disableShieldIfNeeded() {
        if (!useShield.getValue()) return;
        if (mc.player.getOffHandStack().getItem() != Items.SHIELD) return;
        if (!mc.player.isBlocking()) return;

        mc.options.useKey.setPressed(false);
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1) {
            event.cancel();
        }
    }

    public boolean isMaceAttackActive() {
        return isMaceAttack;
    }

    public enum Mode {
        MACE("Mace"),
        WEAPONS("Weapons"),
        MACE_AND_WEAPONS("Mace and Weapons"),
        ALL_ITEMS("All Items");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
