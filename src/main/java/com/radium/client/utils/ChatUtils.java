package com.radium.client.utils;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.Module;
import net.minecraft.text.Text;

public final class ChatUtils {

    private ChatUtils() {
    }

    public static void m(String message) {
        send(buildPrefix() + "§f" + message);
    }

    public static void w(String message) {
        send(buildPrefix() + "§e" + message);
    }

    public static void e(String message) {
        send(buildPrefix() + "§c" + message);
    }

    private static void send(String fullMessage) {
        if (RadiumClient.mc != null && RadiumClient.mc.player != null) {
            RadiumClient.mc.player.sendMessage(Text.literal(fullMessage), false);
        }
    }

    private static String buildPrefix() {
        String moduleName = inferCallerModuleName();
        if (moduleName == null || moduleName.isEmpty()) moduleName = "General";
        return "§c[Radium] §b" + moduleName + " §f| ";
    }

    private static String inferCallerModuleName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className == null) continue;
            if (!className.startsWith("com.radium.client.modules.")) continue;
            try {
                Class<?> clazz = Class.forName(className);
                if (Module.class.isAssignableFrom(clazz)) {
                    String simple = clazz.getSimpleName();

                    if (RadiumClient.getModuleManager() != null) {
                        for (Module mod : RadiumClient.getModuleManager().getModules()) {
                            if (mod != null && mod.getClass() == clazz) {
                                return mod.getName();
                            }
                        }
                    }
                    return simple;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}



