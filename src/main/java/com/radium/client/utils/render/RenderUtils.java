package com.radium.client.utils.render;
// radium client

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.radium.client.utils.MathUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import static com.mojang.blaze3d.systems.RenderSystem.*;
import static com.radium.client.client.RadiumClient.mc;


public final class RenderUtils {

    public static VertexSorter vertexSorter;

    public static void unscaledProjection() {
        vertexSorter = RenderSystem.getVertexSorting();
        if (vertexSorter != null)
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), 0, 1000, 21000), VertexSorter.BY_Z);
    }

    public static void scaledProjection() {
        if (vertexSorter != null)
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, (float) (mc.getWindow().getFramebufferWidth() / mc.getWindow().getScaleFactor()), (float) (mc.getWindow().getFramebufferHeight() / mc.getWindow().getScaleFactor()), 0, 1000, 21000), vertexSorter);
    }

    public static void customScaledProjection(double scale) {
        if (vertexSorter != null)
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, (float) (mc.getWindow().getFramebufferWidth() / scale), (float) (mc.getWindow().getFramebufferHeight() / scale), 0, 1000, 21000), vertexSorter);
    }

    public static void fillRect(DrawContext context, int x, int y, int w, int h, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, (float) x, (float) y, 0).color(color);
        buf.vertex(mat, (float) (x + w), (float) y, 0).color(color);
        buf.vertex(mat, (float) (x + w), (float) (y + h), 0).color(color);
        buf.vertex(mat, (float) x, (float) (y + h), 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillRadialGradient(DrawContext context, int cX, int cY, int radius, int innerColor, int outerColor) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, (float) cX, (float) cY, 0).color(innerColor);

        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius) + cX;
            float y = (float) (Math.sin(angle) * radius) + cY;
            buf.vertex(mat, x, y, 0).color(outerColor);
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillSidewaysGradient(DrawContext context, int x, int y, int w, int h, int colorLeft, int colorRight) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x, y, 0).color(colorLeft);
        buf.vertex(mat, x, y + h, 0).color(colorLeft);
        buf.vertex(mat, x + w, y + h, 0).color(colorRight);
        buf.vertex(mat, x + w, y, 0).color(colorRight);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillSidewaysGradient(DrawContext context, int x, int y, int w, int h, int... colors) {
        int amount = colors.length;
        if (amount == 0)
            return;
        else if (amount == 1) {
            fillRect(context, x, y, w, h, colors[0]);
            return;
        } else if (amount == 2) {
            fillSidewaysGradient(context, x, y, w, h, colors[0], colors[1]);
            return;
        }

        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int sectionLen = w / (amount - 1);

        for (int i = 0; i < amount; i++) {
            int color = colors[i];
            int tx = x + i * sectionLen;

            buf.vertex(mat, tx, y, 0).color(color);
            buf.vertex(mat, tx, y + h, 0).color(color);
        }
        buf.vertex(mat, x + w, y, 0).color(colors[amount - 1]);
        buf.vertex(mat, x + w, y + h, 0).color(colors[amount - 1]);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillVerticalGradient(DrawContext context, int x, int y, int w, int h, int colorTop, int colorBottom) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x, y, 0).color(colorTop);
        buf.vertex(mat, x + w, y, 0).color(colorTop);
        buf.vertex(mat, x + w, y + h, 0).color(colorBottom);
        buf.vertex(mat, x, y + h, 0).color(colorBottom);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillVerticalGradient(DrawContext context, int x, int y, int w, int h, int... colors) {
        int amount = colors.length;
        if (amount == 0)
            return;
        else if (amount == 1) {
            fillRect(context, x, y, w, h, colors[0]);
            return;
        } else if (amount == 2) {
            fillVerticalGradient(context, x, y, w, h, colors[0], colors[1]);
            return;
        }

        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int sectionLen = h / (amount - 1);

        for (int i = 0; i < amount; i++) {
            int color = colors[i];
            int ty = y + i * sectionLen;

            buf.vertex(mat, x, ty, 0).color(color);
            buf.vertex(mat, x + w, ty, 0).color(color);
        }
        buf.vertex(mat, x, y + h, 0).color(colors[amount - 1]);
        buf.vertex(mat, x + w, y + h, 0).color(colors[amount - 1]);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillAnnulusArcGradient(DrawContext context, int cx, int cy, int radius, int start, int end, int thickness, int innerColor, int outerColor) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        for (int i = start - 90; i <= end - 90; i++) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float x1 = cx + cos * radius;
            float y1 = cy + sin * radius;
            float x2 = cx + cos * (radius + thickness);
            float y2 = cy + sin * (radius + thickness);
            buf.vertex(mat, x1, y1, 0).color(innerColor);
            buf.vertex(mat, x2, y2, 0).color(outerColor);
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillArc(DrawContext context, int cX, int cY, int radius, int start, int end, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, (float) cX, (float) cY, 0).color(color);

        for (int i = start - 90; i <= end - 90; i++) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius) + cX;
            float y = (float) (Math.sin(angle) * radius) + cY;
            buf.vertex(mat, x, y, 0).color(color);
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillCircle(DrawContext context, int cX, int cY, int radius, int color) {
        fillArc(context, cX, cY, radius, 0, 360, color);
    }

    public static void fillAnnulusArc(DrawContext context, int cx, int cy, int radius, int start, int end, int thickness, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        for (int i = start - 90; i <= end - 90; i++) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float x1 = cx + cos * radius;
            float y1 = cy + sin * radius;
            float x2 = cx + cos * (radius + thickness);
            float y2 = cy + sin * (radius + thickness);
            buf.vertex(mat, x1, y1, 0).color(color);
            buf.vertex(mat, x2, y2, 0).color(color);
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillAnnulus(DrawContext context, int cx, int cy, int radius, int thickness, int color) {
        fillAnnulusArc(context, cx, cy, radius, 0, 360, thickness, color);
    }

    public static void fillRoundRect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        r = MathUtils.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color);

        int[][] corners = {
                {x + w - r, y + r},
                {x + w - r, y + h - r},
                {x + r, y + h - r},
                {x + r, y + r}
        };

        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color);
            }
        }

        buf.vertex(mat, corners[0][0], y, 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();

    }

    public static void fillRoundRect(DrawContext context, int x, int y, int w, int h,
                                     int topLeft, int topRight, int bottomRight, int bottomLeft,
                                     int color) {
        topLeft = MathUtils.clamp(topLeft, 0, Math.min(w, h) / 2);
        topRight = MathUtils.clamp(topRight, 0, Math.min(w, h) / 2);
        bottomRight = MathUtils.clamp(bottomRight, 0, Math.min(w, h) / 2);
        bottomLeft = MathUtils.clamp(bottomLeft, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();


        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color);


        buf.vertex(mat, x + topLeft, y, 0).color(color);
        buf.vertex(mat, x + w - topRight, y, 0).color(color);


        for (int i = -90; i <= 0; i += 2) {
            float angle = (float) Math.toRadians(i);
            float rx = x + w - topRight + (float) Math.cos(angle) * topRight;
            float ry = y + topRight + (float) Math.sin(angle) * topRight;
            buf.vertex(mat, rx, ry, 0).color(color);
        }


        buf.vertex(mat, x + w, y + topRight, 0).color(color);
        buf.vertex(mat, x + w, y + h - bottomRight, 0).color(color);


        for (int i = 0; i <= 90; i += 2) {
            float angle = (float) Math.toRadians(i);
            float rx = x + w - bottomRight + (float) Math.cos(angle) * bottomRight;
            float ry = y + h - bottomRight + (float) Math.sin(angle) * bottomRight;
            buf.vertex(mat, rx, ry, 0).color(color);
        }


        buf.vertex(mat, x + w - bottomRight, y + h, 0).color(color);
        buf.vertex(mat, x + bottomLeft, y + h, 0).color(color);


        for (int i = 90; i <= 180; i += 10) {
            float angle = (float) Math.toRadians(i);
            float rx = x + bottomLeft + (float) Math.cos(angle) * bottomLeft;
            float ry = y + h - bottomLeft + (float) Math.sin(angle) * bottomLeft;
            buf.vertex(mat, rx, ry, 0).color(color);
        }


        buf.vertex(mat, x, y + h - bottomLeft, 0).color(color);
        buf.vertex(mat, x, y + topLeft, 0).color(color);


        for (int i = 180; i <= 270; i += 10) {
            float angle = (float) Math.toRadians(i);
            float rx = x + topLeft + (float) Math.cos(angle) * topLeft;
            float ry = y + topLeft + (float) Math.sin(angle) * topLeft;
            buf.vertex(mat, rx, ry, 0).color(color);
        }


        buf.vertex(mat, x + topLeft, y, 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillRoundTabTop(DrawContext context, int x, int y, int w, int h, int r, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color);

        int[][] corners = {
                {x + r, y + r},
                {x + w - r, y + r}
        };

        for (int corner = 0; corner < 2; corner++) {
            int cornerStart = (corner - 2) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color);
            }
        }

        buf.vertex(mat, x + w, y + h, 0).color(color);
        buf.vertex(mat, x, y + h, 0).color(color);
        buf.vertex(mat, x, corners[0][1], 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillRoundTabBottom(DrawContext context, int x, int y, int w, int h, int r, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color);

        int[][] corners = {
                {x + w - r, y + h - r},
                {x + r, y + h - r}
        };

        for (int corner = 0; corner < 2; corner++) {
            int cornerStart = corner * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color);
            }
        }

        buf.vertex(mat, x, y, 0).color(color);
        buf.vertex(mat, x + w, y, 0).color(color);
        buf.vertex(mat, x + w, corners[0][1], 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void fillRoundHoriLine(DrawContext context, int x, int y, int length, int thickness, int color) {
        fillRoundRect(context, x, y, length, thickness, thickness / 2, color);
    }

    public static void fillRoundVertLine(DrawContext context, int x, int y, int length, int thickness, int color) {
        fillRoundRect(context, x, y, thickness, length, thickness / 2, color);
    }


    public static void drawRect(DrawContext context, int x, int y, int w, int h, int color) {
        drawHorLine(context, x, y, w, color);
        drawVerLine(context, x, y + 1, h - 2, color);
        drawVerLine(context, x + w - 1, y + 1, h - 2, color);
        drawHorLine(context, x, y + h - 1, w, color);
    }

    public static void drawBox(DrawContext context, int x, int y, int w, int h, int color) {
        drawLine(context, x, y, x + w, y, color);
        drawLine(context, x, y + h, x + w, y + h, color);
        drawLine(context, x, y, x, y + h, color);
        drawLine(context, x + w, y, x + w, y + h, color);
    }

    public static void drawHorLine(DrawContext context, int x, int y, int length, int color) {
        fillRect(context, x, y, length, 1, color);
    }

    public static void drawVerLine(DrawContext context, int x, int y, int length, int color) {
        fillRect(context, x, y, 1, length, color);
    }

    public static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, (float) x1, (float) y1, 0).color(color);
        buf.vertex(mat, (float) x2, (float) y2, 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void drawArc(DrawContext context, int cX, int cY, int radius, int start, int end, int color) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        for (int i = start - 90; i <= end - 90; i++) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius) + cX;
            float y = (float) (Math.sin(angle) * radius) + cY;
            buf.vertex(mat, x, y, 0).color(color);
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void drawCircle(DrawContext context, int cX, int cY, int radius, int color) {
        drawArc(context, cX, cY, radius, 0, 360, color);
    }

    public static void drawRoundRect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        r = MathUtils.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        int[][] corners = {
                {x + w - r, y + r},
                {x + w - r, y + h - r},
                {x + r, y + h - r},
                {x + r, y + r}
        };

        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color);
            }
        }

        buf.vertex(mat, corners[0][0], y, 0).color(color);

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    public static void drawRoundHoriLine(DrawContext context, int x, int y, int length, int thickness, int color) {
        drawRoundRect(context, x, y, length, thickness, thickness / 2, color);
    }

    public static void drawRoundVertLine(DrawContext context, int x, int y, int length, int thickness, int color) {
        drawRoundRect(context, x, y, thickness, length, thickness / 2, color);
    }

    public static void drawTextureQuad(DrawContext context, Identifier texture, int x, int y, int w, int h) {
        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x, y, 0).texture(0f, 0f);
        buf.vertex(mat, x + w, y, 0).texture(1f, 0f);
        buf.vertex(mat, x + w, y + h, 0).texture(1f, 1f);
        buf.vertex(mat, x, y + h, 0).texture(0f, 1f);

        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderColor(1, 1, 1, 1);
        setShaderTexture(0, texture);

        drawBuffer(buf);

        enableCull();
        disableBlend();
    }

    public static void drawRoundTexture(DrawContext context, Identifier texture, int x, int y, int w, int h, int r) {
        r = MathUtils.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).texture(0.5F, 0.5F);

        int[][] corners = {
                {x + w - r, y + r},
                {x + w - r, y + h - r},
                {x + r, y + h - r},
                {x + r, y + r}
        };

        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                float u = (rx - x) / w;
                float v = (ry - y) / h;
                buf.vertex(mat, rx, ry, 0).texture(u, v);
            }
        }

        buf.vertex(mat, corners[0][0], y, 0).texture(((float) corners[0][0] - x) / w, 0);

        disableCull();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderTexture(0, texture);
        setShaderColor(1, 1, 1, 1);

        drawBuffer(buf);

        enableCull();
    }

    public static void drawCircleTexture(DrawContext context, Identifier texture, int x, int y, int size) {
        int radius = size / 2;

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x, y, 0).texture(0.5F, 0.5F);
        for (int i = 0; i <= 360; i += 10) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle) * radius + x;
            float sin = (float) Math.sin(angle) * radius + y;
            float tu = (cos - x + radius) / size;
            float tv = (sin - y + radius) / size;
            buf.vertex(mat, cos, sin, 0).texture(tu, tv);
        }

        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderColor(1, 1, 1, 1);
        setShaderTexture(0, texture);

        drawBuffer(buf);

        enableCull();
        disableBlend();
    }

    public static void drawCirclePlayerHead(DrawContext context, SkinTextures texture, int x, int y, int size) {
        int radius = size / 2;
        float u = 1 / 8F;
        float v = 1 / 8F;

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x, y, 0).texture(0.5F * u + u, 0.5F * v + v);
        for (int i = 0; i <= 360; i += 10) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle) * radius + x;
            float sin = (float) Math.sin(angle) * radius + y;
            float tu = (cos - x + radius) / size;
            float tv = (sin - y + radius) / size;
            buf.vertex(mat, cos, sin, 0).texture(tu * u + u, tv * v + v);
        }

        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderColor(1, 1, 1, 1);
        setShaderTexture(0, texture.texture());

        drawBuffer(buf);

        enableCull();
        disableBlend();

        buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE);
        buf.vertex(mat, x, y, 0.0069420F).texture(0.5F * u + u * 5, 0.5F * v + v);
        for (int i = 0; i <= 360; i += 10) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle) * radius + x;
            float sin = (float) Math.sin(angle) * radius + y;
            float tu = (cos - x + radius) / size;
            float tv = (sin - y + radius) / size;
            buf.vertex(mat, cos, sin, 0.0069420F).texture(tu * u + u * 5, tv * v + v);
        }

        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderColor(1, 1, 1, 1);
        setShaderTexture(0, texture.texture());

        drawBuffer(buf);

        enableCull();
        disableBlend();
    }

    public static void drawRoundedPlayerHead(DrawContext context, SkinTextures texture, int x, int y, int size, int r) {
        r = MathUtils.clamp(r, 0, size / 2);

        float u = 1 / 8F;
        float v = 1 / 8F;
        int[][] corners = {
                {x + size - r, y + r},
                {x + size - r, y + size - r},
                {x + r, y + size - r},
                {x + r, y + r}
        };

        BufferBuilder buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE);
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        buf.vertex(mat, x + size / 2F, y + size / 2F, 0).texture(0.5F * u + u, 0.5F * v + v);
        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                float tu = (rx - x) / size * u + u;
                float tv = (ry - y) / size * v + v;
                buf.vertex(mat, rx, ry, 0).texture(tu, tv);
            }
        }
        buf.vertex(mat, corners[0][0], y, 0).texture(((float) corners[0][0] - x) / size * u + u, 0 * v + v);

        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderColor(1, 1, 1, 1);
        setShaderTexture(0, texture.texture());

        drawBuffer(buf);

        enableCull();
        disableBlend();

        buf = getBuffer(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE);
        buf.vertex(mat, x + size / 2F, y + size / 2F, 0).texture(0.5F * u + u * 5, 0.5F * v + v);
        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                float tu = (rx - x) / size * u + u * 5;
                float tv = (ry - y) / size * v + v;
                buf.vertex(mat, rx, ry, 0).texture(tu, tv);
            }
        }
        buf.vertex(mat, corners[0][0], y, 0).texture(((float) corners[0][0] - x) / size * u + u * 5, 0 * v + v);

        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionTexProgram);
        setShaderColor(1, 1, 1, 1);
        setShaderTexture(0, texture.texture());

        drawBuffer(buf);

        enableCull();
        disableBlend();
    }


    public static void beginRendering() {
        disableCull();
        enableBlend();
        defaultBlendFunc();
        setShader(GameRenderer::getPositionColorProgram);
        setShaderColor(1, 1, 1, 1);
    }

    public static void finishRendering() {
        enableCull();
        disableBlend();
        setShader(GameRenderer::getPositionTexProgram);
    }

    public static void check(boolean check, String msg) {
        if (!check) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void drawBuffer(BufferBuilder buf) {
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public static BufferBuilder getBuffer(VertexFormat.DrawMode drawMode, VertexFormat format) {
        return Tessellator.getInstance().begin(drawMode, format);
    }

}

