package com.elfmcys.yesstevemodel.client.sound.data;

import java.nio.ByteBuffer;

public class SoundData {
    private final ByteBuffer byteBuffer;
    private final SoundFormat soundFormat;
    private final int sampleRate;
    private final long samples;

    // Native Access
    public SoundData(ByteBuffer byteBuffer, int soundFormat, int sampleRate, long samples) {
        if (soundFormat == 2) {
            this.byteBuffer = ByteBuffer.allocateDirect(byteBuffer.remaining());
        } else {
            this.byteBuffer = ByteBuffer.allocate(byteBuffer.remaining());
        }
        this.byteBuffer.duplicate().put(byteBuffer.duplicate());
        this.soundFormat = switch (soundFormat) {
            case 1 -> SoundFormat.VORBIS;
            case 2 -> SoundFormat.OPUS;
            default -> SoundFormat.UNDEFINED;
        };
        this.sampleRate = sampleRate;
        this.samples = samples;
    }

    public long samples() {
        return samples;
    }

    public int sampleRate() {
        return sampleRate;
    }

    public SoundFormat soundFormat() {
        return soundFormat;
    }

    public ByteBuffer byteBuffer() {
        return byteBuffer;
    }
}