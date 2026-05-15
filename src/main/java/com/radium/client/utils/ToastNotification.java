package com.radium.client.utils;
// radium client

public class ToastNotification {
    private final String title;
    private final String message;
    private final ToastType type;
    private final long createdAt;
    private final long duration;
    private float animationProgress = 0f;
    private float targetProgress = 1f;

    public ToastNotification(String title, String message, ToastType type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.duration = duration;
        this.createdAt = System.currentTimeMillis();
    }

    public ToastNotification(String title, String message, ToastType type) {
        this(title, message, type, 3000L); // Default 3 seconds
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public ToastType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt >= duration;
    }

    public float getRemainingProgress() {
        long elapsed = System.currentTimeMillis() - createdAt;
        return Math.max(0f, 1f - (elapsed / (float) duration));
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public void setAnimationProgress(float progress) {
        this.animationProgress = progress;
    }

    public float getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(float target) {
        this.targetProgress = target;
    }

    public int getColor() {
        return switch (type) {
            case SUCCESS, MODULE_ENABLED, CONFIG_SAVE, CONFIG_LOAD, FRIEND_JOIN -> 0xFF4CAF50; // Green
            case ERROR, MODULE_DISABLED, FRIEND_LEAVE -> 0xFFF44336; // Red
            case WARNING, LOW_TOTEM -> 0xFFFF9800; // Orange
            case INFO, KEYBIND_CHANGE, CUSTOM -> 0xFF2196F3; // Blue
        };
    }

    public enum ToastType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        MODULE_ENABLED,
        MODULE_DISABLED,
        CONFIG_SAVE,
        CONFIG_LOAD,
        KEYBIND_CHANGE,
        FRIEND_JOIN,
        FRIEND_LEAVE,
        LOW_TOTEM,
        CUSTOM
    }
}
