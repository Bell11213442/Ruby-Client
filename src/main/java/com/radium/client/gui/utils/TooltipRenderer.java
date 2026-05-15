package com.radium.client.gui.utils;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class TooltipRenderer {
    private final int tooltipDelay = 500;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer textRenderer = client.textRenderer;
    private Module hoveredModule = null;
    private long hoverStartTime = 0;
    private boolean showingTooltip = false;
    private int tooltipX = 0;
    private int tooltipY = 0;

    public void update(int mouseX, int mouseY) {
        long currentTime = System.currentTimeMillis();

        if (hoveredModule != null && !showingTooltip) {
            if (currentTime - hoverStartTime > tooltipDelay) {
                showingTooltip = true;
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
        }

        if (hoveredModule == null) {
            showingTooltip = false;
        }
    }

    public void setHoveredModule(Module module, int mouseX, int mouseY) {
        if (hoveredModule != module) {
            hoveredModule = module;
            hoverStartTime = System.currentTimeMillis();
            showingTooltip = false;
            tooltipX = mouseX;
            tooltipY = mouseY;
        }
    }

    public void clearHover() {
        hoveredModule = null;
        showingTooltip = false;
    }

    public void render(DrawContext context, float animationProgress) {
        if (!showingTooltip || hoveredModule == null) return;

        String description = hoveredModule.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available";
        }

        String moduleName = hoveredModule.getName();
        int moduleNameWidth = textRenderer.getWidth(moduleName);

        List<String> lines = wrapText(description, 250);
        int maxLineWidth = lines.stream().mapToInt(textRenderer::getWidth).max().orElse(0);

        int contentWidth = Math.max(moduleNameWidth, maxLineWidth);
        int tooltipWidth = Math.max(120, contentWidth + 16);

        if (tooltipWidth < 250) {
            lines = wrapText(description, tooltipWidth - 16);
            maxLineWidth = lines.stream().mapToInt(textRenderer::getWidth).max().orElse(0);
            tooltipWidth = Math.max(120, Math.max(moduleNameWidth, maxLineWidth) + 16);
        }

        int tooltipHeight = 14 + 4 + (lines.size() * 10) + 8;

        int finalTooltipX = tooltipX + 10;
        int finalTooltipY = tooltipY - tooltipHeight - 5;

        int screenWidth = client.getWindow().getWidth();
        int screenHeight = client.getWindow().getHeight();

        if (finalTooltipX + tooltipWidth > screenWidth) {
            finalTooltipX = tooltipX - tooltipWidth - 10;
        }
        if (finalTooltipY < 0) {
            finalTooltipY = tooltipY + 20;
        }
        if (finalTooltipX < 0) {
            finalTooltipX = 5;
        }

        long elapsed = System.currentTimeMillis() - (hoverStartTime + tooltipDelay);
        float tooltipAlpha = Math.min(1.0f, elapsed / 150.0f);

        if (tooltipAlpha <= 0) return;

        renderTooltip(context, finalTooltipX, finalTooltipY, tooltipWidth, tooltipHeight,
                moduleName, lines, tooltipAlpha, animationProgress);
    }

    private void renderTooltip(DrawContext context, int x, int y, int width, int height,
                               String moduleName, List<String> descriptionLines,
                               float tooltipAlpha, float animationProgress) {
        int cornerRadius = 12;

        int bgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsPanelColor(), tooltipAlpha * 0.95f);
        GuiUtils.drawRoundedRect(context, x, y, width, height, cornerRadius, bgColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), tooltipAlpha * 0.8f);
        drawRoundedRectOutline(context, x, y, width, height, cornerRadius, borderColor);

        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), (int) (tooltipAlpha * 255)) | 0xFF000000;
        context.drawText(textRenderer, moduleName, x + 8, y + 4, headerColor, false);

        int separatorY = y + 16;
        int separatorColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSeparatorColor(), tooltipAlpha);
        context.fill(x + 8, separatorY, x + width - 8, separatorY + 1, separatorColor);

        int textColor = RadiumGuiTheme.applyAlpha(0xFFCCCCCC, (int) (tooltipAlpha * 255)) | 0xFF000000;
        int textY = separatorY + 4;
        for (String line : descriptionLines) {
            context.drawText(textRenderer, line, x + 8, textY, textColor, false);
            textY += 10;
        }
    }

    private void drawRoundedRectOutline(DrawContext context, int x, int y, int width, int height,
                                        int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + 1, color);
            context.fill(x, y + height - 1, x + width, y + height, color);
            context.fill(x, y, x + 1, y + height, color);
            context.fill(x + width - 1, y, x + width, y + height, color);
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);

        context.fill(x + radius, y, x + width - radius, y + 1, color);
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + 1, y + height - radius, color);
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color);

        drawCornerOutline(context, x, y, radius, color, true, true);
        drawCornerOutline(context, x + width - radius, y, radius, color, false, true);
        drawCornerOutline(context, x, y + height - radius, radius, color, true, false);
        drawCornerOutline(context, x + width - radius, y + height - radius, radius, color, false, false);
    }

    private void drawCornerOutline(DrawContext context, int x, int y, int radius, int color,
                                   boolean left, boolean top) {
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                double distance = Math.sqrt((radius - i - 0.5) * (radius - i - 0.5) +
                        (radius - j - 0.5) * (radius - j - 0.5));
                if (distance >= radius - 1 && distance <= radius) {
                    int pixelX = left ? x + i : x + radius - i - 1;
                    int pixelY = top ? y + j : y + radius - j - 1;
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color);
                }
            }
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        if (text == null || text.trim().isEmpty()) {
            List<String> result = new ArrayList<>();
            result.add("No description available");
            return result;
        }

        int availableWidth = maxWidth - 16;
        if (availableWidth <= 0) availableWidth = 100;

        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int testWidth = textRenderer.getWidth(testLine);

            if (testWidth <= availableWidth || currentLine.length() == 0) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);

                if (textRenderer.getWidth(word) > availableWidth) {
                    String truncated = textRenderer.trimToWidth(word, availableWidth - textRenderer.getWidth("...")) + "...";
                    currentLine = new StringBuilder(truncated);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.isEmpty() ? List.of("No description available") : lines;
    }

    public boolean isShowingTooltip() {
        return showingTooltip;
    }
}

