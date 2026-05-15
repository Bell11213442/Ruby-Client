package com.radium.client.gui.panels;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ColorPickerPanel {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int panelWidth = 180;
    private final int panelHeight = 200;
    private final int headerHeight = 25;
    private final int padding = 10;
    private final int hueBarHeight = 100;
    private final int alphaBarHeight = 15;
    private final int hexInputHeight = 22;
    private final int previewSize = 30;
    private final com.radium.client.gui.utils.TextEditor hexEditor = new com.radium.client.gui.utils.TextEditor();
    private final int colorSquareSize = 100;
    private final int hueBarWidth = 15;
    private ColorSetting editingColor;
    private int panelX = 1000, panelY = 600;
    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;
    private float alpha = 1f;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private boolean draggingColorDot = false;
    private boolean draggingHueBar = false;
    private boolean draggingAlphaBar = false;
    private boolean editingHex = false;
    private long animationStartTime;
    private boolean isAnimating;

    public ColorPickerPanel() {
        this.animationStartTime = System.currentTimeMillis();
        this.isAnimating = true;
    }

    public void setEditingColor(ColorSetting color) {
        this.editingColor = color;
        if (color != null) {
            Color c = color.getValue();
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
            this.alpha = c.getAlpha() / 255f;
            updateHexFromColor();
        } else {
            editingHex = false;
            hexEditor.setText("");
            hexEditor.stopEditing();
        }
    }

    public void openAt(ColorSetting color, int x, int y) {
        setEditingColor(color);
        this.animationStartTime = System.currentTimeMillis();
        this.isAnimating = true;

        if (client != null && client.getWindow() != null) {
            int screenW = client.getWindow().getWidth();
            int screenH = client.getWindow().getHeight();

            x = Math.max(5, Math.min(screenW - panelWidth - 5, x));
            y = Math.max(5, Math.min(screenH - panelHeight - 5, y));
        }

        panelX = x;
        panelY = y;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float progress) {
        if (editingColor == null) return;

        float animationProgress = 1.0f;
        if (isAnimating) {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            animationProgress = easeOutCubic(Math.min(1.0f, elapsed / (float) RadiumGuiTheme.ANIMATION_DURATION));
            if (animationProgress >= 1.0f) isAnimating = false;
        }

        int cornerRadius = getCachedCornerRadius();

        int bgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsPanelColor(), animationProgress * RadiumGuiTheme.getPanelAlpha());
        RenderUtils.fillRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, bgColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress * 0.5f);
        RenderUtils.drawRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, borderColor);

        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), animationProgress * RadiumGuiTheme.getPanelAlpha());
        RenderUtils.fillRoundTabTop(context, panelX, panelY, panelWidth, headerHeight, cornerRadius, headerColor);

        String title = "Color Picker";
        int titleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, title, panelX + padding, panelY + (headerHeight - 8) / 2, titleColor, true);

        String closeText = "✕";
        boolean closeHovered = isHovered(mouseX, mouseY, panelX + panelWidth - 20, panelY + 4, 16, 16);
        int closeColor = closeHovered ? RadiumGuiTheme.getAccentColor() : 0xFFCCCCCC;
        context.drawText(client.textRenderer, closeText, panelX + panelWidth - 18, panelY + (headerHeight - 8) / 2,
                RadiumGuiTheme.applyAlpha(closeColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        int contentY = panelY + headerHeight + padding;

        int colorSquareX = panelX + padding;
        int colorSquareY = contentY;

        int hueBarX = colorSquareX + colorSquareSize + padding;
        int hueBarY = colorSquareY;

        renderColorSquare(context, colorSquareX, colorSquareY, animationProgress);
        renderHueBar(context, hueBarX, hueBarY, animationProgress);

        float dotX = colorSquareX + (saturation * colorSquareSize);
        float dotY = colorSquareY + ((1f - brightness) * colorSquareSize);
        renderColorDot(context, (int) dotX, (int) dotY, animationProgress);

        float hueY = hueBarY + (hue * hueBarHeight);
        renderHueIndicator(context, hueBarX, (int) hueY, animationProgress);

        int alphaBarY = colorSquareY + colorSquareSize + padding;
        int alphaBarX = colorSquareX;
        int alphaBarWidth = colorSquareSize + padding + hueBarWidth;

        renderAlphaBar(context, alphaBarX, alphaBarY, alphaBarWidth, animationProgress);

        float alphaX = alphaBarX + (alpha * alphaBarWidth);
        renderAlphaIndicator(context, (int) alphaX, alphaBarY, animationProgress);

        int hexY = alphaBarY + alphaBarHeight + padding;
        int hexX = panelX + padding;
        int hexWidth = panelWidth - padding * 2 - previewSize - 5;

        renderHexInput(context, hexX, hexY, hexWidth, mouseX, mouseY, animationProgress);

        int previewX = hexX + hexWidth + 5;
        int previewY = hexY - 4;

        renderColorPreview(context, previewX, previewY, animationProgress);
    }

    private void renderColorSquare(DrawContext context, int x, int y, float progress) {
        int step = 2;
        for (int py = 0; py < colorSquareSize; py += step) {
            float brightnessValue = 1f - ((float) py / colorSquareSize);
            for (int px = 0; px < colorSquareSize; px += step) {
                float saturationValue = (float) px / colorSquareSize;
                int color = Color.HSBtoRGB(hue, saturationValue, brightnessValue);
                context.fill(x + px, y + py, x + px + step, y + py + step,
                        RadiumGuiTheme.applyAlpha(color, progress));
            }
        }

        RenderUtils.drawRoundRect(context, x, y, colorSquareSize, colorSquareSize, 4,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), progress * 0.6f));
    }

    private void renderHueBar(DrawContext context, int x, int y, float progress) {
        RenderUtils.fillRoundRect(context, x, y, hueBarWidth, hueBarHeight, 4,
                RadiumGuiTheme.applyAlpha(0xFF222222, progress * RadiumGuiTheme.getPanelAlpha()));

        int step = 2;
        for (int i = 0; i < hueBarHeight; i += step) {
            float hueValue = (float) i / hueBarHeight;
            int color = Color.HSBtoRGB(hueValue, 1f, 1f);
            context.fill(x + 2, y + i, x + hueBarWidth - 2, y + i + step,
                    RadiumGuiTheme.applyAlpha(color, progress));
        }

        RenderUtils.drawRoundRect(context, x, y, hueBarWidth, hueBarHeight, 4,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), progress * 0.6f));
    }

    private void renderAlphaBar(DrawContext context, int x, int y, int width, float progress) {
        RenderUtils.fillRoundRect(context, x, y, width, alphaBarHeight, 4,
                RadiumGuiTheme.applyAlpha(0xFF333333, progress * RadiumGuiTheme.getPanelAlpha()));

        Color baseColor = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        int step = 2;
        for (int i = 0; i < width; i += step) {
            float alphaValue = (float) i / width;
            int color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                    (int) (alphaValue * 255)).getRGB();
            context.fill(x + i, y + 2, x + i + step, y + alphaBarHeight - 2, color);
        }

        RenderUtils.drawRoundRect(context, x, y, width, alphaBarHeight, 4,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), progress * 0.6f));
    }

    private void renderColorDot(DrawContext context, int x, int y, float progress) {
        int colorSquareX = panelX + padding;
        int colorSquareY = panelY + headerHeight + padding;

        x = Math.max(colorSquareX, Math.min(colorSquareX + colorSquareSize, x));
        y = Math.max(colorSquareY, Math.min(colorSquareY + colorSquareSize, y));

        int dotSize = 8;

        RenderUtils.fillRoundRect(context, x - dotSize / 2 - 2, y - dotSize / 2 - 2, dotSize + 4, dotSize + 4, dotSize / 2 + 2,
                RadiumGuiTheme.applyAlpha(0xFF000000, progress * 0.8f));

        RenderUtils.fillRoundRect(context, x - dotSize / 2 - 1, y - dotSize / 2 - 1, dotSize + 2, dotSize + 2, dotSize / 2 + 1,
                RadiumGuiTheme.applyAlpha(0xFFFFFFFF, progress));

        int currentColor = getCurrentColorInt();
        RenderUtils.fillRoundRect(context, x - dotSize / 2, y - dotSize / 2, dotSize, dotSize, dotSize / 2, currentColor);
    }

    private void renderHueIndicator(DrawContext context, int x, int y, float progress) {
        int lineWidth = hueBarWidth + 6;
        int lineHeight = 4;
        int lineX = x - 3;
        int lineY = y - 2;

        RenderUtils.fillRoundRect(context, lineX - 1, lineY - 1, lineWidth + 2, lineHeight + 2, 3,
                RadiumGuiTheme.applyAlpha(0xFF000000, progress * 0.8f));

        RenderUtils.fillRoundRect(context, lineX, lineY, lineWidth, lineHeight, 2,
                RadiumGuiTheme.applyAlpha(0xFFFFFFFF, progress));

        int hueColor = Color.HSBtoRGB(hue, 1f, 1f);
        RenderUtils.fillRoundRect(context, lineX + 1, lineY + 1, lineWidth - 2, lineHeight - 2, 2,
                RadiumGuiTheme.applyAlpha(hueColor, progress));
    }

    private void renderAlphaIndicator(DrawContext context, int x, int y, float progress) {
        int lineWidth = 4;
        int lineHeight = alphaBarHeight + 6;
        int lineX = x - 2;
        int lineY = y - 3;

        RenderUtils.fillRoundRect(context, lineX - 1, lineY - 1, lineWidth + 2, lineHeight + 2, 3,
                RadiumGuiTheme.applyAlpha(0xFF000000, progress * 0.8f));

        RenderUtils.fillRoundRect(context, lineX, lineY, lineWidth, lineHeight, 2,
                RadiumGuiTheme.applyAlpha(0xFFFFFFFF, progress));

        int currentColor = getCurrentColorInt();
        RenderUtils.fillRoundRect(context, lineX + 1, lineY + 1, lineWidth - 2, lineHeight - 2, 2,
                RadiumGuiTheme.applyAlpha(currentColor, progress));
    }

    private void renderHexInput(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float progress) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, hexInputHeight);
        int bgColor = (editingHex || hovered) ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.5f) :
                RadiumGuiTheme.applyAlpha(0xFF333333, progress * RadiumGuiTheme.getPanelAlpha());

        RenderUtils.fillRoundRect(context, x, y, width, hexInputHeight, 6, bgColor);

        String hexText = hexEditor.getText();
        String displayText;
        if (editingHex && hexEditor.isActive()) {
            int cursorPos = hexEditor.getCursorPosition();
            String beforeCursor = hexText.substring(0, Math.min(cursorPos, hexText.length()));
            String afterCursor = hexText.substring(Math.min(cursorPos, hexText.length()));
            displayText = "#" + beforeCursor + "_" + afterCursor;
        } else if (hexText.isEmpty()) {
            displayText = "#" + getHexFromColor();
        } else {
            displayText = "#" + hexText;
        }

        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, displayText, x + 6, y + (hexInputHeight - 8) / 2, textColor, false);

        RenderUtils.drawRoundRect(context, x, y, width, hexInputHeight, 6,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), progress * 0.6f));
    }

    private void renderColorPreview(DrawContext context, int x, int y, float progress) {
        RenderUtils.fillRoundRect(context, x, y, previewSize, previewSize, 6,
                RadiumGuiTheme.applyAlpha(0xFF333333, progress * RadiumGuiTheme.getPanelAlpha()));

        int currentColor = getCurrentColorInt();
        RenderUtils.fillRoundRect(context, x, y, previewSize, previewSize, 6, currentColor);

        RenderUtils.drawRoundRect(context, x, y, previewSize, previewSize, 6,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), progress * 0.6f));
    }

    private int getCurrentColorInt() {
        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        int alphaInt = (int) (alpha * 255);
        return (alphaInt << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    private String getHexFromColor() {
        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        int alphaInt = (int) (alpha * 255);
        return String.format("%02X%02X%02X%02X", alphaInt, color.getRed(), color.getGreen(), color.getBlue());
    }

    private void updateHexFromColor() {
        hexEditor.setText(getHexFromColor());
    }

    private void updateColorFromHex() {
        try {
            String hex = hexEditor.getText().replace("#", "").trim();

            if (hex.length() == 8) {
                int a = Integer.parseInt(hex.substring(0, 2), 16);
                int r = Integer.parseInt(hex.substring(2, 4), 16);
                int g = Integer.parseInt(hex.substring(4, 6), 16);
                int b = Integer.parseInt(hex.substring(6, 8), 16);

                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                this.hue = hsb[0];
                this.saturation = hsb[1];
                this.brightness = hsb[2];
                this.alpha = a / 255f;

                if (editingColor != null) {
                    editingColor.setValue(new Color(r, g, b, a));
                }
            } else if (hex.length() == 6) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);

                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                this.hue = hsb[0];
                this.saturation = hsb[1];
                this.brightness = hsb[2];
                this.alpha = 1f;

                if (editingColor != null) {
                    editingColor.setValue(new Color(r, g, b, 255));
                }
            }
        } catch (NumberFormatException e) {
        }
    }

    public boolean handleClick(double mx, double my, int button) {
        if (editingColor == null) return false;

        if (isHovered(mx, my, panelX + panelWidth - 20, panelY + 4, 16, 16)) {
            editingColor = null;
            draggingColorDot = false;
            draggingHueBar = false;
            draggingAlphaBar = false;
            editingHex = false;
            return true;
        }

        if (button == 0 && isHovered(mx, my, panelX, panelY, panelWidth, headerHeight)) {
            if (!isHovered(mx, my, panelX + panelWidth - 20, panelY + 4, 16, 16)) {
                dragging = true;
                dragOffsetX = (int) mx - panelX;
                dragOffsetY = (int) my - panelY;
                return true;
            }
        }

        int colorSquareX = panelX + padding;
        int colorSquareY = panelY + headerHeight + padding;

        if (button == 0 && isHovered(mx, my, colorSquareX, colorSquareY, colorSquareSize, colorSquareSize)) {
            draggingColorDot = true;
            updateColorFromPosition((int) mx, (int) my);
            return true;
        }

        int hueBarX = colorSquareX + colorSquareSize + padding;
        int hueBarY = colorSquareY;

        if (button == 0 && isHovered(mx, my, hueBarX, hueBarY, hueBarWidth, hueBarHeight)) {
            draggingHueBar = true;
            updateHueFromPosition((int) my);
            return true;
        }

        int alphaBarY = colorSquareY + colorSquareSize + padding;
        int alphaBarX = colorSquareX;
        int alphaBarWidth = colorSquareSize + padding + hueBarWidth;

        if (button == 0 && isHovered(mx, my, alphaBarX, alphaBarY, alphaBarWidth, alphaBarHeight)) {
            draggingAlphaBar = true;
            updateAlphaFromPosition((int) mx, alphaBarWidth);
            return true;
        }

        int hexY = alphaBarY + alphaBarHeight + padding;
        int hexX = panelX + padding;
        int hexWidth = panelWidth - padding * 2 - previewSize - 5;

        if (button == 0 && isHovered(mx, my, hexX, hexY, hexWidth, hexInputHeight)) {
            editingHex = true;
            hexEditor.startEditing(getHexFromColor());
            return true;
        } else if (!isHovered(mx, my, hexX, hexY, hexWidth, hexInputHeight)) {
            editingHex = false;
            hexEditor.stopEditing();
            updateColorFromHex();
        }

        return isHovered(mx, my, panelX, panelY, panelWidth, panelHeight);
    }

    public boolean handleDrag(double mx, double my, int button, double dx, double dy) {
        if (editingColor == null) return false;

        if (dragging && button == 0) {
            panelX = (int) mx - dragOffsetX;
            panelY = (int) my - dragOffsetY;

            if (client != null && client.getWindow() != null) {
                panelX = Math.max(0, Math.min(client.getWindow().getWidth() - panelWidth, panelX));
                panelY = Math.max(0, Math.min(client.getWindow().getHeight() - panelHeight, panelY));
            }
            return true;
        }

        if (button == 0) {
            if (draggingColorDot) {
                updateColorFromPosition((int) mx, (int) my);
                return true;
            }

            if (draggingHueBar) {
                updateHueFromPosition((int) my);
                return true;
            }

            if (draggingAlphaBar) {
                int alphaBarWidth = colorSquareSize + padding + hueBarWidth;
                updateAlphaFromPosition((int) mx, alphaBarWidth);
                return true;
            }
        }

        return false;
    }

    public boolean handleRelease(double mx, double my, int button) {
        if (editingColor == null) return false;

        if (button == 0) {
            dragging = false;
            draggingColorDot = false;
            draggingHueBar = false;
            draggingAlphaBar = false;
        }
        return false;
    }

    private void updateColorFromPosition(int mx, int my) {
        int colorSquareX = panelX + padding;
        int colorSquareY = panelY + headerHeight + padding;

        float newSaturation = Math.max(0f, Math.min(1f, (mx - colorSquareX) / (float) colorSquareSize));
        float newBrightness = Math.max(0f, Math.min(1f, 1f - ((my - colorSquareY) / (float) colorSquareSize)));

        this.saturation = newSaturation;
        this.brightness = newBrightness;

        updateColorValue();
        updateHexFromColor();
    }

    private void updateHueFromPosition(int my) {
        int hueBarY = panelY + headerHeight + padding;
        this.hue = Math.max(0f, Math.min(1f, (my - hueBarY) / (float) hueBarHeight));
        updateColorValue();
        updateHexFromColor();
    }

    private void updateAlphaFromPosition(int mx, int alphaBarWidth) {
        int alphaBarX = panelX + padding;
        this.alpha = Math.max(0f, Math.min(1f, (mx - alphaBarX) / (float) alphaBarWidth));
        updateColorValue();
        updateHexFromColor();
    }

    private void updateColorValue() {
        if (editingColor != null) {
            Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
            int alphaInt = (int) (alpha * 255);
            editingColor.setValue(new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt));
        }
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editingColor == null) return false;

        if (editingHex && hexEditor.isActive()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingHex = false;
                hexEditor.cancelEditing();
                updateHexFromColor();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                editingHex = false;
                hexEditor.stopEditing();
                updateColorFromHex();
                return true;
            }
            if (hexEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
                updateColorFromHex();
                return true;
            }
        }

        return false;
    }

    public boolean handleCharType(char chr, int modifiers) {
        if (editingColor == null || !editingHex || !hexEditor.isActive()) return false;

        char upperChr = Character.toUpperCase(chr);
        if ((upperChr >= '0' && upperChr <= '9') || (upperChr >= 'A' && upperChr <= 'F')) {
            String currentText = hexEditor.getText();
            if (currentText.length() < 8) {
                if (hexEditor.handleCharType(upperChr, modifiers)) {
                    updateColorFromHex();
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isOpen() {
        return editingColor != null;
    }

    private boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private int getCachedCornerRadius() {
        return 12;
    }

    public boolean handleScroll(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (editingColor == null) return false;

        int colorSquareX = panelX + padding;
        int colorSquareY = panelY + headerHeight + padding;
        int alphaBarY = colorSquareY + colorSquareSize + padding;
        int alphaBarX = colorSquareX;
        int alphaBarWidth = colorSquareSize + padding + hueBarWidth;

        if (isHovered(mx, my, panelX, panelY, panelWidth, panelHeight)) {
            if (isHovered(mx, my, alphaBarX, alphaBarY, alphaBarWidth, alphaBarHeight)) {
                alpha -= verticalAmount * 0.05f;
                alpha = Math.max(0f, Math.min(1f, alpha));
                updateColorValue();
                updateHexFromColor();
                return true;
            } else if (isHovered(mx, my, colorSquareX, colorSquareY, colorSquareSize, colorSquareSize)) {
                float newSaturation = Math.max(0f, Math.min(1f, saturation + (float) -verticalAmount * 0.05f));
                float newBrightness = Math.max(0f, Math.min(1f, brightness + (float) verticalAmount * 0.05f));
                saturation = newSaturation;
                brightness = newBrightness;
                updateColorValue();
                updateHexFromColor();
                return true;
            } else if (isHovered(mx, my, colorSquareX + colorSquareSize + padding, colorSquareY, hueBarWidth, hueBarHeight)) {
                hue += verticalAmount * 0.02f;
                hue = Math.max(0f, Math.min(1f, hue));
                updateColorValue();
                updateHexFromColor();
                return true;
            }
        }

        return false;
    }
}

