package com.elfmcys.ysm.format.schema.model.views;

import com.elfmcys.ysm.format.schema.file.AssetFileView;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.file.PBRImageSources;
import mixel.asset.model.ModelDataOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.asset.Texture;
import com.elfmcys.ysm.task.TaskContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Read-only access to one independently distributable render target. */
public final class RenderTargetView {
    private final AssetFileView fileView;
    private final RenderTargetOuterClass.RenderTarget descriptor;
    private final Map<String, Texture.PBRTextureSet> textures;

    public RenderTargetView(AssetFileView fileView, RenderTargetOuterClass.RenderTarget descriptor) {
        this.fileView = fileView;
        this.descriptor = descriptor;
        var values = new LinkedHashMap<String, Texture.PBRTextureSet>();
        if (descriptor.hasTextures()) {
            descriptor.getTextures().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        }
        this.textures = Map.copyOf(values);
    }

    public String id() {
        return descriptor.hasTargetId() ? descriptor.getTargetId() : "";
    }

    public RenderTargetOuterClass.RenderTargetKind kind() {
        return descriptor.hasKind()
                ? descriptor.getKind()
                : RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_UNSPECIFIED;
    }

    public List<String> matches() {
        var values = new ArrayList<String>();
        if (descriptor.hasMatch()) {
            descriptor.getMatch().forEach(values::add);
        }
        return List.copyOf(values);
    }

    public Set<String> getTextureNames() {
        return textures.keySet();
    }

    public Texture.PBRTextureSet textureDescriptor(String name) throws FileNotFoundException {
        var value = textures.get(name);
        if (value == null) {
            throw new FileNotFoundException("Render target " + id() + " contains no texture named " + name);
        }
        return value;
    }

    public CompletableFuture<ModelDataOuterClass.ModelData> readDefinition(TaskContext context,
                                                                            ChunkDataSource source) {
        return fileView.readProtoBlob(context, source, descriptor.getBlobId(),
                ModelDataOuterClass.ModelData::parseFrom);
    }

    public PBRImageSources textureSources(ChunkDataSource source, String textureName)
            throws IOException {
        return fileView.textureSources(source, textureDescriptor(textureName));
    }

    public RenderTargetOuterClass.RenderTarget descriptor() {
        return descriptor;
    }
}
