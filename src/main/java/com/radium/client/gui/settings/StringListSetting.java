package com.radium.client.gui.settings;
// radium client

import java.util.ArrayList;
import java.util.List;

public class StringListSetting extends Setting<List<String>> {

    public StringListSetting(String name) {
        super(name, new ArrayList<>());
    }

    public StringListSetting(String name, List<String> defaultValue) {
        super(name, new ArrayList<>(defaultValue));
    }

    public List<String> getList() {
        return getValue();
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public void add(String item) {
        if (item != null && !item.trim().isEmpty() && !getValue().contains(item)) {
            getValue().add(item);
        }
    }

    public void remove(String item) {
        getValue().remove(item);
    }

    public void remove(int index) {
        if (index >= 0 && index < getValue().size()) {
            getValue().remove(index);
        }
    }

    public void clear() {
        getValue().clear();
    }

    public boolean contains(String item) {
        return getValue().contains(item);
    }

    public int size() {
        return getValue().size();
    }

    public String get(int index) {
        if (index >= 0 && index < getValue().size()) {
            return getValue().get(index);
        }
        return null;
    }

    public void set(int index, String value) {
        if (index >= 0 && index < getValue().size() && value != null && !value.trim().isEmpty()) {
            getValue().set(index, value);
        }
    }
}


