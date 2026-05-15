package com.radium.client.gui.settings;
// radium client

public class ModeSetting<T extends Enum<T>> extends Setting<T> {
    private final Class<T> enumClass;
    private String description;

    public ModeSetting(String name, T defaultValue, Class<T> enumClass) {
        super(name, defaultValue);
        this.enumClass = enumClass;
    }

    public String getDescription() {
        return description;
    }

    public ModeSetting<T> setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isMode(T mode) {
        return getValue() == mode;
    }

    public T[] getModes() {
        return enumClass.getEnumConstants();
    }

    public void cycleMode() {
        T[] modes = getModes();
        T current = getValue();
        int currentIndex = -1;

        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == current) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            int nextIndex = (currentIndex + 1) % modes.length;
            setValue(modes[nextIndex]);
        }
    }

    public void setValue(String modeName) {
        if (modeName == null) return;


        try {
            T mode = Enum.valueOf(enumClass, modeName.toUpperCase());
            setValue(mode);
            return;
        } catch (Exception ignored) {
        }


        for (T constant : enumClass.getEnumConstants()) {
            if (constant.toString().equalsIgnoreCase(modeName)) {
                setValue(constant);
                return;
            }
        }
    }


    @Override
    public String toString() {
        return getValue().name();
    }
}
