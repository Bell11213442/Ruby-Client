package com.radium.client.gui.settings;
// radium client

public class SliderSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double increment;
    private final int decimalPlaces;

    public SliderSetting(String name, double defaultValue, double min, double max, double increment) {
        this(name, defaultValue, min, max, increment, 1);
    }

    public SliderSetting(String name, double defaultValue, double min, double max, double increment, int decimalPlaces) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.decimalPlaces = Math.max(0, decimalPlaces);
    }

    @Override
    public void setValue(Double value) {
        double clampedValue = Math.max(min, Math.min(max, value));

        double multiplier = Math.pow(10, decimalPlaces);
        clampedValue = Math.round(clampedValue * multiplier) / multiplier;

        this.value = clampedValue;
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

    public int getDecimalPlaces() {
        return decimalPlaces;
    }


    public void setValueFromSliderPosition(double position) {
        position = Math.max(0.0, Math.min(1.0, position));
        double newValue = min + (max - min) * position;
        setValue(newValue);
    }


    public double getSliderPosition() {
        if (max == min) return 0.0;
        return (getValue() - min) / (max - min);
    }


    public double getPercentage() {
        return getSliderPosition() * 100.0;
    }


    public void setValueFromPercentage(double percentage) {
        setValueFromSliderPosition(percentage / 100.0);
    }

    public void increase() {
        setValue(getValue() + increment);
    }

    public void decrease() {
        setValue(getValue() - increment);
    }


    public String getFormattedValue() {
        if (decimalPlaces == 0) {
            return String.valueOf(getValue().intValue());
        } else {
            return String.format("%." + decimalPlaces + "f", getValue());
        }
    }


    public boolean isSlider() {
        return true;
    }
}
