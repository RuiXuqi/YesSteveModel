package com.elfmcys.ysm.info.roulette;

import com.elfmcys.ysm.util.FifoHashMap;

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
