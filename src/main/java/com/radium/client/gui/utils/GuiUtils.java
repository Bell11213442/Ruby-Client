package com.radium.client.gui.utils;
// radium client

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

public class GuiUtils {
    public static boolean isHovered(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height,
                                       int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        float a = (float) (color >> 24 & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);


        bufferBuilder.vertex(matrix, x + radius, y + radius, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x + radius, y + height - radius, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x + width - radius, y + height - radius, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x + width - radius, y + radius, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());


        context.fill(x + radius, y, x + width - radius, y + radius, color);
        context.fill(x + radius, y + height - radius, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);


        drawCorner(matrix, x, y, radius, r, g, b, a, 180, 270);
        drawCorner(matrix, x + width - radius, y, radius, r, g, b, a, 270, 360);
        drawCorner(matrix, x + width - radius, y + height - radius, radius, r, g, b, a, 0, 90);
        drawCorner(matrix, x, y + height - radius, radius, r, g, b, a, 90, 180);

        RenderSystem.disableBlend();
    }

    private static void drawCorner(Matrix4f matrix, int x, int y, int radius, float r, float g, float b, float a, int startAngle, int endAngle) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x + radius, y + radius, 0).color(r, g, b, a);

        for (int i = startAngle; i <= endAngle; i += 3) {
            double angle = Math.toRadians(i);
            bufferBuilder.vertex(matrix, (float) (x + radius + Math.cos(angle) * radius), (float) (y + radius + Math.sin(angle) * radius), 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}
