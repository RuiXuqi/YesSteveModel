package com.elfmcys.ysm.natives.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

class FallbackVertexWriter {
    private static final int STRIDE = 8;

    private static final int INDEX_COLOR = 0;
    private static final int INDEX_NORMAL = 1;
    private static final int INDEX_X = 2;
    private static final int INDEX_Y = 3;
    private static final int INDEX_Z = 4;
    private static final int INDEX_TEX_U = 5;
    private static final int INDEX_TEX_V = 6;
    private static final int INDEX_LIGHT = 7;

    private static float[] VERTEX_DATA;

    static float[] getVertexData(int vertexCount) {
        var size = vertexCount * STRIDE;
        if (VERTEX_DATA == null || VERTEX_DATA.length < size) {
            VERTEX_DATA = new float[size];
        }
        return VERTEX_DATA;
    }

    static void write(VertexConsumer vertexBuffer, int vertexCount, int overlayUv) {
        var vertexData = VERTEX_DATA;
        for (int i = 0; i < vertexCount; i++) {
            var offset = i * STRIDE;

            var color = Float.floatToRawIntBits(vertexData[offset + INDEX_COLOR]);
            var r = (color >>> 24) / 255f;
            var g = ((color >>> 16) & 0xFF) / 255f;
            var b = ((color >>> 8) & 0xFF) / 255f;
            var a = (color & 0xFF) / 255f;

            var normal = Float.floatToRawIntBits(vertexData[offset + INDEX_NORMAL]);
            var normalX = (byte) ((normal >> 16) & 0xFF) * 127;
            var normalY = (byte) ((normal >> 8) & 0xFF) * 127;
            var normalZ = (byte) ((normal & 0xFF)) * 127;

            vertexBuffer.vertex(
                    vertexData[offset + INDEX_X],
                    vertexData[offset + INDEX_Y],
                    vertexData[offset + INDEX_Z],
                    r,
                    g,
                    b,
                    a,
                    vertexData[offset + INDEX_TEX_U],
                    vertexData[offset + INDEX_TEX_V],
                    overlayUv,
                    Float.floatToRawIntBits(vertexData[offset + INDEX_LIGHT]),
                    normalX,
                    normalY,
                    normalZ);
        }
    }
}
