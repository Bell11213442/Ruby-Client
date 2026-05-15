package com.radium.client.utils;
// radium client

public class TimerUtil {
    private long lastMS = System.currentTimeMillis();

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS >= time) {
            if (reset) reset();
            return true;
        }
        return false;
    }

    public boolean delay(float milliseconds) {
        return System.currentTimeMillis() - lastMS >= (long) milliseconds;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }
}

