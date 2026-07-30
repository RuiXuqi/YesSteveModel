package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.buffer.UniBufferIO;
import com.elfmcys.ysm.format.schema.model.ModelFileConstant;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.util.ProtoBytes;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class ModelAssetAssembler {
    public CompletableFuture<PreparedTransfer> assemble(TaskContext context, ModelFileHandle model,
                                                        ModelAssetSelector selector) {
        var view = model.view();
        var names = ModelAssetPlan.chunks(view.getManifest(), selector);
        var chunks = selector instanceof ModelAssetSelector.ModelPreview
                ? List.of(readImage(context, model, true))
                : selector instanceof ModelAssetSelector.ModelPresentation presentation
                && presentation.asset() == ModelAssetSelector.PresentationAsset.MODEL_ICON
                ? List.of(readImage(context, model, false))
                : names.stream().sorted().map(name -> readChunk(context, model, name)).toList();
        return CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new)).handle((ignored, error) -> {
            if (error != null) {
                for (var chunk : chunks) {
                    if (chunk.isDone() && !chunk.isCompletedExceptionally()) {
                        var value = chunk.getNow(null);
                        if (value != null) {
                            value.close();
                        }
                    }
                }
                throw new CompletionException(error);
            }
            var prepared = chunks.stream().map(CompletableFuture::join).toList();
            var subject = new ModelAssetSubject.Model(model.descriptor().modelHash(),
                    model.descriptor().descriptorHash());
            var manifest = ModelAssetsProto.ModelAssetManifest.newInstance()
                    .setSubject(ModelAssetProtoMapper.toProto(subject))
                    .setSelector(ModelAssetProtoMapper.toProto(selector));
            if (selector instanceof ModelAssetSelector.RenderTarget target
                    && target.components().contains(ModelAssetSelector.RenderTargetComponent.DEFINITION)) {
                manifest.getMutableContainerPreamble()
                        .setInternalArray(model.descriptor().containerPreamble());
                manifest.getMutableSchemaManifest()
                        .setInternalArray(model.descriptor().schemaManifest());
            }
            return assemble(manifest, prepared);
        });
    }

    public PreparedTransfer singleAttachment(ModelAssetSubject subject, ModelAssetSelector selector,
                                             String name, String encoding, int decodedSize,
                                             byte[] contentHash, UniBuffer data) {
        var manifest = ModelAssetsProto.ModelAssetManifest.newInstance()
                .setSubject(ModelAssetProtoMapper.toProto(subject))
                .setSelector(ModelAssetProtoMapper.toProto(selector));
        var chunk = ModelAssetsProto.ModelChunk.newInstance()
                .setName(name)
                .setEncoding(encoding)
                .setDecodedSize(decodedSize)
                .setContentHash(contentHash);
        return assemble(manifest, List.of(new PreparedChunk(chunk, data)));
    }

    private PreparedTransfer assemble(ModelAssetsProto.ModelAssetManifest manifest,
                                      List<PreparedChunk> chunks) {
        long total = 0;
        for (var chunk : chunks) {
            total += chunk.data.size();
        }
        if (total > UniBuffer.MAX_SIZE) {
            chunks.forEach(PreparedChunk::close);
            throw new IllegalArgumentException("Model asset attachments are too large");
        }
        var attachments = NativeBuffer.allocate((int) total);
        try {
            var offset = 0;
            for (var prepared : chunks) {
                var size = prepared.data.size();
                prepared.descriptor.setRawOffset(offset).setRawSize(size);
                manifest.addChunks(prepared.descriptor);
                UniBufferIO.copy(prepared.data, 0, attachments, offset, size);
                offset += size;
            }
            return PreparedTransfer.modelAssets((int) total, attachments, manifest);
        } catch (Throwable error) {
            attachments.close();
            throw error;
        } finally {
            chunks.forEach(PreparedChunk::close);
        }
    }

    private CompletableFuture<PreparedChunk> readImage(TaskContext context,
                                                       ModelFileHandle model, boolean preview) {
        var imageFuture = preview ? model.view().readThumbnail(context, model.chunks())
                : model.view().readIcon(context, model.chunks());
        return imageFuture.thenApply(image -> {
            if (image == null) {
                throw new CompletionException(new FileNotFoundException(
                        preview ? "Model contains no preview image" : "Model contains no icon"));
            }
            try (image) {
                var data = image.data().acquire();
                try {
                    var chunk = ModelAssetsProto.ModelChunk.newInstance()
                            .setName(preview ? ModelFileConstant.THUMB_BUTTON_CHUNK_NAME
                                    : ModelFileConstant.THUMB_ICON_CHUNK_NAME)
                            .setEncoding(image.format().name().toLowerCase(Locale.ROOT))
                            .setDecodedSize((image.width() << 16) | image.height());
                    ProtoBytes.set(chunk.getMutableContentHash(), ModelHashing.blake3(data));
                    return new PreparedChunk(chunk, data);
                } catch (Throwable error) {
                    data.close();
                    throw error;
                }
            }
        });
    }

    private CompletableFuture<PreparedChunk> readChunk(TaskContext context,
                                                       ModelFileHandle model, String name) {
        var info = model.view().getFileView().getAssetView().getChunkInfo(name);
        if (info == null) {
            return CompletableFuture.failedFuture(new FileNotFoundException(
                    "Model chunk not found: " + name));
        }
        return model.chunks().readStoredVerifiedBytes(context, info).thenApply(bytes -> {
            var chunk = ModelAssetsProto.ModelChunk.newInstance()
                    .setName(name)
                    .setEncoding(info.encoding())
                    .setDecodedSize(info.decodeSize())
                    .setFlags(info.flags())
                    .setAlignmentShift(info.alignmentShift())
                    .setContentHash(info.hash() == null ? new byte[0] : info.hash());
            return new PreparedChunk(chunk, ArrayBuffer.move(bytes));
        });
    }

    private record PreparedChunk(ModelAssetsProto.ModelChunk descriptor,
                                 UniBuffer data) implements AutoCloseable {
        @Override
        public void close() {
            data.close();
        }
    }
}
