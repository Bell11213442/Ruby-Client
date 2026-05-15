package com.radium.client.utils;
// radium client

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

public class PingUtils {

    private static final AtomicLong cachedPing = new AtomicLong(-1);
    private static long lastPingTime = 0;

    public static long getCachedPing() {
        return cachedPing.get();
    }

    public static void updatePingAsync() {
        long now = System.currentTimeMillis();

        if (now - lastPingTime < 1000) return;
        lastPingTime = now;

        new Thread(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getCurrentServerEntry() == null) {
                cachedPing.set(-1);
                return;
            }

            ServerInfo server = mc.getCurrentServerEntry();
            String address = server.address;
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

            try (Socket socket = new Socket()) {
                long start = System.nanoTime();
                socket.connect(new InetSocketAddress(host, port), 1000);
                long end = System.nanoTime();
                cachedPing.set((end - start) / 1_000_000);
            } catch (Exception e) {
                cachedPing.set(-1);
            }
        }).start();
    }
}

