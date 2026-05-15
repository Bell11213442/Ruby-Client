package com.radium.client.utils;
// radium client

import com.radium.client.client.KeybindManager;
import com.radium.client.client.RadiumClient;
import org.lwjgl.glfw.GLFW;

public final class KeyUtils {
    public static boolean isKeyPressed(int keyCode) {
        if (RadiumClient.mc.getWindow() == null) return false;


        if (KeybindManager.isMouseButton(keyCode)) {
            int mouseButton = KeybindManager.getMouseButtonFromKeyCode(keyCode);
            if (mouseButton >= 0) {
                return GLFW.glfwGetMouseButton(RadiumClient.mc.getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
            }
        }


        if (keyCode <= 8) {
            return GLFW.glfwGetMouseButton(RadiumClient.mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        }

        return GLFW.glfwGetKey(RadiumClient.mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }
}

