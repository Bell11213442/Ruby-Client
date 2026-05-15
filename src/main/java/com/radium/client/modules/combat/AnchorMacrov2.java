package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.ItemUseListener;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.mixins.MinecraftClientAccessor;
import com.radium.client.modules.Module;
import com.radium.client.utils.*;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class AnchorMacrov2 extends Module implements TickListener, ItemUseListener {
    private final BooleanSetting whileUse = new BooleanSetting("While Use", false);
    private final BooleanSetting lootProtect = new BooleanSetting("Loot Protect", false);
    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", true);
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 1.0, 0.0, 20.0, 1.0);
    private final NumberSetting switchChance = new NumberSetting("Switch Chance", 100.0, 0.0, 100.0, 1.0);
    private final NumberSetting placeChance = new NumberSetting("Place Chance", 100.0, 0.0, 100.0, 1.0);
    private final NumberSetting glowstoneDelay = new NumberSetting("Glowstone Delay", 0.0, 0.0, 20.0, 1.0);
    private final NumberSetting glowstoneChance = new NumberSetting("Glowstone Chance", 100.0, 0.0, 100.0, 1.0);
    private final NumberSetting explodeDelay = new NumberSetting("Explode Delay", 1.0, 0.0, 20.0, 1.0);
    private final NumberSetting explodeChance = new NumberSetting("Explode Chance", 100.0, 0.0, 100.0, 1.0);
    private final NumberSetting explodeSlot = new NumberSetting("Explode Slot", 9.0, 1.0, 9.0, 1.0);
    private final BooleanSetting onlyOwn = new BooleanSetting("Only Own", false);
    private final BooleanSetting onlyCharge = new BooleanSetting("Only Charge", false);
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private int switchClock = 0;
    private int glowstoneClock = 0;
    private int explodeClock = 0;

    public AnchorMacrov2() {
        super("Anchor Macro v2", "Automatically blows up respawn anchors for you", Category.COMBAT);
        this.addSettings(whileUse, lootProtect, clickSimulation, placeChance, switchDelay, switchChance, glowstoneDelay, glowstoneChance, explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        RadiumClient.getEventManager().add(ItemUseListener.class, this);
        switchClock = 0;
        glowstoneClock = 0;
        explodeClock = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        RadiumClient.getEventManager().remove(ItemUseListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.currentScreen != null) return;

        if (whileUse.getValue() || (!mc.player.isUsingItem() && !WorldUtil.isTool(mc.player.getOffHandStack()))) {
            if (!lootProtect.getValue() || (!WorldUtil.isDeadBodyNearby() && !WorldUtil.isValuableLootNearby())) {
                if (KeyUtils.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                    HitResult hitResult = mc.crosshairTarget;
                    if (hitResult instanceof BlockHitResult hit) {
                        BlockPos pos = hit.getBlockPos();
                        if (BlockUtil.isBlockAtPosition(pos, Blocks.RESPAWN_ANCHOR)) {
                            if (onlyOwn.getValue() && !ownedAnchors.contains(pos)) return;


                            mc.options.useKey.setPressed(false);

                            if (BlockUtil.isAnchorNotCharged(pos)) {
                                if (MathUtils.randomInt(1, 100) <= placeChance.getValue().intValue()) {
                                    if (mc.player.getMainHandStack().getItem() != Items.GLOWSTONE) {
                                        if (switchClock < switchDelay.getValue().intValue()) {
                                            switchClock++;
                                            return;
                                        }

                                        if (MathUtils.randomInt(1, 100) <= switchChance.getValue().intValue()) {
                                            switchClock = 0;
                                            InventoryUtil.swap(Items.GLOWSTONE);
                                        }
                                    }

                                    if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
                                        if (glowstoneClock < glowstoneDelay.getValue().intValue()) {
                                            glowstoneClock++;
                                            return;
                                        }

                                        if (MathUtils.randomInt(1, 100) <= glowstoneChance.getValue().intValue()) {
                                            glowstoneClock = 0;
                                            if (clickSimulation.getValue())
                                                MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                                            ((MinecraftClientAccessor) mc).invokeDoItemUse();
                                        }
                                    }
                                }
                            }

                            if (BlockUtil.isAnchorCharged(pos)) {
                                int slot = explodeSlot.getValue().intValue() - 1;
                                if (mc.player.getInventory().selectedSlot != slot) {
                                    if (switchClock < switchDelay.getValue().intValue()) {
                                        switchClock++;
                                        return;
                                    }

                                    if (MathUtils.randomInt(1, 100) <= switchChance.getValue().intValue()) {
                                        switchClock = 0;
                                        InventoryUtil.swap(slot);
                                    }
                                }

                                if (mc.player.getInventory().selectedSlot == slot) {
                                    if (explodeClock < explodeDelay.getValue().intValue()) {
                                        explodeClock++;
                                        return;
                                    }

                                    if (MathUtils.randomInt(1, 100) <= explodeChance.getValue().intValue()) {
                                        explodeClock = 0;
                                        if (!onlyCharge.getValue()) {
                                            if (clickSimulation.getValue())
                                                MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                                            ((MinecraftClientAccessor) mc).invokeDoItemUse();
                                            ownedAnchors.remove(pos);
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

    @Override
    public void onItemUse(ItemUseListener.ItemUseEvent event) {
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult instanceof BlockHitResult hit) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR) {
                    Direction dir = hit.getSide();
                    BlockPos pos = hit.getBlockPos();
                    if (!mc.world.getBlockState(pos).isReplaceable()) {
                        ownedAnchors.add(pos.offset(dir));
                    } else {
                        ownedAnchors.add(pos);
                    }
                }

                BlockPos bp = hit.getBlockPos();
                if (BlockUtil.isAnchorCharged(bp)) {
                    ownedAnchors.remove(bp);
                }
            }
        }
    }
}

