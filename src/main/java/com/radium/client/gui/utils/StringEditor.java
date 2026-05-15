package com.radium.client.gui.utils;
// radium client

import com.radium.client.gui.settings.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

public class StringEditor {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private StringSetting editingString = null;
    private String currentString = "";
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;

    public void startEditing(StringSetting setting) {
        this.editingString = setting;
        this.currentString = setting.getValue();
        this.cursorPosition = currentString.length();
        this.selectionStart = -1;
        this.selectionEnd = -1;
    }

    public void stopEditing() {
        if (editingString != null) {
            editingString.setValue(currentString);
            editingString = null;
            currentString = "";
            cursorPosition = 0;
            selectionStart = -1;
            selectionEnd = -1;
        }
    }

    public void cancelEditing() {
        editingString = null;
        currentString = "";
        cursorPosition = 0;
        selectionStart = -1;
        selectionEnd = -1;
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private int getSelectionStartPos() {
        if (!hasSelection()) return cursorPosition;
        return Math.min(selectionStart, selectionEnd);
    }

    private int getSelectionEndPos() {
        if (!hasSelection()) return cursorPosition;
        return Math.max(selectionStart, selectionEnd);
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        int start = getSelectionStartPos();
        int end = getSelectionEndPos();
        return currentString.substring(start, end);
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = getSelectionStartPos();
        int end = getSelectionEndPos();
        currentString = currentString.substring(0, start) + currentString.substring(end);
        cursorPosition = start;
        selectionStart = -1;
        selectionEnd = -1;
    }

    private void insertText(String text) {
        if (hasSelection()) {
            deleteSelection();
        }
        currentString = currentString.substring(0, cursorPosition) + text + currentString.substring(cursorPosition);
        cursorPosition += text.length();
    }

    private void moveCursor(int direction, boolean extendSelection) {
        if (extendSelection) {
            if (selectionStart == -1) {
                selectionStart = cursorPosition;
            }
            selectionEnd = cursorPosition;
        } else {
            selectionStart = -1;
            selectionEnd = -1;
        }
        cursorPosition = Math.max(0, Math.min(currentString.length(), cursorPosition + direction));
        if (extendSelection) {
            selectionEnd = cursorPosition;
        }
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editingString == null) {
            return false;
        }

        boolean ctrlPressed = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;


        if (ctrlPressed && keyCode == GLFW.GLFW_KEY_A) {
            selectionStart = 0;
            selectionEnd = currentString.length();
            cursorPosition = currentString.length();
            return true;
        }


        if (ctrlPressed && keyCode == GLFW.GLFW_KEY_C) {
            if (hasSelection()) {
                String selected = getSelectedText();
                client.keyboard.setClipboard(selected);
            }
            return true;
        }


        if (ctrlPressed && keyCode == GLFW.GLFW_KEY_X) {
            if (hasSelection()) {
                String selected = getSelectedText();
                client.keyboard.setClipboard(selected);
                deleteSelection();
            }
            return true;
        }


        if (Screen.isPaste(keyCode)) {
            String clipboardContent = client.keyboard.getClipboard();
            if (clipboardContent != null) {
                insertText(clipboardContent);
            }
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE:
                cancelEditing();
                return true;
            case GLFW.GLFW_KEY_ENTER:
                stopEditing();
                return true;
            case GLFW.GLFW_KEY_BACKSPACE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    currentString = currentString.substring(0, cursorPosition - 1) + currentString.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
            case GLFW.GLFW_KEY_DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < currentString.length()) {
                    currentString = currentString.substring(0, cursorPosition) + currentString.substring(cursorPosition + 1);
                }
                return true;
            case GLFW.GLFW_KEY_LEFT:
                moveCursor(-1, shiftPressed);
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                moveCursor(1, shiftPressed);
                return true;
            case GLFW.GLFW_KEY_HOME:
                if (shiftPressed) {
                    if (selectionStart == -1) {
                        selectionStart = cursorPosition;
                    }
                    selectionEnd = 0;
                } else {
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                cursorPosition = 0;
                return true;
            case GLFW.GLFW_KEY_END:
                if (shiftPressed) {
                    if (selectionStart == -1) {
                        selectionStart = cursorPosition;
                    }
                    selectionEnd = currentString.length();
                } else {
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                cursorPosition = currentString.length();
                return true;
        }

        return false;
    }

    public boolean handleCharType(char chr, int modifiers) {
        if (editingString == null) {
            return false;
        }


        if (chr >= 32 && chr != 127) {
            insertText(String.valueOf(chr));
            return true;
        }

        return false;
    }

    public boolean isEditing() {
        return editingString != null;
    }

    public boolean isEditing(StringSetting setting) {
        return editingString == setting;
    }

    public String getCurrentText() {
        return currentString;
    }

    public StringSetting getEditingSetting() {
        return editingString;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public int getSelectionStart() {
        return selectionStart;
    }

    public int getSelectionEnd() {
        return selectionEnd;
    }

    public boolean hasSelectionPublic() {
        return hasSelection();
    }
}

