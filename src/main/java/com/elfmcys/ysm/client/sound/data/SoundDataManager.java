package com.elfmcys.ysm.client.sound.data;

import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.sound.stream.CustomAudioStream;
import com.elfmcys.ysm.client.sound.stream.OpusAudioStream;
import com.elfmcys.ysm.client.sound.stream.PcmAudioStream;
import com.elfmcys.ysm.client.sound.stream.VorbisAudioStream;
import com.elfmcys.ysm.util.CleanerUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SoundDataManager {
    private static final IdentityHashMap<ModelRenderTarget, WeakReference<ModelSoundHolderImpl>> HOLDER_MAP = new IdentityHashMap<>();
    private static final Object PLACE_HOLDER = new Object();

    public static ModelSoundHolder register(ModelRenderTarget model) {
        RenderSystem.assertOnRenderThread();
        var ref = HOLDER_MAP.get(model);
        if (ref != null) {
            var holder = ref.get();
            if (holder != null) {
                return holder;
            }
        }
        var holder = new ModelSoundHolderImpl();
        CleanerUtil.ref(holder, model, m -> {
            Minecraft.getInstance().execute(() -> {
                HOLDER_MAP.remove(m);
            });
        });
        HOLDER_MAP.put(model, new WeakReference<>(holder));
        return holder;
    }

    static class ModelSoundHolderImpl implements ModelSoundHolder {
        private final ConcurrentHashMap<SoundData, CacheEntry> cacheMap = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<SoundData, Object> buildingCacheMap = new ConcurrentHashMap<>();

        void submitPcmCache(SoundData soundData, ByteBuffer pcmBuffer, IntArrayList segments) {
            cacheMap.put(soundData, new CacheEntry(pcmBuffer, new AudioFormat(soundData.sampleRate(), 16, 1, true, false), segments));
            buildingCacheMap.remove(soundData);
        }

        public CustomAudioStream openStream(SoundData soundData) throws IOException, UnsupportedAudioFileException {
            var cache = cacheMap.get(soundData);
            if (cache != null) {
                return new PcmAudioStream(cache.pcm.duplicate(), cache.segments, cache.format);
            }

            BuildingPcmCache buildingCache;
            if ((soundData.samples() / soundData.sampleRate() <= 4) && !buildingCacheMap.contains(soundData)) {
                buildingCache = new BuildingPcmCache(this, soundData);
                buildingCacheMap.put(soundData, PLACE_HOLDER);
            } else {
                buildingCache = null;
            }

            return switch (soundData.soundFormat()) {
                case VORBIS -> new VorbisAudioStream(soundData.byteBuffer(), buildingCache);
                case OPUS -> new OpusAudioStream(soundData.byteBuffer(), buildingCache);
                default -> throw new UnsupportedAudioFileException();
            };
        }

        private record CacheEntry(ByteBuffer pcm, AudioFormat format, IntArrayList segments) {}
    }
}
