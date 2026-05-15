package com.radium.client.gui.utils;
// radium client

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

public class TextEditor {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private String text = "";
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean isActive = false;

    public void startEditing(String initialText) {
        this.text = initialText != null ? initialText : "";
        this.cursorPosition = this.text.length();
        this.selectionStart = -1;
        this.selectionEnd = -1;
        this.isActive = true;
    }

    public void stopEditing() {
        isActive = false;
    }

    public void cancelEditing() {
        text = "";
        cursorPosition = 0;
        selectionStart = -1;
        selectionEnd = -1;
        isActive = false;
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
        return text.substring(start, end);
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = getSelectionStartPos();
        int end = getSelectionEndPos();
        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        selectionStart = -1;
        selectionEnd = -1;
    }

    private void insertText(String insertText) {
        if (hasSelection()) {
            deleteSelection();
        }
        text = text.substring(0, cursorPosition) + insertText + text.substring(cursorPosition);
        cursorPosition += insertText.length();
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
        cursorPosition = Math.max(0, Math.min(text.length(), cursorPosition + direction));
        if (extendSelection) {
            selectionEnd = cursorPosition;
        }
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isActive) {
            return false;
        }

        boolean ctrlPressed = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;


        if (ctrlPressed && keyCode == GLFW.GLFW_KEY_A) {
            selectionStart = 0;
            selectionEnd = text.length();
            cursorPosition = text.length();
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
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
            case GLFW.GLFW_KEY_DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
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
                    selectionEnd = text.length();
                } else {
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                cursorPosition = text.length();
                return true;
        }

        return false;
    }

    public boolean handleCharType(char chr, int modifiers) {
        if (!isActive) {
            return false;
        }


        if (chr >= 32 && chr != 127) {
            insertText(String.valueOf(chr));
            return true;
        }

        return false;
    }

    public String getText() {
        return text;
    }

    public void setText(String newText) {
        this.text = newText != null ? newText : "";
        this.cursorPosition = Math.min(cursorPosition, this.text.length());
    }

    public boolean isActive() {
        return isActive;
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


