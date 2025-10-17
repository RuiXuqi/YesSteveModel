package com.elfmcys.yesstevemodel.client.sound.decoder;

import com.elfmcys.yesstevemodel.client.event.ClientTickEvent;

import java.util.LinkedList;

public class DecoderManager {
    private static final LinkedList<Holder<OpusDecoder>> OPUS_DECODERS = new LinkedList<>();
    private static volatile int NEXT_FREE_TIME = 0;

    public static OpusDecoder getOpusDecoder() {
        synchronized (OPUS_DECODERS) {
            if (!OPUS_DECODERS.isEmpty()) {
                var decoder = OPUS_DECODERS.removeLast().decoder;
                if (OPUS_DECODERS.isEmpty()) {
                    NEXT_FREE_TIME = 0;
                }
                return decoder;
            }
        }
        return new OpusDecoder();
    }

    public static void returnOpusDecoder(OpusDecoder decoder) {
        decoder.reset();
        synchronized (OPUS_DECODERS) {
            var holder = new Holder<>(decoder, ClientTickEvent.getTickCount() + 20 * 10);
            if (OPUS_DECODERS.isEmpty()) {
                NEXT_FREE_TIME = holder.freeTime;
            }
            OPUS_DECODERS.addLast(holder);
        }
    }

    public static void tick() {
        if (NEXT_FREE_TIME == 0) {
            return;
        }
        var tickCount = ClientTickEvent.getTickCount();
        if (tickCount > NEXT_FREE_TIME) {
            synchronized (OPUS_DECODERS) {
                while (!OPUS_DECODERS.isEmpty()) {
                    var holder = OPUS_DECODERS.getFirst();
                    if (holder.freeTime <= tickCount) {
                        holder.decoder.destroy();
                        OPUS_DECODERS.removeFirst();
                    } else {
                        NEXT_FREE_TIME = holder.freeTime;
                        return;
                    }
                }
                NEXT_FREE_TIME = 0;
            }
        }
    }

    private record Holder<T>(T decoder, int freeTime) {}
}
