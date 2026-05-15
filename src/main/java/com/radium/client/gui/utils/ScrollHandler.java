package com.radium.client.gui.utils;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import net.minecraft.client.gui.DrawContext;

public class ScrollHandler {
    private final float scrollSmoothing = 0.2f;
    private float scrollOffset = 0;
    private float maxScroll = 0;
    private boolean isDragging = false;
    private float targetScroll = 0;
    private float currentScroll = 0;

    public void setMaxScroll(float maxScroll) {
        this.maxScroll = Math.max(0, maxScroll);
        if (scrollOffset > this.maxScroll) {
            scrollOffset = this.maxScroll;
            targetScroll = this.maxScroll;
        }
    }

    public void scroll(double amount) {
        targetScroll -= amount * 10;
        targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
    }

    public void update() {
        if (Math.abs(targetScroll - currentScroll) > 0.1f) {
            currentScroll += (targetScroll - currentScroll) * scrollSmoothing;
        } else {
            currentScroll = targetScroll;
        }
        scrollOffset = currentScroll;
    }

    public boolean handleScroll(double mouseX, double mouseY, double hAmount, double vAmount,
                                int panelX, int panelY, int panelWidth) {
        if (GuiUtils.isHovered(mouseX, mouseY, panelX, panelY, panelWidth, 200)) {
            if (maxScroll > 0) {
                scroll(vAmount);
                return true;
            }
        }
        return false;
    }

    public boolean handleScrollbarDrag(double mouseX, double mouseY, int panelX, int panelY,
                                       int panelWidth, int panelHeight) {
        if (maxScroll <= 0) return false;

        int scrollBarX = panelX + panelWidth - 6;
        int scrollBarY = panelY;

        if (GuiUtils.isHovered(mouseX, mouseY, scrollBarX, scrollBarY, 6, panelHeight)) {
            float scrollRatio = (float) (mouseY - scrollBarY) / panelHeight;
            targetScroll = scrollRatio * maxScroll;
            targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
            isDragging = true;
            return true;
        }

        return false;
    }

    public void renderScrollbar(DrawContext context, int panelX, int panelY, int panelWidth,
                                float animationProgress) {
        if (maxScroll <= 0) return;

        int scrollBarX = panelX + panelWidth - 6;
        int scrollBarY = panelY + 18;
        int scrollBarHeight = 150;
        int scrollBarWidth = 4;

        GuiUtils.drawRoundedRect(context, scrollBarX, scrollBarY, scrollBarWidth,
                scrollBarHeight, 2, RadiumGuiTheme.applyAlpha(0x50000000, animationProgress));

        float scrollProgress = scrollOffset / maxScroll;
        int handleHeight = Math.max(20, (int) (0.3f * scrollBarHeight));
        int handleY = scrollBarY + (int) ((scrollBarHeight - handleHeight) * scrollProgress);

        GuiUtils.drawRoundedRect(context, scrollBarX, handleY, scrollBarWidth,
                handleHeight, 2, RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress));
    }

    public void stopDrag() {
        isDragging = false;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public int getScrollOffset() {
        return -(int) scrollOffset;
    }

    public void reset() {
        scrollOffset = 0;
        targetScroll = 0;
        currentScroll = 0;
        maxScroll = 0;
    }
}

