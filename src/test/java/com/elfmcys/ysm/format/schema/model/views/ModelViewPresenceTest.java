package com.elfmcys.ysm.format.schema.model.views;

import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.info.InfoOuterClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModelViewPresenceTest {
    @Test
    void treatsMissingInfoCollectionsAsEmptyWithoutMutatingMessage() {
        var info = InfoOuterClass.Info.newInstance();

        var view = new ModelInfoView(info, null);

        assertEquals("fallback", view.translateOr("missing", "en_us", "fallback"));
        assertFalse(info.hasLanguageFiles());
        assertFalse(info.hasMetadata());
    }

    @Test
    void treatsMissingRenderTargetCollectionsAndKindAsProtoDefaults() {
        var descriptor = RenderTargetOuterClass.RenderTarget.newInstance();

        var view = new RenderTargetView(null, descriptor);

        assertEquals("", view.id());
        assertEquals(RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_UNSPECIFIED, view.kind());
        assertEquals(0, view.matches().size());
        assertEquals(0, view.getTextureNames().size());
        assertFalse(descriptor.hasMatch());
        assertFalse(descriptor.hasTextures());
        assertFalse(descriptor.hasKind());
    }
}
