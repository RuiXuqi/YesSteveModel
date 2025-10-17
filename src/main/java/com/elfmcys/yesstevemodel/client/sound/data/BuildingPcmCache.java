package com.elfmcys.yesstevemodel.client.sound.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class BuildingPcmCache {
    private final SoundDataManager.ModelSoundHolderImpl holder;
    private final SoundData originalSoundData;
    private final ByteBuf pcmBuffer;
    private final IntArrayList segments;
    private boolean invalidate;

    BuildingPcmCache(SoundDataManager.ModelSoundHolderImpl holder, SoundData originalSoundData) {
        this.holder = holder;
        this.originalSoundData = originalSoundData;
        this.pcmBuffer = PooledByteBufAllocator.DEFAULT.directBuffer((int) originalSoundData.samples() * 2);
        this.segments = new IntArrayList(5);
        this.invalidate = false;
    }

    public void putPcm(ByteBuffer pcmBuffer) {
        if (!invalidate && this.pcmBuffer.writableBytes() > 0) {
            var len = Math.min(this.pcmBuffer.writableBytes(), pcmBuffer.remaining());
            this.pcmBuffer.writeBytes(pcmBuffer.limit(len));
            this.segments.add(len);
        }
    }

    public void submit() {
        if (!invalidate) {
            invalidate = true;
            var newBuffer = BufferUtils.createByteBuffer(pcmBuffer.readableBytes());
            pcmBuffer.readBytes(newBuffer.duplicate());
            pcmBuffer.release();
            holder.submitPcmCache(originalSoundData, newBuffer, segments);
        }
    }
}
