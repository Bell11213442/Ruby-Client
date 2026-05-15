package com.radium.client.modules.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class ProjectilePredictorRenderer {
    private static final ProjectilePredictorRenderer INSTANCE = new ProjectilePredictorRenderer();

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> INSTANCE.render(context));
    }

    private void render(WorldRenderContext context) {
    }
}
