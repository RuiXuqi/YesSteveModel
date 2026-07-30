package com.elfmcys.ysm.format.schema.file;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerView;
import com.elfmcys.ysm.natives.image.Image;
import com.elfmcys.ysm.util.ProtoUtil;
import com.elfmcys.ysm.natives.image.ImageSource;
import mixel.common.ImageOuterClass;
import mixel.manifest.asset.Texture;
import com.elfmcys.ysm.task.TaskContext;
import org.jetbrains.annotations.Nullable;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.ProtoSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

public class AssetFileView {
    private final AssetContainerView assetView;

    public AssetFileView(AssetContainerView assetView) {
        this.assetView = assetView;
    }

    public CompletableFuture<Image> readImageBlob(TaskContext ctx, ChunkDataSource source, ImageOuterClass.Image img) {
        var chunk = assetView.getChunkInfo(AssetFileConstant.BLOB_CHUNK_PREFIX + img.getBlobId());
        if (chunk == null) {
            return CompletableFuture.failedFuture(new FileNotFoundException(
                    "Image blob " + img.getBlobId() + " not found"));
        }
        final ChunkImageSource.KnownImageMetadata metadata;
        try {
            metadata = ChunkImageSource.blobMetadata(
                    chunk, imageFormat(img.getFormat(), chunk.type()), img.getWidth(), img.getHeight());
        } catch (IOException error) {
            return CompletableFuture.failedFuture(error);
        }
        return source.readPayload(ctx, chunk, BufferType.NATIVE)
                .thenApply(blob -> new Image(metadata.format(), metadata.width(), metadata.height(), blob));
    }

    public ImageSource imageBlobSource(ChunkDataSource source, ImageOuterClass.Image image)
            throws IOException {
        var chunk = assetView.getChunkInfo(AssetFileConstant.BLOB_CHUNK_PREFIX + image.getBlobId());
        if (chunk == null) {
            throw new FileNotFoundException("Image blob " + image.getBlobId() + " not found");
        }
        var format = imageFormat(image.getFormat(), chunk.type());
        return ChunkImageSource.blob(source, chunk, format, image.getWidth(), image.getHeight());
    }

    public @Nullable ImageSource imageChunkSource(ChunkDataSource source, String chunkType) throws IOException {
        var chunk = assetView.getChunkInfo(chunkType);
        if (chunk == null) {
            return null;
        }
        return ChunkImageSource.named(source, chunk);
    }

    public AssetContainerView getAssetView() {
        return assetView;
    }

    public PBRImageSources textureSources(ChunkDataSource source, Texture.PBRTextureSet texture)
            throws IOException {
        var uv = imageBlobSource(source, texture.getUv());
        var normal = texture.hasNormal() ? imageBlobSource(source, texture.getNormal()) : null;
        var specular = texture.hasSpecular() ? imageBlobSource(source, texture.getSpecular()) : null;
        return new PBRImageSources(uv, normal, specular);
    }

    public CompletableFuture<UniBuffer> readPayload(TaskContext ctx, ChunkDataSource source,
                                                     String type, BufferType outputType) {
        var chunkInfo = assetView.getChunkInfo(type);
        if (chunkInfo == null) {
            return CompletableFuture.failedFuture(new FileNotFoundException("Chunk " + type + " not found"));
        }
        return source.readPayload(ctx, chunkInfo, outputType);
    }

    public CompletableFuture<UniBuffer> readBlobPayload(TaskContext ctx, ChunkDataSource source,
                                                         int id, BufferType outputType) {
        return readPayload(ctx, source, AssetFileConstant.BLOB_CHUNK_PREFIX + id, outputType);
    }

    public CompletableFuture<@Nullable InputStream> openStream(TaskContext ctx, ChunkDataSource source, int id) {
        var type = AssetFileConstant.STREAM_CHUNK_PREFIX + id;
        var chunkInfo = assetView.getChunkInfo(type);
        if (chunkInfo == null) {
            return CompletableFuture.completedFuture(null);
        }
        return source.openPayloadStream(ctx, chunkInfo);
    }

    public <D extends ProtoMessage<D>> CompletableFuture<D> readProtoBlob(TaskContext ctx, ChunkDataSource source, int blobId, ProtoReader<D> reader) {
        return readBlobPayload(ctx, source, blobId, BufferType.ARRAY).thenApplyAsync(buf -> {
            try (buf) {
                try {
                    if (!(buf instanceof ArrayBuffer arrayBuffer)) {
                        throw new IllegalStateException("Array chunk read returned a non-array buffer");
                    }
                    return reader.readProto(ProtoUtil.source(arrayBuffer));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }, ctx.executor());
    }

    public CompletableFuture<@Nullable Image> readImageChunk(TaskContext ctx, ChunkDataSource source, String chunkType) {
        var chunkInfo = assetView.getChunkInfo(chunkType);
        if (chunkInfo == null) {
            return CompletableFuture.completedFuture(null);
        }
        final ChunkImageSource.KnownImageMetadata metadata;
        try {
            metadata = ChunkImageSource.namedMetadata(chunkInfo);
        } catch (IOException error) {
            return CompletableFuture.failedFuture(error);
        }
        return readPayload(ctx, source, chunkType, BufferType.NATIVE)
                .thenApply(data -> new Image(metadata.format(), metadata.width(), metadata.height(), data));
    }

    private static Image.Format imageFormat(String encoding, String chunkType) throws IOException {
        if (encoding.isEmpty()) {
            throw new IOException("Image protobuf has no format for chunk " + chunkType);
        }
        try {
            return Image.Format.valueOf(encoding.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IOException("Unknown image protobuf format for chunk " + chunkType + ": " + encoding, error);
        }
    }

    @FunctionalInterface
    public interface ProtoReader<D extends ProtoMessage<D>> {
        D readProto(ProtoSource proto) throws IOException;
    }
}
