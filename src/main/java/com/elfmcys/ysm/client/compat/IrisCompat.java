package com.elfmcys.ysm.client.compat;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.info.type.PBRTextureType;
import com.elfmcys.ysm.client.texture.NativeTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.server.packs.resources.ResourceManager;
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
    private static boolean IS_RENDERING_SHADOW = false;
    // Native Access
    @SuppressWarnings("all")
    private static VertexFormat ENTITY_FORMAT;

    public static void init() {
        ModList.get().getModContainerById(MOD_ID).ifPresent(mod -> {
            try {
                if (mod.getModInfo().getVersion().compareTo(new DefaultArtifactVersion("1.7.0")) >= 0) {
                    ENTITY_FORMAT = IrisVertexFormats.ENTITY;
                    ENTITY_ID_GETTER = IrisCompat::getEntityId;
                    PBRLoader.register();
                } else {
                    ENTITY_FORMAT = net.coderbot.iris.vertices.IrisVertexFormats.ENTITY;
                    ENTITY_ID_GETTER = IrisCompat::getEntityIdLegacy;
                    LegacyPBRLoader.register();
                }
                ENTITY_ID_GETTER.getAsLong();
                isRenderingShadow();
                INSTALLED = true;
            } catch (Throwable e) {
                YesSteveModel.LOGGER.error("Failed to setup oculus compat", e);
                ENTITY_FORMAT = null;
                ENTITY_ID_GETTER = null;
                INSTALLED = false;
            }
        });
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isRenderingShadow() {
        return INSTALLED && IrisApi.getInstance().isRenderingShadowPass();
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
        if (INSTALLED) {
            ENTITY_ID = ENTITY_ID_GETTER.getAsLong();
            IS_RENDERING_SHADOW = IrisApi.getInstance().isRenderingShadowPass();
        }
    }

    private static class LegacyPBRLoader implements net.coderbot.iris.texture.pbr.loader.PBRTextureLoader<NativeTexture> {
        private static final LegacyPBRLoader INSTANCE = new LegacyPBRLoader();

        private LegacyPBRLoader() {
        }

        @Override
        public void load(NativeTexture texture, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer) {
            var normalTexture = texture.getPBRTextures().get(PBRTextureType.NORMAL);
            if (normalTexture != null) {
                pbrTextureConsumer.acceptNormalTexture(normalTexture);
            }
            var specularTexture = texture.getPBRTextures().get(PBRTextureType.SPECULAR);
            if (specularTexture != null) {
                pbrTextureConsumer.acceptSpecularTexture(specularTexture);
            }
        }

        public static void register() {
            net.coderbot.iris.texture.pbr.loader.PBRTextureLoaderRegistry.INSTANCE.register(NativeTexture.class, INSTANCE);
        }
    }

    private static class PBRLoader implements PBRTextureLoader<NativeTexture> {
        private static final PBRLoader INSTANCE = new PBRLoader();

        private PBRLoader(){
        }

        @Override
        public void load(NativeTexture texture, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer) {
            var normalTexture = texture.getPBRTextures().get(PBRTextureType.NORMAL);
            if (normalTexture != null) {
                pbrTextureConsumer.acceptNormalTexture(normalTexture);
            }
            var specularTexture = texture.getPBRTextures().get(PBRTextureType.SPECULAR);
            if (specularTexture != null) {
                pbrTextureConsumer.acceptSpecularTexture(specularTexture);
            }
        }

        public static void register() {
            PBRTextureLoaderRegistry.INSTANCE.register(NativeTexture.class, INSTANCE);
        }
    }
}
