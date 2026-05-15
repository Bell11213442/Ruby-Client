package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.Freecam;
import com.radium.client.modules.visual.NoRender;
import com.radium.client.modules.visual.SeeThroughWalls;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @ModifyVariable(method = "setupTerrain", at = @At("HEAD"), ordinal = 1)
    private boolean onSetupTerrain(boolean value) {
        if (RadiumClient.getModuleManager().getModule(Freecam.class).isEnabled()) {
            return true;
        }
        return value;
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(CallbackInfo ci) {
        NoRender noRender = RadiumClient.getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled()
                && !noRender.shouldRenderRain() && !noRender.shouldRenderSnow()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "setupTerrain", at = @At("HEAD"), ordinal = 0)
    private boolean onSetupTerrainOcclusion(boolean value) {
        SeeThroughWalls seeThroughWalls = SeeThroughWalls.get();
        if (seeThroughWalls != null && seeThroughWalls.isEnabled() && !seeThroughWalls.shouldOccludeChunks()) {
            return false;
        }
        return value;
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void onRenderEntity(Entity entity,
                                double cameraX,
                                double cameraY,
                                double cameraZ,
                                float tickDelta,
                                MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers,
                                CallbackInfo ci) {
        NoRender noRender = RadiumClient.getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled() && !noRender.shouldRenderEntity(entity)) {
            ci.cancel();
        }
    }

}

