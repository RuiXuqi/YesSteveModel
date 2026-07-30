package com.elfmcys.ysm.geckolib3.geo.raw.pojo;

// Native Access
public class GeoModelProperties {
    private final String identifier;
    private final double textureHeight;
    private final double textureWidth;
    private final double visibleBoundsHeight;
    private final double visibleBoundsWidth;
    private final double[] visibleBoundsOffset;

    // Native Access
    public GeoModelProperties(String identifier, double textureHeight, double textureWidth, double visibleBoundsHeight, double visibleBoundsWidth, double[] visibleBoundsOffset) {
        this.identifier = identifier;
        this.textureHeight = textureHeight;
        this.textureWidth = textureWidth;
        this.visibleBoundsHeight = visibleBoundsHeight;
        this.visibleBoundsWidth = visibleBoundsWidth;
        this.visibleBoundsOffset = visibleBoundsOffset;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Double getTextureHeight() {
        return textureHeight;
    }

    public Double getTextureWidth() {
        return textureWidth;
    }

    public Double getVisibleBoundsHeight() {
        return visibleBoundsHeight;
    }

    public Double getVisibleBoundsWidth() {
        return visibleBoundsWidth;
    }

    public double[] getVisibleBoundsOffset() {
        return visibleBoundsOffset;
    }
}
