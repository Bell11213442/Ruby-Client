package com.radium.client.gui.utils;
// radium client

import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.SliderSetting;

public class InteractionHandler {
    private boolean draggingSlider = false;
    private SliderSetting draggedSlider = null;
    private boolean draggingNumber = false;
    private NumberSetting draggedNumber = null;

    public boolean handleSliderDrag(SliderSetting setting, double mouseX, double mouseY,
                                    int x, int y, int width, int height) {
        if (draggingSlider && draggedSlider == setting) {
            if (GuiUtils.isHovered(mouseX, mouseY, x, y, width, height)) {
                double newPosition = Math.max(0.0, Math.min(1.0, (mouseX - x) / width));
                setting.setValueFromSliderPosition(newPosition);
                return true;
            }
        }
        return false;
    }

    public boolean handleNumberDrag(NumberSetting setting, double mouseX, double mouseY,
                                    int x, int y, int width, int height) {
        if (draggingNumber && draggedNumber == setting) {
            if (GuiUtils.isHovered(mouseX, mouseY, x, y, width, height)) {
                double newPosition = Math.max(0.0, Math.min(1.0, (mouseX - x) / width));
                double newValue = setting.getMin() + (setting.getMax() - setting.getMin()) * newPosition;
                setting.setValue(newValue);
                return true;
            }
        }
        return false;
    }

    public void startSliderDrag(SliderSetting setting) {
        draggingSlider = true;
        draggedSlider = setting;
    }

    public void startNumberDrag(NumberSetting setting) {
        draggingNumber = true;
        draggedNumber = setting;
    }

    public void stopDragging() {
        draggingSlider = false;
        draggedSlider = null;
        draggingNumber = false;
        draggedNumber = null;
    }

    public boolean isDraggingSlider() {
        return draggingSlider;
    }

    public boolean isDraggingNumber() {
        return draggingNumber;
    }

    public boolean isDragging() {
        return draggingSlider || draggingNumber;
    }
}

