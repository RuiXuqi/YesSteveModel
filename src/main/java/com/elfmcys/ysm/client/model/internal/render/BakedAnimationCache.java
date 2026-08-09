package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.model.AnimationStore;
import com.elfmcys.ysm.client.model.ModelResourceFailureGate;
import com.elfmcys.ysm.format.schema.baked.asset.BakedAssetView;
import com.elfmcys.ysm.format.schema.baked.asset.BakedAssetWriter;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.storage.AtomicSharedCache;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.model.storage.SharedCachePaths;
import mixel.asset.model.ModelDataOuterClass;
import mixel.asset.model.data.AnimationOuterClass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.function.Function;

/** Process-shared immutable baked-animation cache; loaded bindings remain animation-set-scoped. */
public final class BakedAnimationCache {
    static final String CACHE_SUFFIX = ".anim.ysm-cache";
    private static final String CACHE_ABI = "animation-0.1.0-unstable";
    private static final byte[] HASH_DOMAIN =
            "ysm.animation-set.0.1.0-unstable\0".getBytes(StandardCharsets.UTF_8);

    private final SharedCachePaths paths;
    private final AtomicSharedCache cache;

    public BakedAnimationCache(SharedCachePaths paths, AtomicSharedCache cache) {
        this.paths = paths;
        this.cache = cache;
    }

    public AnimationStore loadOrBake(Hash256 modelHash, Hash256 descriptorHash, String targetId,
                                     String animationSet, Hash256 definitionHash,
                                     Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles)
            throws IOException {
        return loadOrBake(modelHash, descriptorHash, targetId, animationSet,
                definitionHash, animationFiles, null);
    }

    public AnimationStore loadOrBake(Hash256 modelHash, Hash256 descriptorHash, String targetId,
                                     String animationSet, Hash256 definitionHash,
                                     Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles,
                                     AnimationStore fallback) throws IOException {
        return loadOrBake(modelHash, descriptorHash, targetId, animationSet, definitionHash,
                animationFiles, fallback, ignored -> ModelResourceFailureGate.none());
    }

    public AnimationStore loadOrBake(Hash256 modelHash, Hash256 descriptorHash, String targetId,
                                     String animationSet, Hash256 definitionHash,
                                     Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles,
                                     AnimationStore fallback,
                                     Function<String, ModelResourceFailureGate> failureGates) throws IOException {
        var files = new ArrayList<ModelDataOuterClass.ModelData.AnimationFilesEntry>();
        animationFiles.forEach(files::add);
        files.sort(Comparator.comparing(ModelDataOuterClass.ModelData.AnimationFilesEntry::getKey));
        if (files.isEmpty()) {
            return fallback == null ? new AnimationStore()
                    : AnimationStore.emptyWithFallback(fallback);
        }
        var animations = flatten(files);
        var setHash = animationSetHash(definitionHash, animationSet);
        var target = path(modelHash, descriptorHash, setHash);
        cache.materialize("baked-animation", modelHash + "/" + targetId + "/" + animationSet + "/" + setHash,
                target, candidate -> validate(candidate, modelHash, descriptorHash, targetId, setHash),
                candidate -> write(candidate, modelHash, descriptorHash, targetId, setHash, animations));
        var view = open(target);
        if (!view.matches(modelHash, descriptorHash, targetId, setHash)) {
            throw new IOException("Baked animation cache identity mismatch");
        }
        return view.createAnimationStore(() -> FileChannel.open(target, StandardOpenOption.READ),
                AnimationProtoMapper::animation, fallback, failureGates);
    }

    public static AnimationStore bindResident(
            Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles)
            throws IOException {
        return bindResident(animationFiles, (animation, bound) -> { });
    }

    public static AnimationStore bindResident(
            Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles,
            BoundAnimationObserver observer) throws IOException {
        var bound = new LinkedHashMap<String,
                com.elfmcys.ysm.geckolib3.core.builder.Animation>();
        for (var animation : flatten(animationFiles)) {
            var value = AnimationProtoMapper.animation(animation);
            observer.accept(animation, value);
            bound.put(animation.getName(), value);
        }
        return AnimationStore.eager(bound);
    }

    @FunctionalInterface
    public interface BoundAnimationObserver {
        void accept(AnimationOuterClass.Animation source,
                    com.elfmcys.ysm.geckolib3.core.builder.Animation bound)
                throws IOException;
    }

    private Path path(Hash256 modelHash, Hash256 descriptorHash, Hash256 definitionHash) {
        return paths.baked().resolve(CACHE_ABI).resolve(modelHash.toString())
                .resolve(descriptorHash.toString()).resolve(definitionHash + CACHE_SUFFIX);
    }

    static ArrayList<AnimationOuterClass.Animation> flatten(
            Iterable<ModelDataOuterClass.ModelData.AnimationFilesEntry> animationFiles) throws IOException {
        var result = new ArrayList<AnimationOuterClass.Animation>();
        var names = new HashSet<String>();
        for (var file : animationFiles) {
            for (var animation : file.getValue().getAnimations()) {
                if (animation.getName().isBlank() || !names.add(animation.getName())) {
                    throw new IOException("Duplicate or empty animation name in animation set: "
                            + animation.getName());
                }
                result.add(animation);
            }
        }
        result.sort(Comparator.comparing(AnimationOuterClass.Animation::getName));
        return result;
    }

    static Hash256 animationSetHash(Hash256 definitionHash, String animationSet) {
        var setName = animationSet.getBytes(StandardCharsets.UTF_8);
        var input = ByteBuffer.allocate(HASH_DOMAIN.length + Hash256.SIZE + Integer.BYTES + setName.length)
                .put(HASH_DOMAIN).put(definitionHash.bytes())
                .putInt(setName.length).put(setName);
        return ModelHashing.blake3(input.array());
    }

    private static void write(Path file, Hash256 modelHash, Hash256 descriptorHash, String targetId,
                              Hash256 definitionHash, Iterable<AnimationOuterClass.Animation> animations)
            throws IOException {
        try (var writer = new BakedAssetWriter();
             var channel = FileChannel.open(file, StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.setData(modelHash, descriptorHash, targetId, definitionHash, animations);
            writer.write(channel);
            channel.force(true);
        }
    }

    private static boolean validate(Path file, Hash256 modelHash, Hash256 descriptorHash,
                                    String targetId, Hash256 definitionHash) {
        try {
            return open(file).matches(modelHash, descriptorHash, targetId, definitionHash);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static BakedAssetView open(Path file) throws IOException {
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            return new BakedAssetView(channel);
        }
    }
}
