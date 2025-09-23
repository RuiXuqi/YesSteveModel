package com.elfmcys.yesstevemodel.client.texture;

import com.elfmcys.yesstevemodel.info.type.PBRTextureType;
import com.elfmcys.yesstevemodel.util.CleanerUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

// Native access
public class NativeTexture extends AbstractTexture implements PBRTextureSet {
    // Native access
    @SuppressWarnings("all")
    private final long nativeId;
    private Map<PBRTextureType, NativeTexture> pbrTextures = Reference2ReferenceMaps.emptyMap();

    // Native access
    public NativeTexture(long nativeId) {
        this.nativeId = nativeId;
        CleanerUtil.ref(this, nativeId, NativeTexture::free);
    }

    @Override
    public void load(@NotNull ResourceManager resourceManager) {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::doLoad);
        } else {
            this.doLoad();
        }
    }

    private void doLoad() {
        GlStateManager._bindTexture(getId());
        upload();
    }

    private native void upload();

    // 不需要在 Render Thread 上调用
    private static native void free(long id);

    // Native Access
    @SuppressWarnings("unused")
    private void setPBRTextures(Map<PBRTextureType, NativeTexture> pbrTextures) {
        this.pbrTextures = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(pbrTextures));
    }

    public Map<PBRTextureType, ? extends AbstractTexture> getPBRTextures() {
        return pbrTextures;
    }
}
