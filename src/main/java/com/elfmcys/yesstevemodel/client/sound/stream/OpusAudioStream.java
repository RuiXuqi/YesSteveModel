package com.elfmcys.yesstevemodel.client.sound.stream;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.sound.decoder.DecoderManager;
import com.elfmcys.yesstevemodel.client.sound.decoder.OpusDecoder;
import com.elfmcys.yesstevemodel.client.sound.data.BuildingPcmCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class OpusAudioStream implements CustomAudioStream {
    private final static ByteBuffer EMPTY_BUFFER = BufferUtils.createByteBuffer(0);
    private final static AudioFormat AUDIO_FORMAT = new AudioFormat(48000, 16, 1, true, false);

    private final OpusDecoder decoder;
    private final @Nullable BuildingPcmCache pcmCache;
    private final ByteBuf buf;
    private volatile boolean closed;
    private boolean eof;

    public OpusAudioStream(ByteBuffer data, @Nullable BuildingPcmCache pcmCache) throws UnsupportedAudioFileException {
        this.decoder = DecoderManager.getOpusDecoder();
        this.pcmCache = pcmCache;
        this.buf = PooledByteBufAllocator.DEFAULT.buffer((int) AUDIO_FORMAT.getSampleRate() * 2);
        this.buf.retain();
        this.decoder.init(data);
    }

    @Override
    public @NotNull ByteBuffer read(int size) throws IOException {
        if (size == 0 || eof || closed) {
            return EMPTY_BUFFER;
        }

        if (buf.capacity() < size) {
            buf.capacity(size);
        }
        var dst = buf.nioBuffer(0, size);
        var len = decoder.decode(dst.duplicate());
        if (len <= 0) {
            if (len == 0 && pcmCache != null) {
                pcmCache.submit();
            }
            if (len < 0) {
                YesSteveModel.LOGGER.error("Decoder error: {}", len);
            }
            eof = true;
            return EMPTY_BUFFER;
        }

        var result = dst.slice(0, len);
        if (pcmCache != null) {
            pcmCache.putPcm(result.slice());
        }
        return result;
    }

    @Override
    public @NotNull AudioFormat getFormat() {
        return AUDIO_FORMAT;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            buf.release();
            DecoderManager.returnOpusDecoder(decoder);
            closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
