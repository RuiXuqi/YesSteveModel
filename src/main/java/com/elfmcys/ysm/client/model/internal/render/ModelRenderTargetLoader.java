package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.animation.molang.CustomMolangParser;
import com.elfmcys.ysm.client.model.AnimationStore;
import com.elfmcys.ysm.client.model.ModelResourceFailures;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.PlayerLocator;
import com.elfmcys.ysm.client.model.PlayerModelVariant;
import com.elfmcys.ysm.client.model.data.CommonAssetData;
import com.elfmcys.ysm.client.model.data.ModelRenderTargetBuildInput;
import com.elfmcys.ysm.client.model.data.PlayerModelData;
import com.elfmcys.ysm.client.model.data.ProjectileModelData;
import com.elfmcys.ysm.client.model.data.RenderTargetData;
import com.elfmcys.ysm.client.model.data.VehicleModelData;
import com.elfmcys.ysm.client.model.internal.metadata.ManifestModelInfoMapper;
import com.elfmcys.ysm.client.texture.CustomPBRTextureSet;
import com.elfmcys.ysm.format.schema.file.AssetFileConstant;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.file.PBRImageSources;
import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.format.schema.model.views.RenderTargetView;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.geckolib3.file.AnimationControllerFile;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.catalog.DefaultAnimationKey;
import com.elfmcys.ysm.network.message.model.ModelAssetPlan;
import mixel.common.ImageOuterClass;
import mixel.asset.model.ModelDataOuterClass;
import mixel.asset.model.data.GeoModelOuterClass;
import mixel.asset.strings.StringDataOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.asset.Texture;
import com.elfmcys.ysm.task.TaskContext;
import com.elfmcys.ysm.util.FifoHashMap;
import com.elfmcys.ysm.util.ResourceTransaction;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import us.hebi.quickbuf.ProtoSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Builds the current Java model render target from a model container view. */
public final class ModelRenderTargetLoader {
    private static final int CURRENT_RAW_UV_VERSION = 29;

    private final Executor workers;
    private final BuildStage buildStage;
    private final DefaultAnimationRuntime defaultAnimations;

    public ModelRenderTargetLoader(BakedModelCache bakedModels, BakedAnimationCache bakedAnimations,
                              Executor workers, DefaultAnimationRuntime defaultAnimations) {
        this.defaultAnimations = defaultAnimations;
        this.workers = workers;
        this.buildStage = new DefaultBuildStage(
                bakedModels, bakedAnimations, workers, defaultAnimations);
    }

    ModelRenderTargetLoader(Executor workers, BuildStage buildStage) {
        this.workers = workers;
        this.buildStage = buildStage;
        this.defaultAnimations = new DefaultAnimationRuntime();
    }

    public CompletableFuture<ModelRenderTarget> loadTarget(TaskContext context,
                                                       ModelDescriptor descriptor, ChunkDataSource chunks,
                                                       String targetId,
                                                       String requestedTexture, boolean prewarmAnimations,
                                                       boolean defaultModel,
                                                       ModelResourceFailures resourceFailures) {
        final LoadRequest request;
        try {
            requireActive(context);
            var view = descriptor.view();
            var target = view.requireRenderTarget(targetId);
            var selectedTexture = ModelAssetPlan.chooseTexture(view.getManifest(), targetId, requestedTexture);
            request = new LoadRequest(descriptor, view, target, target.textureDescriptor(selectedTexture),
                    target.textureSources(chunks, selectedTexture), targetId, selectedTexture,
                    prewarmAnimations, defaultModel, resourceFailures);
            requireActive(context);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new UncheckedIOException(e));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }

        final CompletableFuture<ModelDataOuterClass.ModelData> definition;
        final CompletableFuture<StringDataOuterClass.StringData> commonStrings;
        try {
            definition = request.target().readDefinition(context, chunks);
            commonStrings = readCommonStrings(context, request.view(), chunks);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return buildLoaded(context, definition.thenCombine(commonStrings, (modelData, strings) ->
                new LoadedTarget(request, modelData, strings)));
    }

    CompletableFuture<ModelRenderTarget> buildLoaded(TaskContext context,
                                                     CompletableFuture<LoadedTarget> loaded) {
        return loaded.thenApplyAsync(input -> {
            requireActive(context);
            try {
                return buildStage.build(context, input);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, workers);
    }

    private static CompletableFuture<StringDataOuterClass.StringData> readCommonStrings(
            TaskContext context, ModelFileView view, ChunkDataSource chunks) {
        var common = view.getManifest().getCommonBehavior();
        if (common.hasStringsBlobId() && common.getStringsBlobId() > 0) {
            return view.getCommon().readStringData(context, chunks);
        }
        return CompletableFuture.completedFuture(null);
    }

    private static void requireActive(TaskContext context) {
        if (context.cancelled()) {
            throw new CancellationException("Model render target load was cancelled");
        }
    }

    @FunctionalInterface
    interface BuildStage {
        ModelRenderTarget build(TaskContext context, LoadedTarget input) throws IOException;
    }

    record LoadRequest(ModelDescriptor descriptor, ModelFileView view, RenderTargetView target,
                       Texture.PBRTextureSet textureProto, PBRImageSources textureSources,
                       String targetId, String selectedTexture, boolean prewarmAnimations,
                       boolean defaultModel, ModelResourceFailures resourceFailures) {
    }

    record LoadedTarget(LoadRequest request, ModelDataOuterClass.ModelData definition,
                        StringDataOuterClass.StringData commonStrings) {
    }

    private record DefaultBuildStage(BakedModelCache bakedModels, BakedAnimationCache bakedAnimations,
                                     Executor workers, DefaultAnimationRuntime defaultAnimations)
            implements BuildStage {
        @Override
        public ModelRenderTarget build(TaskContext context, LoadedTarget input) throws IOException {
            var request = input.request();
            var descriptor = request.descriptor();
            var view = request.view();
            var target = request.target();
            var textureProto = request.textureProto();
            var textureSources = request.textureSources();
            var targetId = request.targetId();
            var selectedTexture = request.selectedTexture();
            var definition = input.definition();
            requireActive(context);
                    return buildNow(context, descriptor, view, target, textureProto, textureSources, targetId,
                            selectedTexture, definition, input.commonStrings(), request.prewarmAnimations(),
                            request.defaultModel(), request.resourceFailures());
        }

        private ModelRenderTarget buildNow(TaskContext context, ModelDescriptor descriptor, ModelFileView view,
                                           RenderTargetView target, Texture.PBRTextureSet textureProto,
                                           PBRImageSources textureSources, String targetId, String selectedTexture,
                                            ModelDataOuterClass.ModelData definition,
                                            StringDataOuterClass.StringData commonStrings,
                                            boolean prewarmAnimations,
                                            boolean defaultModel,
                                            ModelResourceFailures resourceFailures) throws IOException {
            try (var resources = new ResourceTransaction();
                 var texturePixels = new LazyTexturePixels(textureSources)) {
                var textureResource = targetId + "/" + selectedTexture + "/";
                var texture = resources.own(new CustomPBRTextureSet(textureSources, workers,
                        component -> resourceFailures.texture(textureResource + component)));
                var textureHash = textureHash(view, textureProto.getUv());
                var definitionHash = definitionHash(view, target.descriptor().getBlobId());
                var controllers = controllerFiles(definition);
                Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles =
                        definition.hasAnimationFiles() ? definition.getAnimationFiles() : List.of();
                final RenderTargetData targetData;
                switch (target.kind()) {
                case RENDER_TARGET_KIND_PLAYER -> {
                    var main = resources.own(bakeModel(descriptor, targetId + "/" + selectedTexture + "/main",
                            textureHash, geoModel(definition, "main"), texturePixels, target, textureProto,
                            defaultModel));
                    var arm = resources.own(bakeModel(descriptor, targetId + "/" + selectedTexture + "/arm",
                            textureHash, geoModel(definition, "arm"), texturePixels, target, textureProto,
                            defaultModel));
                    var mainFiles = new ArrayList<ModelDataOuterClass.ModelData.AnimationFilesEntry>();
                    var firstPersonFiles = new ArrayList<ModelDataOuterClass.ModelData.AnimationFilesEntry>();
                    for (var file : animationFiles) {
                        (file.getKey().equals("fp_arm") ? firstPersonFiles : mainFiles).add(file);
                    }
                    var animations = resources.own(loadAnimations(descriptor, target.descriptor(), targetId,
                            "main", definitionHash, mainFiles, prewarmAnimations, defaultModel,
                            resourceFailures));
                    var firstPersonAnimations = resources.own(loadAnimations(descriptor, target.descriptor(),
                            targetId, "fp_arm", definitionHash, firstPersonFiles,
                            prewarmAnimations, defaultModel, resourceFailures));
                    var variants = new FifoHashMap<>(new String[]{selectedTexture},
                            new PlayerModelVariant[]{new PlayerModelVariant(main, arm, texture)});
                    targetData = new PlayerModelData(variants, animations, firstPersonAnimations,
                            List.copyOf(controllers.values()));
                }
                case RENDER_TARGET_KIND_PROJECTILE, RENDER_TARGET_KIND_VEHICLE -> {
                    var baked = resources.own(bakeModel(descriptor, targetId + "/" + selectedTexture,
                            textureHash, geoModel(definition, "main"), texturePixels, target, textureProto,
                            defaultModel));
                    var animations = resources.own(loadAnimations(descriptor, target.descriptor(), targetId,
                            "main", definitionHash, animationFiles, prewarmAnimations,
                            defaultModel, resourceFailures));
                    var controller = controllers.values().stream().findFirst().orElse(null);
                    targetData =
                            target.kind() == RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PROJECTILE
                                    ? new ProjectileModelData(baked, animations, controller, texture)
                                    : new VehicleModelData(baked, animations, controller, texture);
                }
                default -> throw new IOException("Unsupported render target kind: " + target.kind());
                }

                var common = commonAssets(view, commonStrings);
                var data = new ModelRenderTargetBuildInput(targetId, targetData, common,
                        ManifestModelInfoMapper.map(descriptor.modelHash(), view.getManifest()));
                var result = ModelRenderTargetAssembler.build(descriptor.modelHash(), data);
                requireActive(context);
                resources.commit();
                return result;
            }
        }

        private static CommonAssetData commonAssets(ModelFileView view,
                                                    StringDataOuterClass.StringData source) {
            var functions = new Object2ReferenceOpenHashMap<String,
                    IValue>();
            if (source != null && source.hasUserFunctions()) {
                var parser = CustomMolangParser.rentInstance();
                try {
                    for (var function : source.getUserFunctions()) {
                        functions.put(function.getName(), parser.parseExpression(function.getContent(), false));
                    }
                } finally {
                    CustomMolangParser.returnInstance(parser);
                }
            }
            var languages = new LinkedHashMap<String, Map<String, String>>();
            var info = view.getManifest().getInfo();
            if (info.hasLanguageFiles()) {
                for (var language : info.getLanguageFiles()) {
                    var values = new LinkedHashMap<String, String>();
                    if (language.hasEntries()) {
                        language.getEntries().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
                    }
                    languages.put(language.getLocale(), Map.copyOf(values));
                }
            }
            return new CommonAssetData(Map.of(), functions, Map.copyOf(languages));
        }

        private static Map<String, AnimationControllerFile> controllerFiles(
                ModelDataOuterClass.ModelData source) {
            var result = new LinkedHashMap<String, AnimationControllerFile>();
            if (source.hasAnimationControllers()) {
                source.getAnimationControllers().forEach(entry -> result.put(entry.getKey(),
                        AnimationProtoMapper.controllerFile(entry.getValue())));
            }
            return result;
        }

        private GeoModel bakeModel(ModelDescriptor descriptor, String resourceName, byte[] textureHash,
                                   GeoModelOuterClass.GeoModel geo,
                                   BakedModelCache.TexturePixelsSupplier texturePixels,
                                   RenderTargetView target,
                                   Texture.PBRTextureSet textureProto,
                                   boolean defaultModel) throws IOException {
            if (defaultModel) {
                return BakedModelCache.bakeResident(geo, texturePixels,
                        CURRENT_RAW_UV_VERSION,
                        target.descriptor().getSettings().getForceCulling(),
                        false, hasPbr(textureProto), PlayerLocator.get());
            }
            return bakedModels.loadOrBake(descriptor.modelHash(), descriptor.descriptorHash(),
                    resourceName, textureHash, geo, texturePixels, CURRENT_RAW_UV_VERSION,
                    target.descriptor().getSettings().getForceCulling(), false,
                    hasPbr(textureProto), PlayerLocator.get());
        }

        private AnimationStore loadAnimations(ModelDescriptor descriptor,
                                              RenderTargetOuterClass.RenderTarget target,
                                              String targetId, String animationSet,
                                              ModelHash definitionHash,
                                              Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry>
                                                      animationFiles,
                                              boolean prewarm, boolean defaultModel,
                                              ModelResourceFailures resourceFailures) throws IOException {
            if (defaultModel) {
                return BakedAnimationCache.bindResident(animationFiles,
                        (animation, bound) -> defaultAnimations.requireCurrent(
                                target, animationSet, animation));
            }
            var domain = DefaultAnimationKey.domain(target, animationSet);
            var fallback = defaultAnimations.fallback(domain);
            var animations = bakedAnimations.loadOrBake(descriptor.modelHash(), descriptor.descriptorHash(),
                    targetId, animationSet, definitionHash, animationFiles, fallback,
                    name -> resourceFailures.animation(targetId + "/" + animationSet + "/" + name));
            if (prewarm) {
                for (var animation : List.copyOf(animations.keySet())) {
                    animations.get(animation);
                }
            }
            return animations;
        }
    }

    static GeoModelOuterClass.GeoModel geoModel(ModelDataOuterClass.ModelData source, String name)
            throws IOException {
        for (var entry : source.getGeoModels()) {
            if (entry.getKey().equals(name)) {
                return GeoModelOuterClass.GeoModel.parseFrom(ProtoSource.newInstance(entry.getValue()));
            }
        }
        throw new FileNotFoundException("Model data contains no geo model named " + name);
    }

    private static byte[] textureHash(ModelFileView view,
                                      ImageOuterClass.Image image)
            throws IOException {
        var chunk = view.getFileView().getAssetView().getChunkInfo(AssetFileConstant.BLOB_CHUNK_PREFIX
                + image.getBlobId());
        if (chunk == null || chunk.hash() == null || chunk.hash().length != ModelHash.SIZE) {
            throw new IOException("Texture chunk contains no content hash");
        }
        return chunk.hash();
    }

    private static ModelHash definitionHash(ModelFileView view, int blobId) throws IOException {
        var chunk = view.getFileView().getAssetView().getChunkInfo(AssetFileConstant.BLOB_CHUNK_PREFIX + blobId);
        if (chunk == null || chunk.hash() == null || chunk.hash().length != ModelHash.SIZE) {
            throw new IOException("Render target definition contains no content hash");
        }
        return new ModelHash(chunk.hash());
    }

    private static boolean hasPbr(Texture.PBRTextureSet texture) {
        return texture.hasNormal() || texture.hasSpecular();
    }

    private static final class LazyTexturePixels
            implements BakedModelCache.TexturePixelsSupplier, AutoCloseable {
        private final PBRImageSources sources;
        private NativeImage pixels;

        private LazyTexturePixels(PBRImageSources sources) {
            this.sources = sources;
        }

        @Override
        public NativeImage get() throws IOException {
            if (pixels == null) {
                try (var image = sources.uv().open()) {
                    pixels = image.decode();
                }
            }
            return pixels;
        }

        @Override
        public void close() {
            if (pixels != null) {
                pixels.close();
                pixels = null;
            }
        }
    }
}
