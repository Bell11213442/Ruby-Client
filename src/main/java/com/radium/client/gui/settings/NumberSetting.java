package com.radium.client.gui.settings;
// radium client

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    @Override
    public void setValue(Double value) {
        super.setValue(Math.max(min, Math.min(max, value)));
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

    public void increase() {
        setValue(getValue() + increment);
    }

    public void decrease() {
        setValue(getValue() - increment);
    }
}
