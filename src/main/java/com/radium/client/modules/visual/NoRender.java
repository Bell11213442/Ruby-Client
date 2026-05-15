package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.util.math.BlockPos;

public final class NoRender extends Module {


    private final BooleanSetting rain = new BooleanSetting("Rain", true);
    private final BooleanSetting snow = new BooleanSetting("Snow", true);
    private final BooleanSetting thunder = new BooleanSetting("Thunder", true);
    private final BooleanSetting fog = new BooleanSetting("Fog", false);


    private final BooleanSetting fire = new BooleanSetting("Fire", false);
    private final BooleanSetting water = new BooleanSetting("Water", false);
    private final BooleanSetting lava = new BooleanSetting("Lava", false);


    private final BooleanSetting armorStands = new BooleanSetting("Armor Stands", false);
    private final BooleanSetting itemFrames = new BooleanSetting("Item Frames", false);
    private final BooleanSetting paintings = new BooleanSetting("Paintings", false);
    private final BooleanSetting enderman = new BooleanSetting("Enderman", false);
    private final BooleanSetting guardian = new BooleanSetting("Guardian", false);
    private final BooleanSetting slime = new BooleanSetting("Slime", false);
    private final BooleanSetting droppedItems = new BooleanSetting("Dropped Items", false);


    private final BooleanSetting vignette = new BooleanSetting("Vignette", false);
    private final BooleanSetting hurtCam = new BooleanSetting("Hurt Camera", false);
    private final BooleanSetting totemAnimation = new BooleanSetting("Totem Animation", false);
    private final BooleanSetting noSwing = new BooleanSetting("No Swing", false);


    private final NumberSetting entityDistance = new NumberSetting("Entity Distance", 64.0, 0.0, 1000.0, 1.0);
    private final NumberSetting blockDistance = new NumberSetting("Block Distance", 128.0, 0.0, 1000.0, 1.0);

    public NoRender() {
        super("NoRender", "Disables various rendering elements to improve performance", Category.VISUAL);
        addSettings(
                this.rain, this.snow, this.thunder, this.fog,
                this.fire, this.water, this.lava,
                this.armorStands, this.itemFrames, this.paintings, this.enderman, this.guardian, this.slime, this.droppedItems,
                this.vignette, this.hurtCam, this.totemAnimation, this.noSwing,
                this.entityDistance, this.blockDistance
        );
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;

        if (this.rain.getValue() || this.snow.getValue()) {
            mc.world.setRainGradient(0.0f);
        }

        if (this.thunder.getValue()) {
            mc.world.setThunderGradient(0.0f);
        }
    }

    @Override
    public void onDisable() {
        if (mc.world != null) {
            mc.world.setRainGradient(1.0f);
            mc.world.setThunderGradient(1.0f);
        }
    }


    public boolean shouldRenderRain() {
        return !this.rain.getValue();
    }

    public boolean shouldRenderSnow() {
        return !this.snow.getValue();
    }

    public boolean shouldRenderThunder() {
        return !this.thunder.getValue();
    }

    public boolean shouldRenderFog() {
        return !this.fog.getValue();
    }

    public boolean shouldRenderHurtCam() {
        return !this.hurtCam.getValue();
    }

    public boolean shouldRenderTotemAnimation() {
        return !this.totemAnimation.getValue();
    }

    public boolean shouldRenderSwing() {
        return !this.noSwing.getValue();
    }

    public boolean shouldRenderEntity(Entity entity) {
        if (entity == null || mc.player == null) return true;
        if (entity == mc.player) return true;

        if (mc.player.distanceTo(entity) > this.entityDistance.getValue()) {
            return false;
        }

        if (this.droppedItems.getValue() && entity instanceof ItemEntity) {
            return false;
        }
        if (this.armorStands.getValue() && entity instanceof ArmorStandEntity) {
            return false;
        }
        if (this.itemFrames.getValue() && entity instanceof ItemFrameEntity) {
            return false;
        }
        if (this.paintings.getValue() && entity instanceof PaintingEntity) {
            return false;
        }
        if (this.enderman.getValue() && entity instanceof EndermanEntity) {
            return false;
        }
        if (this.guardian.getValue() && entity instanceof GuardianEntity) {
            return false;
        }
        return !this.slime.getValue() || !(entity instanceof SlimeEntity);
    }

    public boolean shouldRenderBlock(BlockState state, BlockPos pos) {
        if (state == null || mc.player == null) return true;

        if (mc.player.getBlockPos().getSquaredDistance(pos) > this.blockDistance.getValue() * this.blockDistance.getValue()) {
            return false;
        }

        if (this.water.getValue() && state.getBlock() == Blocks.WATER) {
            return false;
        }
        if (this.lava.getValue() && state.getBlock() == Blocks.LAVA) {
            return false;
        }
        return !this.fire.getValue() || state.getBlock() != Blocks.FIRE;
    }
}
