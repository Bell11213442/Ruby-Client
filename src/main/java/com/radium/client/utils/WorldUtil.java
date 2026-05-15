package com.radium.client.utils;
// radium client

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class WorldUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isDeadBodyNearby() {
        if (mc.world == null || mc.player == null) return false;
        return mc.world.getPlayers().parallelStream()
                .filter(e -> e != mc.player)
                .filter(e -> e.squaredDistanceTo(mc.player) <= 36.0D)
                .anyMatch(LivingEntity::isDead);
    }

    public static boolean isValuableLootNearby() {
        if (mc.player == null || mc.world == null) return false;
        Box area = new Box(mc.player.getX() - 10.0, mc.player.getY() - 5.0, mc.player.getZ() - 10.0,
                mc.player.getX() + 10.0, mc.player.getY() + 5.0, mc.player.getZ() + 10.0);

        int valuableArmorCount = 0;
        List<Entity> entities = mc.world.getOtherEntities(null, area);

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();
                if (stack.isEmpty()) continue;

                if (stack.getItem() instanceof ArmorItem armor) {


                    String matName = armor.getMaterial().value().toString();
                    if (stack.getItem() == Items.NETHERITE_HELMET || stack.getItem() == Items.NETHERITE_CHESTPLATE ||
                            stack.getItem() == Items.NETHERITE_LEGGINGS || stack.getItem() == Items.NETHERITE_BOOTS ||
                            stack.getItem() == Items.DIAMOND_HELMET || stack.getItem() == Items.DIAMOND_CHESTPLATE ||
                            stack.getItem() == Items.DIAMOND_LEGGINGS || stack.getItem() == Items.DIAMOND_BOOTS) {
                        valuableArmorCount++;
                    }
                } else if (stack.getCount() > 32) {
                    if (stack.getItem() == Items.END_CRYSTAL || stack.getItem() == Items.OBSIDIAN ||
                            stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE || stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                        return true;
                    }
                }
            }
        }
        return valuableArmorCount >= 2;
    }

    public static void placeBlock(BlockHitResult blockHit, boolean swingHand) {
        if (mc.interactionManager == null || mc.player == null) return;
        if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit).isAccepted() && swingHand) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static void hitEntity(Entity entity, boolean swingHand) {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.attackEntity(mc.player, entity);
        if (swingHand) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static double distance(Vec3d from, Vec3d to) {
        return from.distanceTo(to);
    }

    public static boolean isTool(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ToolItem toolItem)) {
            return false;
        }
        ToolMaterial material = toolItem.getMaterial();
        return material == ToolMaterials.DIAMOND || material == ToolMaterials.NETHERITE;
    }

    public static PlayerEntity findNearestPlayer(PlayerEntity sender, float radius, boolean seeOnly, boolean ignoreSelf) {
        if (mc.world == null) return null;
        PlayerEntity target = null;
        double minDistance = radius;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (ignoreSelf && player == sender) continue;
            if (player.isDead()) continue;
            if (seeOnly && !sender.canSee(player)) continue;

            double distance = sender.distanceTo(player);
            if (distance < minDistance) {
                minDistance = distance;
                target = player;
            }
        }
        return target;
    }

    public static net.minecraft.util.hit.HitResult getHitResult(double distance) {
        if (mc.player == null) return null;
        return mc.player.raycast(distance, 0, false);
    }
}

