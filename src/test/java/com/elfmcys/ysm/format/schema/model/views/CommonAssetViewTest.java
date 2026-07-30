package com.elfmcys.ysm.format.schema.model.views;

import com.elfmcys.ysm.format.schema.file.AssetFileView;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import mixel.common.SoundOuterClass;
import mixel.manifest.asset.CommonOuterClass;
import com.elfmcys.ysm.task.TaskContext;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonAssetViewTest {
    @Test
    void treatsMissingSoundsAsEmptyWithoutMutatingMessage() {
        var common = CommonOuterClass.Common.newInstance();

        var view = new CommonAssetView(common, new RecordingAssetFileView());

        assertFalse(common.hasSounds());
        assertNull(view.openSoundStream(null, null, "missing").join());
    }

    @Test
    void preservesExplicitEmptySoundsWithoutMutatingMessage() {
        var common = CommonOuterClass.Common.newInstance();
        common.getMutableSounds();

        new CommonAssetView(common, new RecordingAssetFileView());

        assertTrue(common.hasSounds());
        assertEquals(0, common.getSounds().length());
    }

    @Test
    void indexesAllPresentSounds() {
        var common = CommonOuterClass.Common.newInstance()
                .addSounds(SoundOuterClass.Sound.newInstance().setName("first").setStreamId(11))
                .addSounds(SoundOuterClass.Sound.newInstance().setName("second").setStreamId(22));
        var fileView = new RecordingAssetFileView();
        var view = new CommonAssetView(common, fileView);

        assertNull(view.openSoundStream(null, null, "second").join());
        assertEquals(22, fileView.lastStreamId);
    }

    private static final class RecordingAssetFileView extends AssetFileView {
        private int lastStreamId = -1;

        private RecordingAssetFileView() {
            super(null);
        }

        @Override
        public CompletableFuture<InputStream> openStream(
                TaskContext ctx, ChunkDataSource source, int id) {
            lastStreamId = id;
            return CompletableFuture.completedFuture(null);
        }
    }
}
