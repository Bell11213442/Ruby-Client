package com.radium.client.modules.donut;
// radium client

import com.radium.client.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;

import java.util.HashSet;
import java.util.Set;

public class AntiTrap extends Module {

    private final Set<Entity> hiddenEntities = new HashSet<>();

    public AntiTrap() {
        super("AntiTrap", "Helps you escape traps by removing certain entities.", Category.DONUT);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            return;
        }


        for (Entity entity : mc.world.getEntities()) {
            if (entity == null || entity.isRemoved()) continue;

            if (isTrapEntity(entity)) {
                if (!hiddenEntities.contains(entity)) {

                    entity.setInvisible(true);
                    if (entity instanceof ArmorStandEntity) {
                        entity.setInvisible(true);
                    }
                    hiddenEntities.add(entity);
                }
            } else {

                if (hiddenEntities.contains(entity)) {
                    restoreEntity(entity);
                    hiddenEntities.remove(entity);
                }
            }
        }


        hiddenEntities.removeIf(entity -> entity == null || entity.isRemoved());
    }

    @Override
    public void onDisable() {

        for (Entity entity : new HashSet<>(hiddenEntities)) {
            if (entity != null && !entity.isRemoved() && entity.getWorld() != null) {
                restoreEntity(entity);
            }
        }
        hiddenEntities.clear();
    }

    private void restoreEntity(Entity entity) {
        if (entity == null || entity.isRemoved()) return;


        entity.setInvisible(false);
        if (entity instanceof ArmorStandEntity) {
            entity.setInvisible(false);
        }
    }

    private boolean isTrapEntity(Entity entity) {
        if (entity == null) return false;


        if (entity instanceof ArmorStandEntity) return true;
        if (entity instanceof ItemFrameEntity) return true;


        EntityType<?> entityType = entity.getType();
        return entityType != null && (
                entityType.equals(EntityType.ARMOR_STAND) ||
                        entityType.equals(EntityType.ITEM_FRAME) ||
                        entityType.equals(EntityType.GLOW_ITEM_FRAME) ||
                        entityType.equals(EntityType.CHEST_MINECART)
        );
    }
}
