package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.buffer.BufferType;
import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.client.animation.molang.CustomMolangParser;
import com.elfmcys.ysm.client.model.internal.render.AnimationProtoMapper;
import com.elfmcys.ysm.format.schema.file.AssetFileConstant;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.model.ModelFileConstant;
import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.natives.render.NativeBakedModel;
import mixel.asset.model.ModelDataOuterClass;
import mixel.asset.model.data.AnimationOuterClass;
import mixel.asset.model.data.GeoModelOuterClass;
import mixel.manifest.asset.Texture;
import com.elfmcys.ysm.util.ProtoUtil;
import us.hebi.quickbuf.ProtoSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Build-safe semantic materialization. It never registers textures or writes bake caches. */
public final class BuiltinModelMaterializer {
    private static final int CURRENT_RAW_UV_VERSION = 29;

    private BuiltinModelMaterializer() {
    }

    public static Result materialize(ModelFileHandle handle) throws IOException {
        var view = handle.view();
        var chunks = handle.chunks();
        decodeNamedImage(view, chunks, ModelFileConstant.THUMB_BUTTON_CHUNK_NAME);
        decodeNamedImage(view, chunks, ModelFileConstant.THUMB_ICON_CHUNK_NAME);
        decodePresentationImages(view, chunks);

        var animationHashes = new LinkedHashMap<DefaultAnimationKey, Hash256>();
        for (var target : view.getRenderTargets()) {
            var definition = readDefinition(view, chunks, target.descriptor().getBlobId());
            bindControllers(definition);
            bindAndHashAnimations(target.descriptor(), definition, animationHashes);
            for (var textureName : target.getTextureNames()) {
                var texture = target.textureDescriptor(textureName);
                var sources = target.textureSources(chunks, textureName);
                decodeAdditionalTextureImages(sources);
                try (var uv = sources.uv().open();
                     var pixels = uv.decodeToBuffer()) {
                    for (var geoEntry : definition.getGeoModels()) {
                        var geo = GeoModelOuterClass.GeoModel.parseFrom(
                                ProtoSource.newInstance(geoEntry.getValue()));
                        if (!NativeBakedModel.tryBake(geo, pixels, uv.width(), uv.height(),
                                CURRENT_RAW_UV_VERSION,
                                target.descriptor().getSettings().getForceCulling(),
                                false, hasPbr(texture))) {
                            throw new IOException("Native tryBake rejected model target="
                                    + target.id() + " texture=" + textureName
                                    + " geometry=" + geoEntry.getKey());
                        }
                    }
                }
            }
        }
        bindCommonStrings(view, chunks);
        verifyStreams(view, chunks);
        return new Result(handle.descriptor().modelHash(), Map.copyOf(animationHashes));
    }

    private static ModelDataOuterClass.ModelData readDefinition(
            ModelFileView view, ChunkDataSource chunks, int blobId) throws IOException {
        var chunk = view.getFileView().getAssetView().getChunkInfo(
                AssetFileConstant.BLOB_CHUNK_PREFIX + blobId);
        if (chunk == null) {
            throw new FileNotFoundException("Missing render target definition blob " + blobId);
        }
        try (var data = chunks.readPayload(chunk, BufferType.ARRAY)) {
            return ModelDataOuterClass.ModelData.parseFrom(
                    ProtoUtil.source((ArrayBuffer) data));
        }
    }

    private static void bindControllers(ModelDataOuterClass.ModelData definition) {
        if (definition.hasAnimationControllers()) {
            definition.getAnimationControllers().forEach(entry ->
                    AnimationProtoMapper.controllerFile(entry.getValue()));
        }
    }

    private static void bindAndHashAnimations(
            mixel.manifest.asset.RenderTargetOuterClass.RenderTarget target,
            ModelDataOuterClass.ModelData definition,
            Map<DefaultAnimationKey, Hash256> output) throws IOException {
        if (!definition.hasAnimationFiles()) {
            return;
        }
        for (var file : definition.getAnimationFiles()) {
            var domain = DefaultAnimationKey.domain(target, file.getKey());
            for (var animation : file.getValue().getAnimations()) {
                AnimationProtoMapper.animation(animation);
                var key = new DefaultAnimationKey(domain, animation.getName());
                var hash = payloadHash(animation);
                var previous = output.putIfAbsent(key, hash);
                if (previous != null && !previous.equals(hash)) {
                    throw new IOException("Conflicting default animation payloads for " + key);
                }
            }
        }
    }

    public static Hash256 payloadHash(AnimationOuterClass.Animation animation)
            throws IOException {
        return ModelHashing.blake3(ProtoUtil.serializeToArray(animation));
    }

    private static void decodeAdditionalTextureImages(
            com.elfmcys.ysm.format.schema.file.PBRImageSources sources)
            throws IOException {
        if (sources.normal() != null) {
            decode(sources.normal());
        }
        if (sources.specular() != null) {
            decode(sources.specular());
        }
    }

    private static void decode(com.elfmcys.ysm.natives.image.ImageSource source)
            throws IOException {
        try (var image = source.open(); var ignored = image.decodeToBuffer()) {
        }
    }

    private static void decodeNamedImage(ModelFileView view, ChunkDataSource chunks,
                                         String type) throws IOException {
        var source = view.getFileView().imageChunkSource(chunks, type);
        if (source != null) {
            decode(source);
        }
    }

    private static void decodePresentationImages(ModelFileView view,
                                                 ChunkDataSource chunks)
            throws IOException {
        var info = view.getManifest().getInfo();
        if (info.hasSettings()) {
            var settings = info.getSettings();
            if (settings.hasGuiForeground()) {
                decode(view.getFileView().imageBlobSource(
                        chunks, settings.getGuiForeground()));
            }
            if (settings.hasGuiBackground()) {
                decode(view.getFileView().imageBlobSource(
                        chunks, settings.getGuiBackground()));
            }
        }
        if (info.hasMetadata() && info.getMetadata().hasAuthors()) {
            for (var author : info.getMetadata().getAuthors()) {
                if (author.hasAvatar()) {
                    decode(view.getFileView().imageBlobSource(
                            chunks, author.getAvatar()));
                }
            }
        }
    }

    private static void bindCommonStrings(ModelFileView view, ChunkDataSource chunks)
            throws IOException {
        var common = view.getManifest().getCommonBehavior();
        if (!common.hasStringsBlobId() || common.getStringsBlobId() == 0) {
            return;
        }
        var chunk = view.getFileView().getAssetView().getChunkInfo(
                AssetFileConstant.BLOB_CHUNK_PREFIX + common.getStringsBlobId());
        if (chunk == null) {
            throw new FileNotFoundException("Missing common strings blob");
        }
        try (var data = chunks.readPayload(chunk, BufferType.ARRAY)) {
            var strings = mixel.asset.strings.StringDataOuterClass.StringData
                    .parseFrom(ProtoUtil.source((ArrayBuffer) data));
            if (strings.hasUserFunctions()) {
                var parser = CustomMolangParser.rentInstance();
                try {
                    strings.getUserFunctions().forEach(function ->
                            parser.parseExpression(function.getContent(), false));
                } finally {
                    CustomMolangParser.returnInstance(parser);
                }
            }
        }
    }

    private static void verifyStreams(ModelFileView view, ChunkDataSource chunks)
            throws IOException {
        var common = view.getManifest().getCommonBehavior();
        if (!common.hasSounds()) {
            return;
        }
        for (var sound : common.getSounds()) {
            var chunk = view.getFileView().getAssetView().getChunkInfo(
                    AssetFileConstant.STREAM_CHUNK_PREFIX + sound.getStreamId());
            if (chunk == null) {
                throw new FileNotFoundException("Missing sound stream " + sound.getName());
            }
            try (var input = chunks.openPayloadStream(chunk)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            }
        }
    }

    private static boolean hasPbr(Texture.PBRTextureSet texture) {
        return texture.hasNormal() || texture.hasSpecular();
    }

    public record Result(Hash256 modelHash,
                         Map<DefaultAnimationKey, Hash256> animationHashes) {
    }
}
