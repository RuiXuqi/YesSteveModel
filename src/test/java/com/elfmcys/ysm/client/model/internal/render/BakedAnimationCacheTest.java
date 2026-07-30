package com.elfmcys.ysm.client.model.internal.render;

import mixel.asset.model.ModelDataOuterClass;
import mixel.asset.model.data.AnimationOuterClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BakedAnimationCacheTest {
    @Test
    void allowsSameAnimationNameInDifferentAnimationSets() {
        var main = animationFile("main");
        var firstPerson = animationFile("fp_arm");

        assertEquals("parallel0", assertDoesNotThrow(() -> BakedAnimationCache.flatten(List.of(main)))
                .get(0).getName());
        assertEquals("parallel0", assertDoesNotThrow(() -> BakedAnimationCache.flatten(List.of(firstPerson)))
                .get(0).getName());
    }

    @Test
    void rejectsDuplicateAnimationNameWithinOneAnimationSet() {
        assertThrows(IOException.class, () -> BakedAnimationCache.flatten(List.of(
                animationFile("main"), animationFile("extra"))));
    }

    @Test
    void usesUnstableBakeCacheSuffixes() {
        assertEquals(".geo.ysm-cache", BakedModelCache.CACHE_SUFFIX);
        assertEquals(".anim.ysm-cache", BakedAnimationCache.CACHE_SUFFIX);
    }

    private static ModelDataOuterClass.ModelData.AnimationFilesEntry animationFile(String key) {
        var file = AnimationOuterClass.AnimationFile.newInstance()
                .addAnimations(AnimationOuterClass.Animation.newInstance().setName("parallel0"));
        return ModelDataOuterClass.ModelData.AnimationFilesEntry.newInstance()
                .setKey(key).setValue(file);
    }
}
