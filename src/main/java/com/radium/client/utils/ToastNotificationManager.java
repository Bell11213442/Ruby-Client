package com.radium.client.utils;

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.RadiumGuiTheme;

import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ToastNotificationManager {
    private static final ToastNotificationManager INSTANCE = new ToastNotificationManager();
    private static final int TOAST_WIDTH = 250;
    private static final int TOAST_HEIGHT = 60;
    private static final int TOAST_PADDING = 10;
    private static final int TOAST_SPACING = 8;
    private static final int TOAST_MARGIN = 20;
    private static final int TOAST_RADIUS = 6;
    private static final long ANIMATION_DURATION = 200L;
    private final List<ToastNotification> activeToasts = new ArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private ToastNotificationManager() {
    }

    public static ToastNotificationManager getInstance() {
        return INSTANCE;
    }

    public void show(ToastNotification toast) {
        if (RadiumClient.mc == null) return;

        removeExpired();

        activeToasts.add(0, toast);

        while (activeToasts.size() > 5) {
            activeToasts.remove(activeToasts.size() - 1);
        }

        playSoundForType(toast.getType());
    }

    public void show(String title, String message, ToastNotification.ToastType type) {
        show(new ToastNotification(title, message, type));
    }

    public void render(DrawContext context, float delta) {
        if (RadiumClient.mc == null) return;

        RenderUtils.unscaledProjection();

        int screenWidth = mc.getWindow().getFramebufferWidth();
        int screenHeight = mc.getWindow().getFramebufferHeight();

        int startX = screenWidth - TOAST_WIDTH - TOAST_MARGIN;
        int currentY = screenHeight - TOAST_MARGIN;

        removeExpired();

        long currentTime = System.currentTimeMillis();
        Iterator<ToastNotification> iterator = activeToasts.iterator();
        while (iterator.hasNext()) {
            ToastNotification toast = iterator.next();

            long elapsed = currentTime - toast.getCreatedAt();
            if (elapsed < ANIMATION_DURATION) {
                float progress = elapsed / (float) ANIMATION_DURATION;
                toast.setAnimationProgress(easeOutCubic(progress));
            } else if (toast.isExpired()) {
                float progress = (elapsed - toast.getDuration()) / (float) ANIMATION_DURATION;
                if (progress >= 1f) {
                    iterator.remove();
                    continue;
                }
                toast.setAnimationProgress(1f - easeInCubic(progress));
            } else {
                toast.setAnimationProgress(1f);
            }

            renderToast(context, toast, startX, currentY - TOAST_HEIGHT, delta);

            currentY -= TOAST_HEIGHT + TOAST_SPACING;
        }

        RenderUtils.scaledProjection();
    }

    private void renderToast(DrawContext context, ToastNotification toast, int x, int y, float delta) {
        float animProgress = toast.getAnimationProgress();

        int animX = (int) (x + (TOAST_WIDTH * (1f - animProgress)));

        // Get theme from ThemeManager
        com.radium.client.themes.Theme theme = com.radium.client.themes.ThemeManager.getHudTheme();
        com.radium.client.modules.client.HUD hud = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.HUD.class);
        int alphaValue = hud != null ? hud.alpha.getValue().intValue() : 80;

        int backgroundColor = theme.getBackgroundColor(alphaValue);
        int shadowColor = theme.getShadowColor();
        int borderColor = theme.getBorderColor();
        int textColor = theme.getTextColor(hud);
        
        // Status color for title and border accents
        int statusColor = toast.getColor();
        int titleColor = RadiumGuiTheme.applyAlpha(statusColor, 1f);
        
        int radius = theme.getRadius(6);
        boolean useShadows = theme.useShadows();
        boolean useBorders = theme.useBorders();

        if (useShadows) {
             RenderUtils.fillRoundRect(context, animX + 1, y + 1, TOAST_WIDTH, TOAST_HEIGHT, radius, radius, radius, radius, shadowColor);
        }
        
        RenderUtils.fillRoundRect(context, animX, y, TOAST_WIDTH, TOAST_HEIGHT, radius, radius, radius, radius, backgroundColor);
        
        if (useBorders) {
            RenderUtils.drawRoundRect(context, animX, y, TOAST_WIDTH, TOAST_HEIGHT, radius, borderColor);
        }

        boolean useCustomFont = false;
        int textX = animX + TOAST_PADDING;
        int titleY = y + TOAST_PADDING + 2;
        int messageY = y + TOAST_PADDING + 18;

        context.getMatrices().push();
        // Custom font support removed
        context.drawText(mc.textRenderer, toast.getTitle(), textX, titleY, titleColor, false);
        if (toast.getMessage() != null && !toast.getMessage().isEmpty()) {
            context.drawText(mc.textRenderer, toast.getMessage(), textX, messageY, textColor, false);
        }
        context.getMatrices().pop();

        float remaining = toast.getRemainingProgress();
        if (remaining < 1f && remaining > 0f) {
            int maxBarWidth = TOAST_WIDTH - (radius * 2);
            int progressWidth = (int) (maxBarWidth * remaining);
            int progressY = y + TOAST_HEIGHT - 3;
            int progressX = animX + radius;

            RenderUtils.fillRoundRect(context, progressX, progressY, progressWidth, 2, 1, RadiumGuiTheme.applyAlpha(statusColor, 0.6f));
        }
    }

    private void removeExpired() {
        long currentTime = System.currentTimeMillis();
        activeToasts.removeIf(toast -> {
            long elapsed = currentTime - toast.getCreatedAt();
            return elapsed >= toast.getDuration() + ANIMATION_DURATION;
        });
    }

    private void playSoundForType(ToastNotification.ToastType type) {
        if (mc.getSoundManager() == null) return;

        switch (type) {
            case SUCCESS, MODULE_ENABLED, CONFIG_SAVE, CONFIG_LOAD, FRIEND_JOIN:
                mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f));
                break;
            case ERROR, MODULE_DISABLED, FRIEND_LEAVE:
                mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.8f));
                break;
            case WARNING, LOW_TOTEM:
                mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f));
                break;
            case INFO, KEYBIND_CHANGE:
                mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 0.9f));
                break;
        }
    }

    private float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    private float easeInCubic(float t) {
        return t * t * t;
    }

    public void clear() {
        activeToasts.clear();
    }
}
