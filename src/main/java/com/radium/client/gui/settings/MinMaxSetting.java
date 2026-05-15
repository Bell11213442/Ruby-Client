package com.radium.client.gui.settings;
// radium client

import java.util.Random;

public class MinMaxSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double increment;
    private final Random random = new Random();
    private double minValue;
    private double maxValue;

    public MinMaxSetting(String name, double min, double max, double increment, double defaultMin, double defaultMax) {
        super(name, defaultMin);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.minValue = defaultMin;
        this.maxValue = defaultMax;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double value) {
        this.minValue = Math.max(min, Math.min(max, Math.min(value, maxValue)));
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double value) {
        this.maxValue = Math.max(min, Math.max(minValue, Math.min(max, value)));
    }

    public int getRandomValueInt() {
        if (minValue >= maxValue) {
            return (int) minValue;
        }
        return (int) (minValue + random.nextDouble() * (maxValue - minValue));
    }

    public double getRandomValueDouble() {
        if (minValue >= maxValue) {
            return minValue;
        }
        return (minValue + random.nextDouble() * (maxValue - minValue));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }

    @Override
    public void setValue(Double value) {
        setMinValue(value);
    }
}


