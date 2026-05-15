package com.radium.client.gui.settings;
// radium client

import java.util.function.BiConsumer;

public abstract class Setting<T> {
    protected String name;
    protected T value;
    protected T defaultValue;
    protected BiConsumer<T, T> callback;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        T old = this.value;
        this.value = value;
        if (callback != null) {
            callback.accept(old, value);
        }
    }

    public void setCallback(BiConsumer<T, T> callback) {
        this.callback = callback;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void reset() {
        setValue(this.defaultValue);
    }
}
