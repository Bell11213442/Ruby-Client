package com.radium.client.gui.utils;
// radium client

import net.minecraft.client.MinecraftClient;

public class DragHandler {
    private final int animationDuration = 300;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int targetX = 0;
    private int targetY = 0;
    private float currentX = 0;
    private float currentY = 0;
    private long animationStartTime = 0;
    private boolean isAnimating = false;

    public void startDrag(double mouseX, double mouseY, int elementX, int elementY) {
        this.isDragging = true;
        this.dragOffsetX = (int) mouseX - elementX;
        this.dragOffsetY = (int) mouseY - elementY;
        this.currentX = elementX;
        this.currentY = elementY;
        this.targetX = elementX;
        this.targetY = elementY;
        this.isAnimating = false;
    }

    public int[] updatePosition(double mouseX, double mouseY) {
        if (!isDragging) return new int[]{targetX, targetY};

        int newX = (int) mouseX - dragOffsetX;
        int newY = (int) mouseY - dragOffsetY;


        if (client != null && client.getWindow() != null) {
            int screenWidth = client.getWindow().getWidth();
            int screenHeight = client.getWindow().getHeight();


            newX = Math.max(-50, Math.min(screenWidth - 50, newX));
            newY = Math.max(0, Math.min(screenHeight - 50, newY));
        } else {

            newX = Math.max(0, newX);
            newY = Math.max(0, newY);
        }

        targetX = newX;
        targetY = newY;
        currentX = newX;
        currentY = newY;

        return new int[]{newX, newY};
    }

    public void stopDrag() {
        if (isDragging) {
            isDragging = false;
            isAnimating = true;
            animationStartTime = System.currentTimeMillis();
        }
    }

    public int[] getAnimatedPosition() {
        if (isDragging) {

            return new int[]{targetX, targetY};
        }

        if (!isAnimating) {
            return new int[]{targetX, targetY};
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;
        float progress = Math.min(1.0f, elapsed / (float) animationDuration);
        progress = easeOutCubic(progress);

        if (progress >= 1.0f) {
            isAnimating = false;
            currentX = targetX;
            currentY = targetY;
        } else {

            float deltaX = targetX - currentX;
            float deltaY = targetY - currentY;
            currentX += deltaX * progress * 0.3f;
            currentY += deltaY * progress * 0.3f;
        }

        return new int[]{Math.round(currentX), Math.round(currentY)};
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public boolean isDragging() {
        return isDragging;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public void setScreenBounds(int width, int height, int elementWidth, int elementHeight) {

        targetX = Math.max(0, Math.min(width - elementWidth, targetX));
        targetY = Math.max(0, Math.min(height - elementHeight, targetY));

        if (!isDragging) {
            currentX = targetX;
            currentY = targetY;
        }
    }

    public void setPosition(int x, int y) {
        this.targetX = x;
        this.targetY = y;
        this.currentX = x;
        this.currentY = y;
        this.isAnimating = false;
        this.isDragging = false;
    }
}

