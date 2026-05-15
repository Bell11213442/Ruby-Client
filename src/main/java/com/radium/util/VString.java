package com.radium.util;
// radium client

public record VString(String string) {

    @Override
    public String string() {
        return string != null ? string : "";
    }

    public int codePointAt(int index) {
        if (index < 0 || index >= string.length()) {
            return -1;
        }
        return Character.codePointAt(string, index);
    }

    @Override
    public String toString() {
        return string();
    }
}

