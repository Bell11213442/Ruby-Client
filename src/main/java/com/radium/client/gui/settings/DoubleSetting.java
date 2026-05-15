package com.radium.client.gui.settings;
// radium client

public class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;

    public DoubleSetting(String name, double defaultValue, double min, double max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        setValue(defaultValue);
    }

    @Override
    public void setValue(Double value) {
        if (value == null) return;
        this.value = Math.max(min, Math.min(max, value));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public void increase(double amount) {
        setValue(getValue() + amount);
    }

    public void decrease(double amount) {
        setValue(getValue() - amount);
    }
}

