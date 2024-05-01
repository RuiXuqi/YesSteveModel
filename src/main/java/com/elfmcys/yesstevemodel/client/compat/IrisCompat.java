package com.elfmcys.yesstevemodel.client.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.function.LongSupplier;

public class IrisCompat {
    private static final String MOD_ID = "oculus";
    private static boolean INSTALLED = false;
    private static LongSupplier ENTITY_ID_GETTER;
    // Native Access
    @SuppressWarnings("all")
    private static long ENTITY_ID = -1;
    // Native Access
    @SuppressWarnings("all")
    private static VertexFormat ENTITY_FORMAT;

    public static void init() {
        ModList.get().getModContainerById(MOD_ID).ifPresent(mod -> {
            INSTALLED = true;
            if (mod.getModInfo().getVersion().compareTo(new DefaultArtifactVersion("1.7.0")) >= 0) {
                ENTITY_FORMAT = IrisVertexFormats.ENTITY;
                ENTITY_ID_GETTER = IrisCompat::getEntityId;
            } else {
                ENTITY_FORMAT = net.coderbot.iris.vertices.IrisVertexFormats.ENTITY;
                ENTITY_ID_GETTER = IrisCompat::getEntityIdLegacy;
            }
        });
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isRenderingShadow() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    private static long getEntityIdLegacy() {
        short s0 = (short) net.coderbot.iris.uniforms.CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        short s1 = (short) net.coderbot.iris.uniforms.CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        short s2 = (short) net.coderbot.iris.uniforms.CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
        return s0 | ((long) s1 << 16) | ((long) s2 << 32);     // little endian
    }

    private static long getEntityId() {
        short s0 = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        short s1 = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        short s2 = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
        return s0 | ((long) s1 << 16) | ((long) s2 << 32);     // little endian
    }

    public static void setupState() {
        ENTITY_ID = ENTITY_ID_GETTER.getAsLong();
    }
}
