package com.radium.client.client;
// radium client

import com.radium.client.modules.Module;
import com.radium.client.modules.client.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KeybindManager {
    private static final int MOUSE_BUTTON_OFFSET = 1000;
    private final Map<Integer, Boolean> keyStates = new HashMap<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public KeybindManager() {
    }

    public static String getKeyName(int keyCode) {
        if (keyCode <= 0) return "None";

        if (keyCode >= MOUSE_BUTTON_OFFSET) {
            return getMouseButtonName(keyCode - MOUSE_BUTTON_OFFSET);
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE:
                return "Space";
            case GLFW.GLFW_KEY_ESCAPE:
                return "Escape";
            case GLFW.GLFW_KEY_ENTER:
                return "Enter";
            case GLFW.GLFW_KEY_TAB:
                return "Tab";
            case GLFW.GLFW_KEY_BACKSPACE:
                return "Backspace";
            case GLFW.GLFW_KEY_INSERT:
                return "Insert";
            case GLFW.GLFW_KEY_DELETE:
                return "Delete";
            case GLFW.GLFW_KEY_RIGHT:
                return "Right Arrow";
            case GLFW.GLFW_KEY_LEFT:
                return "Left Arrow";
            case GLFW.GLFW_KEY_DOWN:
                return "Down Arrow";
            case GLFW.GLFW_KEY_UP:
                return "Up Arrow";
            case GLFW.GLFW_KEY_PAGE_UP:
                return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN:
                return "Page Down";
            case GLFW.GLFW_KEY_HOME:
                return "Home";
            case GLFW.GLFW_KEY_END:
                return "End";
            case GLFW.GLFW_KEY_CAPS_LOCK:
                return "Caps Lock";
            case GLFW.GLFW_KEY_SCROLL_LOCK:
                return "Scroll Lock";
            case GLFW.GLFW_KEY_NUM_LOCK:
                return "Num Lock";
            case GLFW.GLFW_KEY_PRINT_SCREEN:
                return "Print Screen";
            case GLFW.GLFW_KEY_PAUSE:
                return "Pause";
            case GLFW.GLFW_KEY_LEFT_SHIFT:
                return "Left Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL:
                return "Left Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT:
                return "Left Alt";
            case GLFW.GLFW_KEY_LEFT_SUPER:
                return "Left Super";
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                return "Right Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                return "Right Ctrl";
            case GLFW.GLFW_KEY_RIGHT_ALT:
                return "Right Alt";
            case GLFW.GLFW_KEY_RIGHT_SUPER:
                return "Right Super";
        }

        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }

        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return "Numpad " + (keyCode - GLFW.GLFW_KEY_KP_0);
        }

        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) keyCode);
        }

        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) keyCode);
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            default -> "Unknown (" + keyCode + ")";
        };

    }

    private static String getMouseButtonName(int mouseButton) {
        return switch (mouseButton) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "Left Click";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Right Click";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Middle Click";
            case GLFW.GLFW_MOUSE_BUTTON_4 -> "Mouse 4";
            case GLFW.GLFW_MOUSE_BUTTON_5 -> "Mouse 5";
            case GLFW.GLFW_MOUSE_BUTTON_6 -> "Mouse 6";
            case GLFW.GLFW_MOUSE_BUTTON_7 -> "Mouse 7";
            case GLFW.GLFW_MOUSE_BUTTON_8 -> "Mouse 8";
            default -> "Mouse " + (mouseButton + 1);
        };
    }

    public static int getMouseButtonKeyCode(int mouseButton) {
        return MOUSE_BUTTON_OFFSET + mouseButton;
    }

    public static boolean isMouseButton(int keyCode) {
        return keyCode >= MOUSE_BUTTON_OFFSET;
    }

    public static int getMouseButtonFromKeyCode(int keyCode) {
        if (isMouseButton(keyCode)) {
            return keyCode - MOUSE_BUTTON_OFFSET;
        }
        return -1;
    }

    public void checkKeybinds() {
        if (RadiumClient.moduleManager == null) return;


        for (Module module : RadiumClient.moduleManager.getModules()) {
            int keyCode = module.getKeyBind();
            if (keyCode > 0) {
                if (module.getName().equals("Radium")) {
                    checkKey(keyCode, module);
                } else if (mc.currentScreen == null) {
                    checkKey(keyCode, module);
                }
            }
            if (keyCode <= 0 && module instanceof ClickGUI) {
                module.setKeyBind(344);
            }
        }
    }

    // pretty ass code
    private void checkKey(int keyCode, Module module) {
        if (module == null || mc == null) return;

        boolean currentState = keyCode >= MOUSE_BUTTON_OFFSET
                ? isMouseButtonPressed(keyCode - MOUSE_BUTTON_OFFSET)
                : isKeyPressed(keyCode);

        boolean previousState = keyStates.getOrDefault(keyCode, false);

        if (module instanceof ClickGUI gui) {
            int activeKey = gui.getKeyBind() > 0 ? gui.getKeyBind() : GLFW.GLFW_KEY_RIGHT_SHIFT;
            boolean activePressed = isKeyPressed(activeKey);
            boolean activePrevious = keyStates.getOrDefault(activeKey, false);

            if (activePressed && !activePrevious) {
                mc.execute(() -> {
                    Screen screen = mc.currentScreen;

                    if (!(screen instanceof com.radium.client.gui.ClickGuiScreen)) {
                        gui.updateGuiScreen();
                        if (gui.currentGuiScreen != null && gui.canOpenGUI()) {
                            mc.setScreen(gui.currentGuiScreen);
                        }
                    } else {
                        try {
                            screen.close();
                        } catch (Exception ignored) {}
                    }
                });
            }

            keyStates.put(activeKey, activePressed);
        } else {
            if (currentState && !previousState) {
                module.toggle();
            }
        }
        keyStates.put(keyCode, currentState);
    }

    private boolean isKeyPressed(int keyCode) {
        if (mc.getWindow() == null) return false;

        try {
            return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMouseButtonPressed(int mouseButton) {
        if (mc.getWindow() == null) return false;

        try {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }

    public void autoAssignKeybinds() {
        if (RadiumClient.moduleManager == null) return;

        Set<Integer> usedKeys = new java.util.HashSet<>();
        for (Module module : RadiumClient.moduleManager.getModules()) {
            if (module.getKeyBind() != -1) {
                usedKeys.add(module.getKeyBind());
            }
        }

        usedKeys.add(GLFW.GLFW_KEY_RIGHT_SHIFT);

        int[] defaultKeys = {
                GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H,
                GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_M,
                GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_Q,
                GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_X,
                GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_Z,
                GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4,
                GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8,
                GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12,
                getMouseButtonKeyCode(GLFW.GLFW_MOUSE_BUTTON_4),
                getMouseButtonKeyCode(GLFW.GLFW_MOUSE_BUTTON_5),
                getMouseButtonKeyCode(GLFW.GLFW_MOUSE_BUTTON_6),
                getMouseButtonKeyCode(GLFW.GLFW_MOUSE_BUTTON_7),
                getMouseButtonKeyCode(GLFW.GLFW_MOUSE_BUTTON_8)
        };

        int keyIndex = 0;
        for (Module module : RadiumClient.moduleManager.getModules()) {
            if (module.getKeyBind() == -1 && keyIndex < defaultKeys.length) {
                while (keyIndex < defaultKeys.length && usedKeys.contains(defaultKeys[keyIndex])) {
                    keyIndex++;
                }

                if (keyIndex < defaultKeys.length) {
                    module.setKeyBind(defaultKeys[keyIndex]);
                    usedKeys.add(defaultKeys[keyIndex]);
                    keyIndex++;
                }
            }
        }
    }
}
