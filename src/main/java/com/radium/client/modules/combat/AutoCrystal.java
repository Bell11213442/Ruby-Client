package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.KeybindSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.CrystalUtil;
import com.radium.client.utils.InventoryUtil;
import com.radium.client.utils.WorldUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public final class AutoCrystal extends Module implements TickListener {
    private final KeybindSetting activateKey = new KeybindSetting("Activate Key", -1);
    private final NumberSetting placeDelay = new NumberSetting("Place Delay", 2.0, 0.0, 20.0, 1.0);
    private final NumberSetting breakDelay = new NumberSetting("Break Delay", 2.0, 0.0, 20.0, 1.0);
    private final NumberSetting placeChance = new NumberSetting("Place Chance", 100.0, 0.0, 100.0, 1.0);
    private final NumberSetting breakChance = new NumberSetting("Break Chance", 100.0, 0.0, 100.0, 1.0);
    private final BooleanSetting lootProtect = new BooleanSetting("Loot protect", false);
    private final BooleanSetting fakePunch = new BooleanSetting("Fake Punch", false);
    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", true);
    private final BooleanSetting damageTick = new BooleanSetting("Damage Tick", false);
    private final BooleanSetting antiWeakness = new BooleanSetting("Anti-Weakness", false);
    private final NumberSetting particleChance = new NumberSetting("Particle Chance", 20.0, 0.0, 100.0, 1.0);
    private final Random random = new Random();
    public boolean crystalling;
    private int placeClock;
    private int breakClock;

    public AutoCrystal() {
        super("Auto Crystal", "Automatically crystals fast for you", Category.COMBAT);
        this.addSettings(activateKey, placeDelay, breakDelay, placeChance, breakChance, lootProtect, fakePunch, clickSimulation, damageTick, antiWeakness, particleChance);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        this.placeClock = 0;
        this.breakClock = 0;
        this.crystalling = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        this.placeClock = 0;
        this.breakClock = 0;
        this.crystalling = false;
        super.onDisable();
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen == null) {
            boolean dontPlace = this.placeClock != 0;
            boolean dontBreak = this.breakClock != 0;


            if (this.lootProtect.getValue()) {
                if (WorldUtil.isDeadBodyNearby() || WorldUtil.isValuableLootNearby()) return;
            }

            int randomInt = random.nextInt(100) + 1;

            if (dontPlace) {
                --this.placeClock;
            }

            if (dontBreak) {
                --this.breakClock;
            }

            if (!mc.player.isDead()) {
                if (!this.damageTick.getValue() || !this.damageTickCheck()) {
                    if (this.activateKey.getValue() != -1 && !isKeyPressed(this.activateKey.getValue())) {
                        this.placeClock = 0;
                        this.breakClock = 0;
                        this.crystalling = false;
                    } else {
                        this.crystalling = true;


                        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                            HitResult target = mc.crosshairTarget;


                            if (target instanceof BlockHitResult hit) {
                                if (hit.getType() == HitResult.Type.BLOCK) {
                                    BlockPos pos = hit.getBlockPos();
                                    boolean isObsidian = mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN);
                                    boolean isBedrock = mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
                                    boolean canPlace = (isObsidian || isBedrock) && CrystalUtil.canPlaceCrystalClientAssumeObsidian(pos);

                                    if (!dontPlace && randomInt <= this.placeChance.getValue()) {
                                        if (canPlace) {
                                            WorldUtil.placeBlock(hit, true);
                                            this.placeClock = this.placeDelay.getValue().intValue();
                                        }


                                        if (this.fakePunch.getValue()) {
                                            if (!dontBreak && randomInt <= this.breakChance.getValue()) {
                                                if (isObsidian || isBedrock) return;

                                                mc.interactionManager.attackBlock(pos, hit.getSide());
                                                mc.player.swingHand(Hand.MAIN_HAND);
                                                this.breakClock = this.breakDelay.getValue().intValue();
                                            }
                                        }
                                    }
                                }
                            }


                            randomInt = random.nextInt(100) + 1;
                            if (target instanceof EntityHitResult hit) {
                                if (!dontBreak && randomInt <= this.breakChance.getValue()) {
                                    Entity entity = hit.getEntity();
                                    boolean validTarget = entity instanceof EndCrystalEntity || entity instanceof SlimeEntity;

                                    if (!this.fakePunch.getValue() && !validTarget) return;

                                    if (validTarget) {
                                        int previousSlot = mc.player.getInventory().selectedSlot;
                                        if (this.antiWeakness.getValue() && cantBreakCrystal()) {
                                            InventoryUtil.swapItem(item -> item instanceof SwordItem);
                                        }

                                        WorldUtil.hitEntity(entity, true);
                                        this.breakClock = this.breakDelay.getValue().intValue();

                                        if (this.antiWeakness.getValue()) {
                                            mc.player.getInventory().selectedSlot = previousSlot;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean cantBreakCrystal() {
        if (mc.player == null) return false;

        boolean hasWeakness = mc.player.hasStatusEffect(StatusEffects.WEAKNESS);
        boolean hasStrength = mc.player.hasStatusEffect(StatusEffects.STRENGTH);

        if (!hasWeakness) return false;

        int weaknessLvl = mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier();
        int strengthLvl = hasStrength ? mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() : -1;

        return strengthLvl <= weaknessLvl && !WorldUtil.isTool(mc.player.getMainHandStack());
    }

    private boolean damageTickCheck() {
        return false;
    }

    private boolean isKeyPressed(int key) {
        return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
    }
}

