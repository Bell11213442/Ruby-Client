package com.radium.client.gui;
// radium client

import com.radium.client.modules.misc.Compass;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CompassGuiEditor extends Screen {
    private final Compass compass;
    private int tempX;
    private int tempY;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private final int gridSize = 20;

    public CompassGuiEditor(Compass compass) {
        super(Text.literal("Compass HUD Editor"));
        this.compass = compass;
        this.tempX = compass.getX().getValue().intValue();
        this.tempY = compass.getY().getValue().intValue();
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int buttonHeight = 20;
        int y = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            compass.getX().setValue((double) tempX);
            compass.getY().setValue((double) tempY);
            this.client.setScreen(null);
        }).dimensions(this.width / 2 - buttonWidth - 10, y, buttonWidth, buttonHeight).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
            this.client.setScreen(null);
        }).dimensions(this.width / 2 + 10, y, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int bg = 0x22000000;
        context.fill(0, 0, width, height, bg);
        int gridColor = 0x33000000;
        for (int gx = 0; gx < width; gx += gridSize) {
            RenderUtils.drawVerLine(context, gx, 0, height, gridColor);
        }
        for (int gy = 0; gy < height; gy += gridSize) {
            RenderUtils.drawHorLine(context, 0, gy, width, gridColor);
        }
        super.render(context, mouseX, mouseY, delta);
        compass.renderAt(context, tempX, tempY);
        int s = compass.getSize().getValue().intValue();
        RenderUtils.drawCircle(context, tempX + s / 2, tempY + s / 2, s / 2, 0x88FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            int size = compass.getSize().getValue().intValue();
            int cx = tempX + size / 2;
            int cy = tempY + size / 2;
            int dx = mx - cx;
            int dy = my - cy;
            if (dx * dx + dy * dy <= (size / 2) * (size / 2)) {
                dragging = true;
                dragOffsetX = mx - tempX;
                dragOffsetY = my - tempY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            int newX = mx - dragOffsetX;
            int newY = my - dragOffsetY;
            newX = (newX / gridSize) * gridSize;
            newY = (newY / gridSize) * gridSize;
            tempX = newX;
            tempY = newY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}

