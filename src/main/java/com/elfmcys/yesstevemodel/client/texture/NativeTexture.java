package com.elfmcys.yesstevemodel.client.texture;

import com.elfmcys.yesstevemodel.util.CleanerUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

import org.jetbrains.annotations.NotNull;

// Native access
public class NativeTexture extends AbstractTexture {
    // Native access
    @SuppressWarnings("all")
    private final long nativeId;

    // Native access
    public NativeTexture(long nativeId) {
        this.nativeId = nativeId;
        CleanerUtil.ref(this, NativeTexture::free);
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
    private native void free();
}
