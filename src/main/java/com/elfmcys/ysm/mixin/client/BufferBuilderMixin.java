package com.elfmcys.ysm.mixin.client;

import com.elfmcys.ysm.api.VertexBufferAccessor;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexBufferAccessor {
    @Shadow
    private ByteBuffer buffer;
    @Shadow
    private int nextElementByte;
    @Shadow
    private int vertices;
    @Shadow
    private VertexFormat format;

    @Shadow
    protected abstract void ensureCapacity(int increaseAmount);

    @Unique
    @Override
    public boolean ysm$ok() {
        return true;
    }

    @Unique
    @Override
    public VertexFormat ysm$vertexFormat() {
        return format;
    }

    @Unique
    @Override
    public NativeBuffer ysm$reserve(int vertexCount) {
        ensureCapacity(vertexCount);
        return NativeBuffer.borrow(this.buffer.slice(this.nextElementByte, vertexCount * this.format.getVertexSize()));
    }

    @Override
    public void ysm$advance(int vertexCount) {
        this.vertices += vertexCount;
        this.nextElementByte += vertexCount * this.format.getVertexSize();
    }
}
