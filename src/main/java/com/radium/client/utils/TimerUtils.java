package com.radium.client.utils;
// radium client

public class TimerUtils {
    private long lastTime = 0;

    public boolean delay(float delay) {
        return delay((double) delay);
    }

    public boolean delay(double delay) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime >= delay) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        lastTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - lastTime;
    }

    public boolean hasElapsedTime(long delay) {
        return getElapsedTime() >= delay;
    }

    public long getLastTime() {
        return lastTime;
    }
}


