package com.radium.client.gui.utils;
// radium client

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;

public class BlurUtil {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static ShaderProgram blurShader = null;


    public static void applyBlur(int intensity) {
        if (client.world == null || client.player == null) return;

        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null) return;


        if (blurShader == null) {
            try {

                blurShader = new ShaderProgram(
                        client.getResourceManager(),
                        "gui_shader",
                        VertexFormats.POSITION_TEXTURE
                );
            } catch (Exception e) {

                return;
            }
        }


        int actualIntensity = Math.min(intensity, 3);


        Framebuffer mainFramebuffer = client.getFramebuffer();

        try {
            RenderSystem.enableBlend();
            RenderSystem.setShader(() -> blurShader);


            if (blurShader.getUniform("Radius") != null) {
                blurShader.getUniform("Radius").set((float) actualIntensity);
            }
            if (blurShader.getUniform("TexelSize") != null) {
                blurShader.getUniform("TexelSize").set(
                        1.0f / (float) client.getWindow().getFramebufferWidth(),
                        1.0f / (float) client.getWindow().getFramebufferHeight()
                );
            }


            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

            float width = (float) client.getWindow().getFramebufferWidth();
            float height = (float) client.getWindow().getFramebufferHeight();

            bufferBuilder.vertex(0, height, 0).texture(0, 0);
            bufferBuilder.vertex(width, height, 0).texture(1, 0);
            bufferBuilder.vertex(width, 0, 0).texture(1, 1);
            bufferBuilder.vertex(0, 0, 0).texture(0, 1);


            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableBlend();
        } catch (Exception e) {

        }
    }


    public static void cleanup() {
        if (blurShader != null) {
            blurShader.close();
            blurShader = null;
        }
    }
}

