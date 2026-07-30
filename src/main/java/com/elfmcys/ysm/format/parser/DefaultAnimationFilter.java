package com.elfmcys.ysm.format.parser;

import mixel.asset.model.data.AnimationOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;

import java.io.IOException;

/** Immutable conversion rule for removing animations supplied by the builtin contract. */
@FunctionalInterface
public interface DefaultAnimationFilter {
    AnimationOuterClass.AnimationFile apply(
            RenderTargetOuterClass.RenderTarget target,
            String animationSet,
            AnimationOuterClass.AnimationFile source) throws IOException;

    static DefaultAnimationFilter keepAll() {
        return (target, animationSet, source) -> source;
    }
}
