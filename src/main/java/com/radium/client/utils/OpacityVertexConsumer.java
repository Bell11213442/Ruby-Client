package com.radium.client.utils;
// radium client

import net.minecraft.client.render.VertexConsumer;

public class OpacityVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float opacity;

    public OpacityVertexConsumer(VertexConsumer delegate, float opacity) {
        this.delegate = delegate;
        this.opacity = opacity;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, (int) (alpha * opacity));
        return this;
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha) {
        delegate.color(red, green, blue, alpha * opacity);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }


}


