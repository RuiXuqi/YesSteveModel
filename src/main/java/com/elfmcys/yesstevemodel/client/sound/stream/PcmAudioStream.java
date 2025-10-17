package com.elfmcys.yesstevemodel.client.sound.stream;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PcmAudioStream implements CustomAudioStream {
    private final static ByteBuffer EMPTY_BUFFER = BufferUtils.createByteBuffer(0);

    private final ByteBuffer byteBuffer;
    private final IntArrayList segments;
    private final AudioFormat audioFormat;
    private int offset;
    private int index;
    private volatile boolean closed;

    public PcmAudioStream(ByteBuffer byteBuffer, IntArrayList segments, AudioFormat audioFormat) throws UnsupportedAudioFileException {
        if (audioFormat.getChannels() != 1) {
            throw new UnsupportedAudioFileException();
        }
        this.byteBuffer = byteBuffer;
        this.segments = segments;
        this.audioFormat = audioFormat;
    }

    @Override
    public @NotNull AudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public @NotNull ByteBuffer read(int size) throws IOException {
        if (index >= segments.size() || closed) {
            return EMPTY_BUFFER;
        }
        var segment = segments.getInt(index);
        var slice = byteBuffer.slice(offset, segment);
        index++;
        offset += segment;
        return slice;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
