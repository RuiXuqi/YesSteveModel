package com.elfmcys.yesstevemodel.info.stats;

// Native Access
public class TextureStats {
    private final int width;
    private final int height;

    // Native Access
    public TextureStats(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
