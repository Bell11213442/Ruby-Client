package com.radium.client.gui.components;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.utils.GuiUtils;
import com.radium.client.gui.utils.TextEditor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class SearchBar {
    private final TextEditor editor = new TextEditor();
    private final MinecraftClient client = MinecraftClient.getInstance();
    private int lastWidth = 180;

    public void render(DrawContext context, int x, int y, int width, float animationProgress) {
        int height = 20;

        int bgColor = editor.isActive() ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(),
                        animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getModuleBackground(),
                        animationProgress * RadiumGuiTheme.getPanelAlpha());

        GuiUtils.drawRoundedRect(context, x, y, width, height, 5, bgColor);

        String text = editor.getText();
        String displayText = text;
        if (editor.isActive()) {

            int cursorPos = editor.getCursorPosition();
            String beforeCursor = text.substring(0, cursorPos);
            String afterCursor = text.substring(cursorPos);
            displayText = beforeCursor + "_" + afterCursor;
        } else if (text.isEmpty()) {
            displayText = "Search for an item...";
        }

        int textColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getTextColor(),
                (int) (animationProgress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, displayText, x + 5, y + 6, textColor, false);
    }

    public void setWidth(int width) {
        this.lastWidth = width;
    }

    public boolean handleClick(double mx, double my, int button, int x, int y) {
        if (button == 0 && GuiUtils.isHovered(mx, my, x, y, lastWidth, 20)) {
            if (!editor.isActive()) {
                editor.startEditing(editor.getText());
            }
            return true;
        }

        if (editor.isActive() && !GuiUtils.isHovered(mx, my, x, y, lastWidth, 20)) {
            editor.stopEditing();
        }

        return false;
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        return editor.handleKeyPress(keyCode, scanCode, modifiers);
    }

    public boolean handleCharType(char chr, int modifiers) {
        return editor.handleCharType(chr, modifiers);
    }

    public String getQuery() {
        return editor.getText();
    }

    public void clear() {
        editor.setText("");
        editor.stopEditing();
    }

    public boolean isEditing() {
        return editor.isActive();
    }
}

