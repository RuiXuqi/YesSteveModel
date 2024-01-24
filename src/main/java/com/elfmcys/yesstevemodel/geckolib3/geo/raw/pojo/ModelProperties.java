package com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// Native Access
public class ModelProperties {
    private final String identifier;
    private final double textureHeight;
    private final double textureWidth;
    private final double visibleBoundsHeight;
    private final double visibleBoundsWidth;
    private final double[] visibleBoundsOffset;
    private final double heightScale;
    private final double widthScale;
    @Nullable
    private final ExtraInfo extraInfo;
    private final ModelScript scripts;

    // Native Access
    public ModelProperties(String identifier, double textureHeight, double textureWidth, double visibleBoundsHeight, double visibleBoundsWidth, double[] visibleBoundsOffset, double heightScale, double widthScale, ExtraInfo extraInfo, ModelScript scripts) {
        this.identifier = identifier;
        this.textureHeight = textureHeight;
        this.textureWidth = textureWidth;
        this.visibleBoundsHeight = visibleBoundsHeight;
        this.visibleBoundsWidth = visibleBoundsWidth;
        this.visibleBoundsOffset = visibleBoundsOffset;
        this.heightScale = heightScale;
        this.widthScale = widthScale;
        this.extraInfo = extraInfo;
        this.scripts = scripts;
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

    public double getHeightScale() {
        return heightScale;
    }

    public double getWidthScale() {
        return widthScale;
    }

    @Nullable
    public ExtraInfo getExtraInfo() {
        return extraInfo;
    }

    @Nonnull
    public ModelScript scripts() {
        return scripts;
    }
}
