package com.radium.client.gui;
// radium client

import com.mojang.blaze3d.systems.RenderSystem;
import com.radium.client.client.RadiumClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public final class RenderUtils {
    private RenderUtils() {
    }

    public static Camera getCamera() {
        return RadiumClient.mc.getBlockEntityRenderDispatcher().camera;
    }

    public static Vec3d getCameraPos() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
    }

    public static void renderLine(MatrixStack matrixStack, Color color, Vec3d start, Vec3d end) {
        Matrix4f m = matrixStack.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buf.vertex(m, (float) start.x, (float) start.y, (float) start.z).color(r, g, b, a);
        buf.vertex(m, (float) end.x, (float) end.y, (float) end.z).color(r, g, b, a);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }


    public static void renderRoundedQuad(MatrixStack matrices, Color color, int x1, int y1, int x2, int y2, int z, int radius) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();


        matrices.push();
        matrices.translate(0, 0, z);
        matrices.peek().getPositionMatrix().mul(matrices.peek().getPositionMatrix());

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;

        bufferBuilder.vertex(matrix, (float) x1, (float) y1, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) x1, (float) y2, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) x2, (float) y2, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, (float) x2, (float) y1, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.disableBlend();
        matrices.pop();
    }

    public static void renderFilledBox(MatrixStack matrixStack, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        Matrix4f m = matrixStack.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);


        v(buf, m, x1, y1, z1, r, g, b, a);
        v(buf, m, x2, y1, z1, r, g, b, a);
        v(buf, m, x2, y1, z2, r, g, b, a);
        v(buf, m, x1, y1, z2, r, g, b, a);


        v(buf, m, x1, y2, z1, r, g, b, a);
        v(buf, m, x1, y2, z2, r, g, b, a);
        v(buf, m, x2, y2, z2, r, g, b, a);
        v(buf, m, x2, y2, z1, r, g, b, a);


        v(buf, m, x1, y1, z1, r, g, b, a);
        v(buf, m, x1, y2, z1, r, g, b, a);
        v(buf, m, x2, y2, z1, r, g, b, a);
        v(buf, m, x2, y1, z1, r, g, b, a);


        v(buf, m, x1, y1, z2, r, g, b, a);
        v(buf, m, x2, y1, z2, r, g, b, a);
        v(buf, m, x2, y2, z2, r, g, b, a);
        v(buf, m, x1, y2, z2, r, g, b, a);


        v(buf, m, x1, y1, z1, r, g, b, a);
        v(buf, m, x1, y1, z2, r, g, b, a);
        v(buf, m, x1, y2, z2, r, g, b, a);
        v(buf, m, x1, y2, z1, r, g, b, a);


        v(buf, m, x2, y1, z1, r, g, b, a);
        v(buf, m, x2, y2, z1, r, g, b, a);
        v(buf, m, x2, y2, z2, r, g, b, a);
        v(buf, m, x2, y1, z2, r, g, b, a);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawBox(MatrixStack matrices, Box box, int argb, boolean outline) {
        float a = (argb >>> 24 & 0xFF) / 255f;
        float r = (argb >>> 16 & 0xFF) / 255f;
        float g = (argb >>> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (!outline) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            v(buf, matrix, box.minX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.minX, box.minY, box.maxZ, r, g, b, a);

            v(buf, matrix, box.minX, box.maxY, box.minZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.minZ, r, g, b, a);

            v(buf, matrix, box.minX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.minZ, r, g, b, a);

            v(buf, matrix, box.minX, box.minY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.maxZ, r, g, b, a);

            v(buf, matrix, box.minX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.minX, box.minY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.minZ, r, g, b, a);

            v(buf, matrix, box.maxX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.maxZ, r, g, b, a);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        } else {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            RenderSystem.lineWidth(2.0f);

            lineLoop(buf, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);

            lineLoop(buf, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);

            v(buf, matrix, box.minX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.minZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.minZ, r, g, b, a);
            v(buf, matrix, box.minX, box.minY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.minX, box.maxY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.minY, box.maxZ, r, g, b, a);
            v(buf, matrix, box.maxX, box.maxY, box.maxZ, r, g, b, a);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawBox(MatrixStack matrices,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2,
                               int argb, boolean outline) {
        float a = (argb >>> 24 & 0xFF) / 255f;
        float r = (argb >>> 16 & 0xFF) / 255f;
        float g = (argb >>> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (!outline) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);


            v(buf, matrix, x1, y1, z1, r, g, b, a);
            v(buf, matrix, x2, y1, z1, r, g, b, a);
            v(buf, matrix, x2, y1, z2, r, g, b, a);
            v(buf, matrix, x1, y1, z2, r, g, b, a);


            v(buf, matrix, x1, y2, z1, r, g, b, a);
            v(buf, matrix, x1, y2, z2, r, g, b, a);
            v(buf, matrix, x2, y2, z2, r, g, b, a);
            v(buf, matrix, x2, y2, z1, r, g, b, a);


            v(buf, matrix, x1, y1, z1, r, g, b, a);
            v(buf, matrix, x1, y2, z1, r, g, b, a);
            v(buf, matrix, x2, y2, z1, r, g, b, a);
            v(buf, matrix, x2, y1, z1, r, g, b, a);


            v(buf, matrix, x1, y1, z2, r, g, b, a);
            v(buf, matrix, x2, y1, z2, r, g, b, a);
            v(buf, matrix, x2, y2, z2, r, g, b, a);
            v(buf, matrix, x1, y2, z2, r, g, b, a);


            v(buf, matrix, x1, y1, z1, r, g, b, a);
            v(buf, matrix, x1, y1, z2, r, g, b, a);
            v(buf, matrix, x1, y2, z2, r, g, b, a);
            v(buf, matrix, x1, y2, z1, r, g, b, a);


            v(buf, matrix, x2, y1, z1, r, g, b, a);
            v(buf, matrix, x2, y2, z1, r, g, b, a);
            v(buf, matrix, x2, y2, z2, r, g, b, a);
            v(buf, matrix, x2, y1, z2, r, g, b, a);

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferRenderer.drawWithGlobalProgram(buf.end());

        } else {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            RenderSystem.lineWidth(2.0f);


            lineLoop(buf, matrix, x1, y1, z1, x2, y1, z2, r, g, b, a);

            lineLoop(buf, matrix, x1, y2, z1, x2, y2, z2, r, g, b, a);

            v(buf, matrix, x1, y1, z1, r, g, b, a);
            v(buf, matrix, x1, y2, z1, r, g, b, a);
            v(buf, matrix, x2, y1, z1, r, g, b, a);
            v(buf, matrix, x2, y2, z1, r, g, b, a);
            v(buf, matrix, x1, y1, z2, r, g, b, a);
            v(buf, matrix, x1, y2, z2, r, g, b, a);
            v(buf, matrix, x2, y1, z2, r, g, b, a);
            v(buf, matrix, x2, y2, z2, r, g, b, a);

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }


    private static void lineLoop(BufferBuilder buf, Matrix4f m, double x1, double y, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {

        v(buf, m, x1, y, z1, r, g, b, a);
        v(buf, m, x2, y, z1, r, g, b, a);
        v(buf, m, x2, y, z1, r, g, b, a);
        v(buf, m, x2, y, z2, r, g, b, a);
        v(buf, m, x2, y, z2, r, g, b, a);
        v(buf, m, x1, y, z2, r, g, b, a);
        v(buf, m, x1, y, z2, r, g, b, a);
        v(buf, m, x1, y, z1, r, g, b, a);
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, float r, float g, float bl, float a) {
        b.vertex(m, (float) x, (float) y, (float) z).color(r, g, bl, a);
    }

    public static int withAlpha(int rgb, int alpha255) {
        return (alpha255 & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    public static int rainbow(int offsetMs, float alpha) {
        float hue = (System.currentTimeMillis() + offsetMs) % 3000 / 3000f;
        int rgb = Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF;
        return withAlpha(rgb, (int) (alpha * 255));
    }
}
