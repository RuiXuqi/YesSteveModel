package com.elfmcys.yesstevemodel.client.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.vertices.IrisVertexFormats;
import net.minecraftforge.fml.ModList;

public class IrisCompat {
    private static final String MOD_ID = "oculus";
    private static boolean INSTALLED;
    // Native Access
    @SuppressWarnings("all")
    private static long ENTITY_ID = -1;
    // Native Access
    @SuppressWarnings("all")
    private static VertexFormat ENTITY_FORMAT;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
        if (INSTALLED) {
            ENTITY_FORMAT = IrisVertexFormats.ENTITY;
        }
    }

    public static void setupState() {
        if (!INSTALLED) {
            return;
        }
        setupEntityId();
    }

    private static void setupEntityId() {
        short s0 = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        short s1 = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        short s2 = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
        ENTITY_ID = s0 | ((long) s1 << 16) | ((long) s2 << 32);     // little endian
    }
}
