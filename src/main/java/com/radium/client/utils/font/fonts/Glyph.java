package com.radium.client.utils.font.fonts;

import java.util.Objects;

record Glyph(int u, int v, int width, int height, char value, GlyphMap owner) {

    @Override
    public String toString() {
        return "Glyph[" +
                "u=" + u + ", " +
                "v=" + v + ", " +
                "width=" + width + ", " +
                "height=" + height + ", " +
                "value=" + value + ", " +
                "owner=" + owner + ']';
    }

}
