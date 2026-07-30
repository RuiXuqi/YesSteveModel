package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.AnimationStore;
import com.elfmcys.ysm.model.catalog.BuiltinModelIndex;
import mixel.asset.model.data.AnimationOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Published only after a default target has fully bound its animations. */
public final class DefaultAnimationRuntime {
    private final Map<String, AnimationStore> stores = new ConcurrentHashMap<>();
    private final CurrentAnimationValidator validator;

    public DefaultAnimationRuntime(BuiltinModelIndex contract) {
        this.validator = contract::requireCurrent;
    }

    DefaultAnimationRuntime() {
        validator = (target, animationSet, animation) -> { };
    }

    public void publish(String domain, AnimationStore store) {
        var previous = stores.putIfAbsent(domain, store);
        if (previous != null && previous != store
                && !previous.keySet().equals(store.keySet())) {
            throw new IllegalStateException("Conflicting default animation domain: " + domain);
        }
    }

    public AnimationStore fallback(String domain) {
        return stores.get(domain);
    }

    public void requireCurrent(RenderTargetOuterClass.RenderTarget target,
                               String animationSet,
                               AnimationOuterClass.Animation animation) throws IOException {
        validator.requireCurrent(target, animationSet, animation);
    }

    public void clear() {
        stores.clear();
    }

    @FunctionalInterface
    private interface CurrentAnimationValidator {
        void requireCurrent(RenderTargetOuterClass.RenderTarget target,
                            String animationSet,
                            AnimationOuterClass.Animation animation) throws IOException;
    }
}
