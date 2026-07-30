package com.elfmcys.ysm.format.schema.model.views;

import com.elfmcys.ysm.natives.sound.SoundFormat;
import com.elfmcys.ysm.natives.sound.SoundStream;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.file.AssetFileView;
import mixel.common.SoundOuterClass;
import mixel.asset.strings.StringDataOuterClass;
import mixel.manifest.asset.CommonOuterClass;
import com.elfmcys.ysm.task.TaskContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class CommonAssetView {
    private final AssetFileView view;
    private final HashMap<String, SoundOuterClass.Sound> sounds;
    private final int stringsBlobId;

    public CommonAssetView(CommonOuterClass.Common commonAsset, AssetFileView view) {
        this.view = view;

        this.sounds = new HashMap<>(commonAsset.hasSounds() ? commonAsset.getSounds().length() : 0);
        if (commonAsset.hasSounds()) {
            for (var sound : commonAsset.getSounds()) {
                this.sounds.put(sound.getName(), sound);
            }
        }

        this.stringsBlobId = commonAsset.hasStringsBlobId() ? commonAsset.getStringsBlobId() : 0;
    }

    public CompletableFuture<@Nullable SoundStream> openSoundStream(TaskContext ctx, ChunkDataSource source, String soundName) {
        var sound = sounds.get(soundName);
        if (sound == null) {
            return CompletableFuture.completedFuture(null);
        }
        return view.openStream(ctx, source, sound.getStreamId()).thenApply(stream -> {
            if (stream != null) {
                return new SoundStream(stream, SoundFormat.valueOf(sound.getEncoding()), sound.getSampleRate(), sound.getSamples(), sound.getChannels());
            }
            return null;
        });
    }

    public CompletableFuture<StringDataOuterClass.StringData> readStringData(TaskContext ctx, ChunkDataSource source) {
        return view.readProtoBlob(ctx, source, stringsBlobId, StringDataOuterClass.StringData::parseFrom);
    }
}
