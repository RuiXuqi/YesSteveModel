package com.elfmcys.ysm.format.schema.model;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.format.container.AssetContainerReader;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.format.container.InlineChunkReader;
import com.elfmcys.ysm.format.schema.file.AssetFileView;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.model.views.CommonAssetView;
import com.elfmcys.ysm.format.schema.model.views.ModelInfoView;
import com.elfmcys.ysm.format.schema.model.views.RenderTargetView;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.RenderTargetIds;
import com.elfmcys.ysm.natives.image.Image;
import com.elfmcys.ysm.natives.image.ImageSource;
import mixel.manifest.ManifestOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.info.InfoOuterClass;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.util.ProtoUtil;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ModelFileView {
    private final boolean supported;

    private final AssetFileView fileView;
    private final ModelInfoView metadataView;
    private final List<RenderTargetView> renderTargets;
    private final Map<String, RenderTargetView> renderTargetsById;
    private final CommonAssetView commonView;
    private final ManifestOuterClass.Manifest manifest;
    private volatile byte[] manifestBytes;
    private final InfoOuterClass.PreviewSource thumbnailPreviewSource;

    public ModelFileView(SeekableByteChannel file) throws IOException {
        this(AssetContainerReader.read(file), file);
    }

    private ModelFileView(AssetContainerView assetView, SeekableByteChannel file) throws IOException {
        this(assetView, readManifest(assetView, file));
    }

    private ModelFileView(AssetContainerView assetView, ManifestData manifestData) throws IOException {
        if (!ModelFileConstant.SCHEMA_ID.equals(assetView.getSchema())) {
            throw new IOException("Schema ID mismatch: " + assetView.getSchema());
        }

        var vendor = Objects.requireNonNull(assetView.getSchemaProperty(
                ModelFileConstant.PROP_VENDOR), "Vendor property not found");
        var version = new DefaultArtifactVersion(Objects.requireNonNull(assetView.getSchemaProperty(
                ModelFileConstant.PROP_VERSION), "Version property not found"));
        if (!version.equals(ModelFileConstant.CURRENT_VERSION)) {
            throw new UnsupportedEncodingException(String.format("Unsupported model version: \"%s\". Exported by \"%s\"",
                    version, vendor));
        }
        supported = true;

        if (!assetView.isAcceptedVersion()) {
            throw new UnsupportedEncodingException(String.format("Unsupported asset container version: \"%s\". Exported by \"%s\"",
                    version, vendor));
        }

        this.manifest = manifestData.manifest();
        this.manifestBytes = manifestData.bytes();
        this.thumbnailPreviewSource = thumbnailSource(manifest.getInfo());

        this.fileView = new AssetFileView(assetView);
        metadataView = new ModelInfoView(manifest.getInfo(), this.fileView);
        commonView = new CommonAssetView(manifest.getCommonBehavior(), this.fileView);
        validateThumbnailSource(thumbnailPreviewSource,
                assetView.getChunkInfo(ModelFileConstant.THUMB_BUTTON_CHUNK_NAME) != null);
        validatePlayerRenderTarget(manifest);
        var targets = new ArrayList<RenderTargetView>(manifest.getRenderTargets().length());
        var byId = new LinkedHashMap<String, RenderTargetView>();
        for (var target : manifest.getRenderTargets()) {
            var view = new RenderTargetView(this.fileView, target);
            if (view.id().isBlank() || byId.putIfAbsent(view.id(), view) != null) {
                throw new IOException("Duplicate or empty render target id: " + view.id());
            }
            if (view.kind() == RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_UNSPECIFIED) {
                throw new IOException("Render target has no kind: " + view.id());
            }
            if (view.getTextureNames().isEmpty()) {
                throw new IOException("Render target has no textures: " + view.id());
            }
            targets.add(view);
        }
        renderTargets = List.copyOf(targets);
        renderTargetsById = Map.copyOf(byId);
    }

    static void validatePlayerRenderTarget(ManifestOuterClass.Manifest manifest) throws IOException {
        if (!manifest.hasRenderTargets()) {
            throw new IOException("Model contains no player render target");
        }
        var found = false;
        for (var target : manifest.getRenderTargets()) {
            var targetId = target.hasTargetId() ? target.getTargetId() : "";
            var kind = target.hasKind()
                    ? target.getKind()
                    : RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_UNSPECIFIED;
            if (targetId.equals(RenderTargetIds.PLAYER)) {
                if (kind != RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER) {
                    throw new IOException("Player render target has invalid kind: " + kind);
                }
                found = true;
            } else if (kind == RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER) {
                throw new IOException("Player render target has invalid id: " + targetId);
            }
        }
        if (!found) {
            throw new IOException("Model contains no player render target");
        }
    }

    static void validateThumbnailSource(InfoOuterClass.PreviewSource source, boolean hasThumbnail) throws IOException {
        if (source == null) {
            throw new IOException("Thumbnail has an unknown preview source");
        }
        if (source == InfoOuterClass.PreviewSource.PREVIEW_SOURCE_UNSPECIFIED) {
            if (hasThumbnail) {
                throw new IOException("Thumbnail chunk has no preview source");
            }
        } else if (!hasThumbnail) {
            throw new IOException("Thumbnail preview source has no thumbnail chunk: " + source);
        }
    }

    static InfoOuterClass.PreviewSource thumbnailSource(InfoOuterClass.Info info) {
        return info.hasThumbnailSource()
                ? info.getThumbnailSource()
                : InfoOuterClass.PreviewSource.PREVIEW_SOURCE_UNSPECIFIED;
    }

    public static ModelFileView readMetadata(byte[] containerPreamble, byte[] manifestBytes) {
        try {
            var assetView = AssetContainerReader.readPreamble(containerPreamble);
            var manifest = ManifestOuterClass.Manifest.parseFrom(ProtoSource.newInstance(manifestBytes));
            return new ModelFileView(assetView, new ManifestData(manifest, manifestBytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ManifestData readManifest(AssetContainerView assetView, SeekableByteChannel file) throws IOException {
        var chunk = assetView.getChunkInfo(ModelFileConstant.MANIFEST_CHUNK_NAME);
        if (chunk == null) {
            throw new IOException("No manifest data found");
        }
        try (var data = InlineChunkReader.readPayload(file, chunk, BufferType.ARRAY)) {
            if (data.size() == 0) {
                throw new IOException("No manifest data found");
            }
            if (!(data instanceof ArrayBuffer arrayBuffer)) {
                throw new IOException("Model manifest is not array-backed");
            }
            var manifest = ManifestOuterClass.Manifest.parseFrom(ProtoUtil.source(arrayBuffer));
            var bytes = new byte[arrayBuffer.size()];
            System.arraycopy(arrayBuffer.array(), arrayBuffer.arrayOffset(), bytes, 0, bytes.length);
            return new ManifestData(manifest, bytes);
        }
    }

    public boolean supported() {
        return supported;
    }

    public AssetFileView getFileView() {
        return fileView;
    }

    public CompletableFuture<@Nullable Image> readThumbnail(TaskContext ctx, ChunkDataSource source) {
        return fileView.readImageChunk(ctx, source, ModelFileConstant.THUMB_BUTTON_CHUNK_NAME);
    }

    public @Nullable ImageSource thumbnailSource(ChunkDataSource source) throws IOException {
        return fileView.imageChunkSource(source, ModelFileConstant.THUMB_BUTTON_CHUNK_NAME);
    }

    public InfoOuterClass.PreviewSource getThumbnailPreviewSource() {
        return thumbnailPreviewSource;
    }

    public CompletableFuture<@Nullable Image> readIcon(TaskContext ctx, ChunkDataSource source) {
        return fileView.readImageChunk(ctx, source, ModelFileConstant.THUMB_ICON_CHUNK_NAME);
    }

    public @Nullable ImageSource iconSource(ChunkDataSource source) throws IOException {
        return fileView.imageChunkSource(source, ModelFileConstant.THUMB_ICON_CHUNK_NAME);
    }

    public ModelInfoView getMetadata() {
        return metadataView;
    }

    public List<RenderTargetView> getRenderTargets() {
        return renderTargets;
    }

    public @Nullable RenderTargetView getRenderTarget(String targetId) {
        return renderTargetsById.get(targetId);
    }

    public RenderTargetView requireRenderTarget(String targetId) {
        var target = getRenderTarget(targetId);
        if (target == null) {
            throw new IllegalArgumentException("Model contains no render target: " + targetId);
        }
        return target;
    }

    public RenderTargetView getPlayer() {
        return requireRenderTarget(RenderTargetIds.PLAYER);
    }

    public CommonAssetView getCommon() {
        return commonView;
    }

    public ManifestOuterClass.Manifest getManifest() {
        return manifest;
    }

    public byte[] getManifestBytes() {
        return manifestBytes.clone();
    }

    /** Drops the encoded manifest after intrinsic assets have been fully materialized. */
    public void discardManifestBytes() {
        manifestBytes = new byte[0];
    }

    public ModelHash getModelHash() throws IOException {
        var properties = manifest.getInfo().getProperties();
        if (!properties.hasHashId() || properties.getHashId().length() != ModelHash.SIZE) {
            throw new IOException("Manifest contains no valid full model hash");
        }
        var hash = properties.getHashId();
        return new ModelHash(hash.array(), 0, hash.length());
    }

    private record ManifestData(ManifestOuterClass.Manifest manifest, byte[] bytes) {
    }
}
