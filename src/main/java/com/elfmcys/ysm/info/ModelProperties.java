package com.elfmcys.ysm.info;

import com.elfmcys.ysm.info.roulette.ExtraAnimationButton;
import com.elfmcys.ysm.info.roulette.ExtraAnimationClassify;
import com.elfmcys.ysm.util.FifoHashMap;
import com.google.common.collect.Maps;

import java.util.Map;

// Native Access
public class ModelProperties {
    private final float widthScale;
    private final float heightScale;
    private final String defaultTexture;
    private final String previewAnimation;
    private final FifoHashMap<String, String> extraAnimationOrderMap;
    private final Map<String, ExtraAnimationButton> extraAnimationButtonsMap;
    private final Map<String, FifoHashMap<String, String>> extraAnimationClassifyMap;
    private final boolean free;
    private final boolean renderLayersFirst;
    private final boolean disablePreviewRotation;

    // Native Access
    public ModelProperties(float widthScale, float heightScale, String defaultTexture, String previewAnimation,
                           FifoHashMap<String, String> extraAnimationOrderMap,
                           ExtraAnimationButton[] extraAnimationButtonsList,
                           ExtraAnimationClassify[] extraAnimationClassifyList,
                           boolean free, boolean renderLayersFirst, boolean disablePreviewRotation) {
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        this.defaultTexture = defaultTexture;
        this.previewAnimation = previewAnimation;
        this.extraAnimationOrderMap = extraAnimationOrderMap;
        this.extraAnimationButtonsMap = transformExtraAnimationButton(extraAnimationButtonsList);
        this.extraAnimationClassifyMap = transformExtraAnimationClassify(extraAnimationClassifyList);
        this.free = free;
        this.renderLayersFirst = renderLayersFirst;
        this.disablePreviewRotation = disablePreviewRotation;
    }

    private static Map<String, ExtraAnimationButton> transformExtraAnimationButton(ExtraAnimationButton[] extraAnimationButtonsList) {
        Map<String, ExtraAnimationButton> output = Maps.newHashMap();
        for (ExtraAnimationButton button : extraAnimationButtonsList) {
            output.put(button.getId(), button);
        }
        return output;
    }

    private static Map<String, FifoHashMap<String, String>> transformExtraAnimationClassify(ExtraAnimationClassify[] extraAnimationClassifies) {
        Map<String, FifoHashMap<String, String>> output = Maps.newHashMap();
        for (ExtraAnimationClassify classify : extraAnimationClassifies) {
            output.put(classify.getId(), classify.getExtraAnimationMap());
        }
        return output;
    }

    public float widthScale() {
        return widthScale;
    }

    public float heightScale() {
        return heightScale;
    }

    // 清单的原始值可能有错，应该用 modelContainer.playerModel().defaultTextureName()
    public String defaultTexture() {
        return defaultTexture;
    }

    public String previewAnimation() {
        return previewAnimation;
    }

    public FifoHashMap<String, String> extraAnimationOrderMap() {
        return extraAnimationOrderMap;
    }

    public Map<String, ExtraAnimationButton> extraAnimationButtonsMap() {
        return extraAnimationButtonsMap;
    }

    public Map<String, FifoHashMap<String, String>> extraAnimationClassifyMap() {
        return extraAnimationClassifyMap;
    }

    public boolean free() {
        return free;
    }

    public boolean renderLayersFirst() {
        return renderLayersFirst;
    }

    public boolean disablePreviewRotation() {
        return disablePreviewRotation;
    }
}
