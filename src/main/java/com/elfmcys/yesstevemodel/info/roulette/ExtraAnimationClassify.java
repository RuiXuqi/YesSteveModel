package com.elfmcys.yesstevemodel.info.roulette;

import com.elfmcys.yesstevemodel.util.FifoHashMap;

// Native Access
public class ExtraAnimationClassify {
    private final String id;
    private final FifoHashMap<String, String> extraAnimationMap;

    // Native Access
    public ExtraAnimationClassify(String id, FifoHashMap<String, String> extraAnimationMap) {
        this.id = id;
        this.extraAnimationMap = extraAnimationMap;
    }

    public String getId() {
        return id;
    }

    public FifoHashMap<String, String> getExtraAnimationMap() {
        return extraAnimationMap;
    }
}
