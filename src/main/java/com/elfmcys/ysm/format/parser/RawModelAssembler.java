package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.parser.pojo.animation.AnimationFile;
import com.elfmcys.ysm.format.parser.pojo.controller.AnimationControllerFile;
import com.elfmcys.ysm.format.parser.pojo.manifest.ModelManifest;
import com.elfmcys.ysm.format.parser.pojo.manifest.models.PBRTextureSet;
import com.elfmcys.ysm.format.parser.pojo.manifest.models.PlayerModelFiles;
import com.elfmcys.ysm.format.parser.pojo.manifest.models.ReplacedModelFiles;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ModelProperties;
import com.elfmcys.ysm.format.parser.pojo.model.GeoModel;
import com.elfmcys.ysm.format.schema.model.ModelFileConstant;
import com.elfmcys.ysm.format.schema.model.ModelFileWriter;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.natives.image.Image;
import mixel.common.ImageOuterClass;
import mixel.common.StringPairOuterClass;
import mixel.asset.model.ModelDataOuterClass;
import mixel.asset.model.data.AnimationControllerOuterClass;
import mixel.asset.model.data.AnimationOuterClass;
import mixel.asset.strings.StringDataOuterClass;
import mixel.asset.strings.data.UserFunc;
import mixel.manifest.ManifestOuterClass;
import mixel.manifest.asset.RenderTargetOuterClass;
import mixel.manifest.asset.Texture;
import mixel.manifest.info.InfoOuterClass;
import mixel.manifest.info.LanguageFileOuterClass;
import com.elfmcys.ysm.util.Closeable;
import com.elfmcys.ysm.util.ProtoUtil;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

final class RawModelAssembler implements Closeable {
    private static final String MANIFEST_FILE_NAME = "ysm.json";

    static final String[] PLAYER_MAIN_ANIMATION_TYPES = {
            "main", "arm", "fp_arm", "extra", "tac", "carryon", "parcool", "swem",
            "slashblade", "tlm", "immersive_melodies", "irons_spell_books"
    };

    private final Gson gson = new Gson();
    private final RawModelSource source;
    private final boolean dryRun;
    private final DefaultAnimationFilter defaultAnimationFilter;
    private final boolean recompressImages;
    @Nullable
    private final Path dstDir;
    @Nullable
    private final ModelFileWriter writer;
    @Nullable
    private final RawImageCompressor imageCompressor;

    private ModelProperties activeProperties;

    RawModelAssembler(RawModelSource source, @Nullable Path dstDir, boolean dryRun,
                      DefaultAnimationFilter defaultAnimationFilter, boolean recompressImages) {
        this.source = Objects.requireNonNull(source, "source");
        this.dryRun = dryRun;
        this.defaultAnimationFilter = Objects.requireNonNull(
                defaultAnimationFilter, "defaultAnimationFilter");
        this.recompressImages = recompressImages;
        this.dstDir = dryRun ? null : Objects.requireNonNull(dstDir, "dstDir");
        this.writer = dryRun ? null : new ModelFileWriter();
        this.imageCompressor = dryRun || !recompressImages ? null : new RawImageCompressor();
    }

    Result parse() {
        try {
            var sourceManifest = readManifest();
            activeProperties = sourceManifest.properties;

            var manifest = dryRun ? null : ManifestOuterClass.Manifest.newInstance();
            writePlayerModel(manifest, sourceManifest.files.player, sourceManifest.properties);
            ModelInfoBuilder.writeInfo(manifest, sourceManifest,
                    ModelFileConstant.CURRENT_VERSION.toString(), this::readImageBlob);
            writeReplacedModels(manifest, sourceManifest);
            writeCommonAssets(manifest, sourceManifest);
            writeContainerImages(manifest, sourceManifest.properties);

            var modelHash = new ModelHash(source.aggregateHash());
            if (dryRun) {
                return new Result(modelHash, null);
            }
            var hashId = modelHash.toString();
            Objects.requireNonNull(manifest, "manifest")
                    .getMutableInfo().getMutableProperties().setHashId(modelHash.bytes());

            writer().setManifest(manifest);
            Files.createDirectories(Objects.requireNonNull(dstDir, "dstDir"));
            var output = dstDir.resolve(hashId + ".mxc");
            try (var channel = FileChannel.open(output,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                writer().write(channel);
            }
            return new Result(modelHash, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ModelManifest readManifest() throws IOException {
        var manifest = source.readJson(MANIFEST_FILE_NAME, "manifest", gson, ModelManifest.class, false)
                .orElseGet(() -> {
                    try {
                        return LegacyManifestBuilder.build(source, gson);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        manifest.normalize();
        return manifest;
    }

    private void writePlayerModel(@Nullable ManifestOuterClass.Manifest manifest,
                                  PlayerModelFiles files, ModelProperties properties)
            throws IOException {
        if (files == null) {
            throw new IOException("Missing player model files");
        }
        var main = readGeoModel(Objects.requireNonNull(files.model.get("main"), "Missing main model path"), true)
                .orElse(null);
        var arm = readGeoModel(Objects.requireNonNull(files.model.get("arm"), "Missing arm model path"), true)
                .orElse(null);
        var playerData = dryRun ? null : ModelDataOuterClass.ModelData.newInstance();

        if (!dryRun) {
            var entry = ModelDataOuterClass.ModelData.GeoModelsEntry.newInstance()
                    .setKey("main");
            entry.getMutableValue().setInternalArray(ProtoUtil.serializeToArray(
                    Objects.requireNonNull(main, "main").model));
            Objects.requireNonNull(playerData, "playerData").addAllGeoModels(entry);

            entry = ModelDataOuterClass.ModelData.GeoModelsEntry.newInstance()
                    .setKey("arm");
            entry.getMutableValue().setInternalArray(ProtoUtil.serializeToArray(
                    Objects.requireNonNull(arm, "arm").model));
            playerData.addAllGeoModels(entry);

            if (properties.heightScale == 0.7f && main.heightScale != 0.7f) {
                properties.heightScale = main.heightScale;
            }
            if (properties.widthScale == 0.7f && main.widthScale != 0.7f) {
                properties.widthScale = main.widthScale;
            }
        }

        var defaultTexture = properties.defaultTexture;

        for (var animationType : PLAYER_MAIN_ANIMATION_TYPES) {
            var path = files.animation.get(animationType);
            if (path != null) {
                var animation = readAnimation(path, true).orElse(null);
                if (!dryRun) {
                    var target = RenderTargetOuterClass.RenderTarget.newInstance()
                            .setTargetId("player")
                            .setKind(RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER);
                    animation = filterAnimations(target, animationType,
                            Objects.requireNonNull(animation, "animation"));
                    if (!animation.hasAnimations()) {
                        continue;
                    }
                    var entry = ModelDataOuterClass.ModelData.AnimationFilesEntry.newInstance();
                    entry.setKey(animationType);
                    entry.setValue(Objects.requireNonNull(animation, "animation"));
                    Objects.requireNonNull(playerData, "playerData").addAnimationFiles(entry);
                }
            }
        }

        for (var controllerFile : files.animationControllers) {
            var controller = readAnimationController(controllerFile);
            if (!dryRun) {
                var name = fileNameWithoutExtension(controllerFile);
                Objects.requireNonNull(playerData, "playerData").addAnimationControllers(
                        ModelDataOuterClass.ModelData.AnimationControllersEntry.newInstance()
                                .setKey(name).setValue(Objects.requireNonNull(controller, "controller")));
            }
        }
        var player = dryRun ? null : RenderTargetOuterClass.RenderTarget.newInstance()
                .setTargetId("player")
                .setKind(RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER);
        for (var texture : files.texture) {
            var textureSet = readTextureSet(texture);
            if (!dryRun) {
                var textureName = textureName(texture.uv);
                if (defaultTexture.isEmpty()) {
                    defaultTexture = textureName;
                    properties.defaultTexture = textureName;
                }
                Objects.requireNonNull(player, "player").addTextures(
                        RenderTargetOuterClass.RenderTarget.TexturesEntry.newInstance()
                                .setKey(textureName)
                                .setValue(Objects.requireNonNull(textureSet, "textureSet")));
            }
        }

        if (!dryRun) {
            Objects.requireNonNull(player, "player")
                    .setSettings(ModelInfoBuilder.modelSettings(properties))
                    .setStats(Objects.requireNonNull(main, "main").stats.toProto())
                    .setBlobId(writer().addProtoBlob(Objects.requireNonNull(playerData, "playerData"), 16));
            Objects.requireNonNull(manifest, "manifest").addRenderTargets(player);
        }
    }

    @SuppressWarnings("deprecation")
    private void writeReplacedModels(@Nullable ManifestOuterClass.Manifest manifest,
                                     ModelManifest sourceManifest) throws IOException {
        var projectileIndex = 0;
        for (var model : sourceManifest.files.projectiles.list) {
            writeReplacedModel(manifest, model, "projectile/%04d".formatted(projectileIndex++),
                    RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PROJECTILE);
        }
        if (sourceManifest.files.arrow != null) {
            writeReplacedModel(manifest, sourceManifest.files.arrow, "projectile/%04d".formatted(projectileIndex),
                    RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PROJECTILE);
        }
        var vehicleIndex = 0;
        for (var model : sourceManifest.files.vehicles.list) {
            writeReplacedModel(manifest, model, "vehicle/%04d".formatted(vehicleIndex++),
                    RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_VEHICLE);
        }
    }

    private void writeReplacedModel(@Nullable ManifestOuterClass.Manifest manifest,
                                    ReplacedModelFiles files, String targetId,
                                    RenderTargetOuterClass.RenderTargetKind kind)
            throws IOException {
        if (files == null || files.match == null || files.match.isEmpty()) {
            return;
        }
        var geo = readGeoModel(files.model, true).orElse(null);
        AnimationOuterClass.AnimationFile animation = null;
        if (!StringUtils.isBlank(files.animation)) {
            animation = readAnimation(files.animation, true).orElse(null);
        }
        AnimationControllerOuterClass.AnimationControllerFile controller = null;
        if (!StringUtils.isBlank(files.controller)) {
            controller = readAnimationController(files.controller);
        }

        RenderTargetOuterClass.RenderTarget replaced = null;
        if (!dryRun) {
            replaced = RenderTargetOuterClass.RenderTarget.newInstance()
                    .setTargetId(targetId)
                    .setKind(kind);
            for (var match : files.match) {
                replaced.addMatch(match);
            }
            var data = ModelDataOuterClass.ModelData.newInstance();
            var entry = ModelDataOuterClass.ModelData.GeoModelsEntry.newInstance()
                    .setKey("main");
            entry.getMutableValue().setInternalArray(ProtoUtil.serializeToArray(
                    Objects.requireNonNull(geo, "geo").model));
            data.addGeoModels(entry);
            if (animation != null) {
                animation = filterAnimations(replaced, "main", animation);
            }
            if (animation != null && animation.hasAnimations()) {
                data.addAnimationFiles(ModelDataOuterClass.ModelData.AnimationFilesEntry.newInstance()
                        .setKey("main").setValue(animation));
            }
            if (controller != null) {
                data.addAnimationControllers(ModelDataOuterClass.ModelData.AnimationControllersEntry.newInstance()
                        .setKey("main").setValue(controller));
            }

            replaced.setBlobId(writer().addProtoBlob(data, 16))
                    .setSettings(ModelInfoBuilder.modelSettings(activeProperties))
                    .setStats(geo.stats.toProto());
        }

        var texture = readTextureSet(files.texture);
        if (dryRun) {
            return;
        }
        Objects.requireNonNull(replaced, "replaced").addTextures(
                RenderTargetOuterClass.RenderTarget.TexturesEntry.newInstance()
                .setKey("default").setValue(Objects.requireNonNull(texture, "texture")));
        Objects.requireNonNull(manifest, "manifest").addRenderTargets(replaced);
    }

    private AnimationOuterClass.AnimationFile filterAnimations(
            RenderTargetOuterClass.RenderTarget target, String animationSet,
            AnimationOuterClass.AnimationFile animations) throws IOException {
        return defaultAnimationFilter.apply(target, animationSet, animations);
    }

    @SuppressWarnings("deprecation")
    private void writeCommonAssets(@Nullable ManifestOuterClass.Manifest manifest,
                                   ModelManifest sourceManifest) throws IOException {
        var files = sourceManifest.files;
        if (!StringUtils.isBlank(files.soundPath)) {
            writeSounds(files.soundPath);
        } else if (files.player != null && !StringUtils.isBlank(files.player.soundPath)) {
            writeSounds(files.player.soundPath);
        } else {
            writeSounds("sounds");
        }

        var strings = dryRun ? null : StringDataOuterClass.StringData.newInstance();
        writeFunctions(strings, StringUtils.isBlank(files.functionPath) ? "functions" : files.functionPath);
        writeLanguageFiles(manifest, StringUtils.isBlank(files.languagePath) ? "lang" : files.languagePath);
        if (!dryRun) {
            Objects.requireNonNull(manifest, "manifest").getMutableCommonBehavior()
                    .setStringsBlobId(writer().addProtoBlob(Objects.requireNonNull(strings, "strings"), 16));
        }
    }

    private void writeContainerImages(@Nullable ManifestOuterClass.Manifest manifest,
                                      ModelProperties properties)
            throws IOException {
        readImage(properties.icon, "icon", false, RawImageCompressor.MODEL_ICON,
                dryRun ? null : writer()::setIcon);
        if (readImage(properties.thumbnail, "thumbnail", false,
                RawImageCompressor.MODEL_THUMBNAIL, dryRun ? null : writer()::setThumbnail) && !dryRun) {
            Objects.requireNonNull(manifest, "manifest").getMutableInfo()
                    .setThumbnailSource(InfoOuterClass.PreviewSource.PREVIEW_SOURCE_RAW);
        }
    }

    @SuppressWarnings("SameParameterValue")
    Optional<GeoBuilder.Result> readGeoModel(String fileName, boolean required) throws IOException {
        if (dryRun) {
            source.readFile(fileName, "model", required);
            return Optional.empty();
        }
        return source.readJson(fileName, "model", gson, GeoModel.class, required)
                .map(m -> {
                    try {
                        return GeoBuilder.build(m);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @SuppressWarnings("SameParameterValue")
    Optional<AnimationOuterClass.AnimationFile> readAnimation(String fileName, boolean required) throws IOException {
        if (dryRun) {
            source.readFile(fileName, "animation", required);
            return Optional.empty();
        }
        return source.readJson(fileName, "animation",
                AnimationFile.createGson(activeProperties != null && activeProperties.mergeMultilineExpr),
                AnimationFile.class, required)
                .map(AnimationBuilder::build);
    }

    @Nullable
    private AnimationControllerOuterClass.AnimationControllerFile readAnimationController(String fileName) throws IOException {
        if (dryRun) {
            source.readFile(fileName, "controller", true);
            return null;
        }
        return source.readJson(fileName, "controller", gson, AnimationControllerFile.class, true)
                .map(AnimationControllerBuilder::build)
                .orElseThrow();
    }

    @Nullable
    private Texture.PBRTextureSet readTextureSet(PBRTextureSet src) throws IOException {
        if (src == null || StringUtils.isBlank(src.uv)) {
            throw new IOException("Missing texture uv");
        }
        var dst = dryRun ? null : Texture.PBRTextureSet.newInstance();
        readImageBlob(src.uv, "texture", RawImageCompressor.LOSSLESS_TEXTURE,
                dst == null ? null : dst::setUv);
        if (!StringUtils.isBlank(src.normal)) {
            readImageBlob(src.normal, "texture/normal", RawImageCompressor.LOSSLESS_TEXTURE,
                    dst == null ? null : dst::setNormal);
        }
        if (!StringUtils.isBlank(src.specular)) {
            readImageBlob(src.specular, "texture/specular", RawImageCompressor.LOSSLESS_TEXTURE,
                    dst == null ? null : dst::setSpecular);
        }
        return dst;
    }

    void readImageBlob(String path, String hashType, RawImageCompressor.Policy policy,
                       @Nullable Consumer<ImageOuterClass.Image> consumer) throws IOException {
        readImage(path, hashType, true, policy, img -> {
            var blobId = writer().addBlob(img.data(), 0);
            Objects.requireNonNull(consumer, "consumer").accept(ImageOuterClass.Image.newInstance()
                    .setBlobId(blobId)
                    .setFormat(img.format().name())
                    .setWidth(img.width())
                    .setHeight(img.height())
                    .setFrameCount(1));
        });
    }

    boolean readImage(String path, String type, boolean required, RawImageCompressor.Policy policy,
                      @Nullable Consumer<Image> consumer) throws IOException {
        var data = source.readFile(path, type, required);
        if (data.isEmpty()) {
            return false;
        }
        if (dryRun) {
            return true;
        }
        try (var original = Image.probe(data.orElseThrow())) {
            try (var stored = recompressImages
                    ? imageCompressor().compress(original, policy, path)
                    : original.share()) {
                Objects.requireNonNull(consumer, "consumer").accept(stored);
            }
        }
        return true;
    }

    private void writeSounds(String path) {
        // TODO
//        collectFiles(path, ".ogg", "sound", true, (name, buf) -> {
//
//        });
    }

    private void writeFunctions(@Nullable StringDataOuterClass.StringData strings, String path) {
        source.collectFiles(path, ".molang", "molang-func", true, (name, buf) -> {
            if (!dryRun) {
                Objects.requireNonNull(strings, "strings")
                        .addUserFunctions(UserFunc.UserFunction.newInstance()
                                .setName(name)
                                .setContent(RawModelSource.readUtf8(buf)));
            }
        });
    }

    private void writeLanguageFiles(@Nullable ManifestOuterClass.Manifest manifest, String path) {
        source.collectFiles(path, ".json", "language", true, (name, buf) -> {
            if (!dryRun) {
                var object = JsonParser.parseString(RawModelSource.readUtf8(buf)).getAsJsonObject();
                var language = LanguageFileOuterClass.LanguageFile.newInstance()
                        .setLocale(name);
                for (var entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                        language.addEntries(stringPair(entry.getKey(), entry.getValue().getAsString()));
                    }
                }
                Objects.requireNonNull(manifest, "manifest").getMutableInfo().addLanguageFiles(language);
            }
        });
    }

    private ModelFileWriter writer() {
        return Objects.requireNonNull(writer, "writer");
    }

    private RawImageCompressor imageCompressor() {
        return Objects.requireNonNull(imageCompressor, "imageCompressor");
    }

    private static StringPairOuterClass.StringPair stringPair(String key, String value) {
        return StringPairOuterClass.StringPair.newInstance()
                .setKey(key)
                .setValue(value);
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        var normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String joinPath(String dir, String name) {
        if (StringUtils.isBlank(dir)) {
            return name;
        }
        return dir + "/" + name;
    }

    private static String textureName(String path) {
        return removeExtension(baseName(path));
    }

    private static String fileNameWithoutExtension(String path) {
        return removeExtension(baseName(path));
    }

    private static String baseName(String path) {
        var normalized = normalizePath(path);
        var index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private static String removeExtension(String path) {
        var index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(0, index) : path;
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }

    record Result(ModelHash modelHash, @Nullable Path output) {
    }
}
