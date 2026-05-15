package com.radium.client.gui.settings;

import com.radium.client.utils.RandomBetweenDouble;

public class RandomBetweenDoubleSetting extends Setting<RandomBetweenDouble> {
    private final double absoluteMin, absoluteMax;
    private final double sliderMin, sliderMax;
    private final boolean noSlider;

    public RandomBetweenDoubleSetting(String name, RandomBetweenDouble defaultValue, double absoluteMin, double absoluteMax, double sliderMin, double sliderMax, boolean noSlider) {
        super(name, defaultValue);
        this.absoluteMin = absoluteMin;
        this.absoluteMax = absoluteMax;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.noSlider = noSlider;
    }

    public RandomBetweenDoubleSetting(String name, RandomBetweenDouble defaultValue, double absoluteMin, double absoluteMax) {
        this(name, defaultValue, absoluteMin, absoluteMax, absoluteMin, absoluteMax, false);
    }

    public double getAbsoluteMin() {
        return absoluteMin;
    }

    public double getAbsoluteMax() {
        return absoluteMax;
    }

    public double getSliderMin() {
        return sliderMin;
    }

    public double getSliderMax() {
        return sliderMax;
    }

    public boolean isNoSlider() {
        return noSlider;
    }

    public double getRandomValue() {
        return getValue().getRandom();
    }

    @Override
    public void setValue(RandomBetweenDouble value) {
        if (value.min < absoluteMin || value.max > absoluteMax || value.min > value.max) {
            return;
        }
        super.setValue(value);
    }

    @Override
    public void reset() {
        this.value = new RandomBetweenDouble(defaultValue.min, defaultValue.max);
    }
}

