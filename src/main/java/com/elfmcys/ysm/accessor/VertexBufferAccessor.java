package com.elfmcys.ysm.accessor;

import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.annotation.Borrowed;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

public interface VertexBufferAccessor {
    boolean ysm$ok();

    VertexFormat ysm$vertexFormat();

    @Borrowed
    NativeBuffer ysm$reserve(int maxVertexCount);

    void ysm$advance(int actualVertexCount);
}
