package com.elfmcys.ysm.client.sound.decoder;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.nio.ByteBuffer;

public class OpusDecoder {
    private final long ptr;

    @SuppressWarnings("unused")
    private volatile ByteBuffer soundData;

    public OpusDecoder() {
        var ptr = this.ptr = nCreate();
        if (ptr == 0) {
            throw new OutOfMemoryError();
        }
    }

    public void init(ByteBuffer data) throws UnsupportedAudioFileException {
        if (!data.isDirect()) {
            throw new IllegalArgumentException("input is not direct buffer");
        }
        if (!nInit(ptr, data.slice())) {
            throw new UnsupportedAudioFileException();
        }
        this.soundData = data;
    }

    public int decode(ByteBuffer dst) {
        if (!dst.isDirect()) {
            throw new IllegalArgumentException("output is not direct buffer");
        }
        return nDecode(ptr, dst.slice());
    }

    public void reset() {
        nReset(ptr);
        this.soundData = null;
    }

    public void destroy() {
        nDestroy(ptr);
    }

    private native static long nCreate();
    private native static boolean nInit(long ptr, ByteBuffer data);
    private native static int nDecode(long ptr, ByteBuffer dst);
    private native static void nReset(long ptr);
    private native static void nDestroy(long ptr);
}
