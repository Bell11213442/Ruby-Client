package com.radium.client.utils.font.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RendererUtils {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    private static final FastMStack empty = new FastMStack();
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final char RND_START = 'a';
    private static final char RND_END = 'z';
    private static final Random RND = new Random();

    public static void setupRender() {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void endRender() {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    public static int lerp(int from, int to, double delta) {
        return (int) Math.floor(from + (to - from) * MathHelper.clamp(delta, 0, 1));
    }

    public static double lerp(double from, double to, double delta) {
        return from + (to - from) * MathHelper.clamp(delta, 0, 1);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static Color lerp(Color a, Color b, double c) {
        if (a == null || b == null) throw new NullPointerException();
        return new Color(lerp(a.getRed(), b.getRed(), c), lerp(a.getGreen(), b.getGreen(), c),
                lerp(a.getBlue(), b.getBlue(), c), lerp(a.getAlpha(), b.getAlpha(), c));
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static Color modify(Color original, int redOverwrite, int greenOverwrite, int blueOverwrite, int alphaOverwrite) {
        if (original == null) throw new NullPointerException();
        return new Color(
                redOverwrite == -1 ? original.getRed() : redOverwrite,
                greenOverwrite == -1 ? original.getGreen() : greenOverwrite,
                blueOverwrite == -1 ? original.getBlue() : blueOverwrite,
                alphaOverwrite == -1 ? original.getAlpha() : alphaOverwrite
        );
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static Vec3d translateVec3dWithMatrixStack(MatrixStack stack, Vec3d in) {
        if (stack == null || in == null) throw new NullPointerException();
        Matrix4f matrix = stack.peek().getPositionMatrix();
        Vector4f vec = new Vector4f((float) in.x, (float) in.y, (float) in.z, 1);
        vec.mul(matrix);
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }

    public static void registerBufferedImageTexture(Identifier i, BufferedImage bi) {
        if (i == null || bi == null) throw new NullPointerException();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", out);
            byte[] bytes = out.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();
            NativeImageBackedTexture tex = new NativeImageBackedTexture(NativeImage.read(data));
            MinecraftClient.getInstance()
                    .execute(() -> MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MatrixStack getEmptyMatrixStack() {
        if (!empty.isEmpty()) {
            throw new IllegalStateException(
                    "Supposed \"empty\" stack is not actually empty; someone does not clean up after themselves.");
        }
        empty.loadIdentity();
        return empty;
    }

    @Contract("-> new")
    public static Vec3d getCrosshairVector() {
        Camera camera = client.gameRenderer.getCamera();
        float pi = (float) Math.PI;
        float yawRad = (float) Math.toRadians(-camera.getYaw());
        float pitchRad = (float) Math.toRadians(-camera.getPitch());
        float f1 = MathHelper.cos(yawRad - pi);
        float f2 = MathHelper.sin(yawRad - pi);
        float f3 = -MathHelper.cos(pitchRad);
        float f4 = MathHelper.sin(pitchRad);
        return new Vec3d(f2 * f3, f4, f1 * f3).add(camera.getPos());
    }

    @Contract(value = "_ -> new", pure = true)
    public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        if (pos == null) throw new NullPointerException();
        Camera camera = client.getEntityRenderDispatcher().camera;
        int displayHeight = client.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getPos().x;
        double deltaY = pos.y - camera.getPos().y;
        double deltaZ = pos.z - camera.getPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(
                lastWorldSpaceMatrix);

        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);

        matrixProj.mul(matrixModel)
                .project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport,
                        target);

        return new Vec3d(target.x / client.getWindow().getScaleFactor(),
                (displayHeight - target.y) / client.getWindow().getScaleFactor(), target.z);
    }

    public static boolean screenSpaceCoordinateIsVisible(Vec3d pos) {
        return pos != null && pos.z > -1 && pos.z < 1;
    }

    @Contract(value = "_,_,_ -> new", pure = true)
    public static Vec3d screenSpaceToWorldSpace(double x, double y, double d) {
        Camera camera = client.getEntityRenderDispatcher().camera;
        int displayHeight = client.getWindow().getScaledHeight();
        int displayWidth = client.getWindow().getScaledWidth();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);

        matrixProj.mul(matrixModel)
                .mul(lastWorldSpaceMatrix)
                .unproject((float) x / displayWidth * viewport[2],
                        (float) (displayHeight - y) / displayHeight * viewport[3], (float) d, viewport, target);

        return new Vec3d(target.x, target.y, target.z).add(camera.getPos());
    }

    public static int getGuiScale() {
        return (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
    }

    private static String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(operand -> String.valueOf((char) RND.nextInt(RND_START, RND_END + 1)))
                .collect(Collectors.joining());
    }

    @Contract(value = "-> new", pure = true)
    public static Identifier randomIdentifier() {
        return Identifier.of("renderer", "temp/" + randomString(32));
    }
}
