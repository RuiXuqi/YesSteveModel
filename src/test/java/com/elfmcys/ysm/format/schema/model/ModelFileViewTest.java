package com.elfmcys.ysm.format.schema.model;

import mixel.manifest.ManifestOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.info.InfoOuterClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelFileViewTest {
    @Test
    void acceptsCanonicalPlayerRenderTarget() {
        assertDoesNotThrow(() -> ModelFileView.validatePlayerRenderTarget(manifest(
                "player", RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER)));
    }

    @Test
    void rejectsMissingOrMisidentifiedPlayerRenderTarget() {
        assertThrows(IOException.class, () -> ModelFileView.validatePlayerRenderTarget(
                ManifestOuterClass.Manifest.newInstance()));
        assertThrows(IOException.class, () -> ModelFileView.validatePlayerRenderTarget(manifest(
                "player", RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PROJECTILE)));
        assertThrows(IOException.class, () -> ModelFileView.validatePlayerRenderTarget(manifest(
                "legacy-player", RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER)));
    }

    @Test
    void validatesThumbnailSourceAgainstChunkPresence() {
        assertDoesNotThrow(() -> ModelFileView.validateThumbnailSource(
                InfoOuterClass.PreviewSource.PREVIEW_SOURCE_UNSPECIFIED, false));
        assertDoesNotThrow(() -> ModelFileView.validateThumbnailSource(
                InfoOuterClass.PreviewSource.PREVIEW_SOURCE_RAW, true));
        assertDoesNotThrow(() -> ModelFileView.validateThumbnailSource(
                InfoOuterClass.PreviewSource.PREVIEW_SOURCE_GENERATED, true));

        assertThrows(IOException.class, () -> ModelFileView.validateThumbnailSource(
                InfoOuterClass.PreviewSource.PREVIEW_SOURCE_UNSPECIFIED, true));
        assertThrows(IOException.class, () -> ModelFileView.validateThumbnailSource(
                InfoOuterClass.PreviewSource.PREVIEW_SOURCE_RAW, false));
        assertThrows(IOException.class, () -> ModelFileView.validateThumbnailSource(
                InfoOuterClass.PreviewSource.PREVIEW_SOURCE_GENERATED, false));
        assertThrows(IOException.class, () -> ModelFileView.validateThumbnailSource(null, false));
    }

    @Test
    void defaultsMissingThumbnailSourceWithoutMutatingInfo() {
        var info = InfoOuterClass.Info.newInstance();

        assertEquals(InfoOuterClass.PreviewSource.PREVIEW_SOURCE_UNSPECIFIED,
                ModelFileView.thumbnailSource(info));
        assertFalse(info.hasThumbnailSource());
    }

    private static ManifestOuterClass.Manifest manifest(
            String targetId, RenderTargetOuterClass.RenderTargetKind kind) {
        return ManifestOuterClass.Manifest.newInstance().addRenderTargets(
                RenderTargetOuterClass.RenderTarget.newInstance().setTargetId(targetId).setKind(kind));
    }
}
