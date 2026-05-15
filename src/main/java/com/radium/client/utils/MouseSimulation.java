package com.radium.client.utils;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.mixins.MinecraftClientAccessor;
import com.radium.client.mixins.MouseAccessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MouseSimulation {
    private static final ExecutorService clickExecutor = Executors.newFixedThreadPool(2);

    public static void mouseClick(int button) {
        mouseClick(button, 35);
    }

    public static void mouseClick(int button, int millis) {
        clickExecutor.submit(() -> {
            try {
                mousePress(button);
                Thread.sleep(millis);
                mouseRelease(button);
            } catch (InterruptedException ignored) {
            }
        });
    }

    public static void mousePress(int button) {
        MouseAccessor mouse = (MouseAccessor) ((MinecraftClientAccessor) RadiumClient.mc).getMouse();
        mouse.invokeOnMouseButton(RadiumClient.mc.getWindow().getHandle(), button, 1, 0);
    }

    public static void mouseRelease(int button) {
        MouseAccessor mouse = (MouseAccessor) ((MinecraftClientAccessor) RadiumClient.mc).getMouse();
        mouse.invokeOnMouseButton(RadiumClient.mc.getWindow().getHandle(), button, 0, 0);
    }
}

