package com.elfmcys.ysm.natives.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeRendererTest {
    @Test
    void convertsJavaArgbToNativeLowRedRgba() {
        assertEquals(0xAADDCCBB, NativeRenderer.packColor(0xAABBCCDD));
        assertEquals(0xFF0000FF, NativeRenderer.packColor(0xFFFF0000));
        assertEquals(0xFFFF0000, NativeRenderer.packColor(0xFF0000FF));
    }
}
