package com.radium.client.gui.utils;
// radium client

import java.util.HashMap;
import java.util.Map;

public class AnimationManager {
    private final Map<String, Animation> animations = new HashMap<>();
    private final long startTime;
    private boolean isGlobalAnimating = false;

    public AnimationManager() {
        this.startTime = System.currentTimeMillis();
        this.isGlobalAnimating = true;
    }

    public void update() {
        long currentTime = System.currentTimeMillis();

        if (isGlobalAnimating) {
            long elapsed = currentTime - startTime;
            if (elapsed >= 350) {
                isGlobalAnimating = false;
            }
        }

        animations.entrySet().removeIf(entry -> {
            Animation anim = entry.getValue();
            anim.update(currentTime);
            return anim.isFinished();
        });
    }

    public float getProgress() {
        if (!isGlobalAnimating) return 1.0f;

        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1.0f, elapsed / 350f);
        return easeOutCubic(progress);
    }

    public void startAnimation(String key, float from, float to, long duration) {
        animations.put(key, new Animation(from, to, duration, System.currentTimeMillis()));
    }

    public float getAnimationValue(String key, float defaultValue) {
        Animation anim = animations.get(key);
        return anim != null ? anim.getValue() : defaultValue;
    }

    public boolean isAnimating(String key) {
        Animation anim = animations.get(key);
        return anim != null && !anim.isFinished();
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : (float) (1 - Math.pow(-2 * t + 2, 3) / 2);
    }

    private static class Animation {
        private final float from;
        private final float to;
        private final long duration;
        private final long startTime;
        private boolean finished = false;

        public Animation(float from, float to, long duration, long startTime) {
            this.from = from;
            this.to = to;
            this.duration = duration;
            this.startTime = startTime;
        }

        public void update(long currentTime) {
            long elapsed = currentTime - startTime;
            if (elapsed >= duration) {
                finished = true;
            }
        }

        public float getValue() {
            if (finished) return to;

            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, (float) elapsed / duration);
            progress = easeOutCubic(progress);

            return from + (to - from) * progress;
        }

        public boolean isFinished() {
            return finished;
        }

        private float easeOutCubic(float t) {
            return 1 - (float) Math.pow(1 - t, 3);
        }
    }
}

