package com.elfmcys.yesstevemodel.info;

import com.elfmcys.yesstevemodel.util.FifoHashMap;

// Native Access
public class ModelProperties {
    private final float widthScale;
    private final float heightScale;
    private final String defaultTexture;
    private final String previewAnimation;
    private final FifoHashMap<String, String> extraAnimationOrderMap;
    private final boolean free;

    // Native Access
    public ModelProperties(float widthScale, float heightScale, String defaultTexture, String previewAnimation, FifoHashMap<String, String> extraAnimationOrderMap, boolean free) {
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        this.defaultTexture = defaultTexture;
        this.previewAnimation = previewAnimation;
        this.extraAnimationOrderMap = extraAnimationOrderMap;
        this.free = free;
    }

    public float widthScale() {
        return widthScale;
    }

    public float heightScale() {
        return heightScale;
    }

    public String defaultTexture() {
        return defaultTexture;
    }

    public String previewAnimation() {
        return previewAnimation;
    }

    public FifoHashMap<String, String> extraAnimationOrderMap() {
        return extraAnimationOrderMap;
    }

    public boolean free() {
        return free;
    }
}
