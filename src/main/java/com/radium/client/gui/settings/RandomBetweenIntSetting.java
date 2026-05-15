package com.radium.client.gui.settings;

import com.radium.client.utils.RandomBetweenInt;

public class RandomBetweenIntSetting extends Setting<RandomBetweenInt> {
    private final int absoluteMin, absoluteMax;
    private final int sliderMin, sliderMax;
    private final boolean noSlider;

    public RandomBetweenIntSetting(String name, RandomBetweenInt defaultValue, int absoluteMin, int absoluteMax, int sliderMin, int sliderMax, boolean noSlider) {
        super(name, defaultValue);
        this.absoluteMin = absoluteMin;
        this.absoluteMax = absoluteMax;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.noSlider = noSlider;
        if (this.value == null) {
            this.value = new RandomBetweenInt(defaultValue.min, defaultValue.max);
        }
        if (this.defaultValue == null) {
            this.defaultValue = new RandomBetweenInt(defaultValue.min, defaultValue.max);
        }
    }

    public RandomBetweenIntSetting(String name, RandomBetweenInt defaultValue, int absoluteMin, int absoluteMax) {
        this(name, defaultValue, absoluteMin, absoluteMax, absoluteMin, absoluteMax, false);
    }

    public int getAbsoluteMin() {
        return absoluteMin;
    }

    public int getAbsoluteMax() {
        return absoluteMax;
    }

    public int getSliderMin() {
        return sliderMin;
    }

    public int getSliderMax() {
        return sliderMax;
    }

    public boolean isNoSlider() {
        return noSlider;
    }

    public int getRandomValue() {
        RandomBetweenInt val = getValue();
        if (val == null) {
            val = defaultValue;
        }
        return val.getRandom();
    }

    @Override
    public void setValue(RandomBetweenInt value) {
        if (value.min < absoluteMin || value.max > absoluteMax || value.min > value.max) {
            return;
        }
        super.setValue(value);
    }

    @Override
    public void reset() {
        this.value = new RandomBetweenInt(defaultValue.min, defaultValue.max);
    }
}

