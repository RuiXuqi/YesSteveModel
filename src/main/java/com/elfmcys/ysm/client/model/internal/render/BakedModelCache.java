package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.format.schema.baked.model.BakedModelConstant;
import com.elfmcys.ysm.format.schema.baked.model.BakedModelView;
import com.elfmcys.ysm.format.schema.baked.model.BakedModelWriter;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoLocatorType;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.storage.AtomicSharedCache;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.model.storage.SharedCachePaths;
import com.elfmcys.ysm.natives.render.NativeBakedModel;
import mixel.asset.model.data.GeoModelOuterClass;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

public final class BakedModelCache {
    static final String CACHE_SUFFIX = ".geo.ysm-cache";
    private static final String NATIVE_ABI = "renderer-0.1.0-unstable";

    private final SharedCachePaths paths;
    private final AtomicSharedCache cache;
    private final String capabilityKey;

    public BakedModelCache(SharedCachePaths paths, AtomicSharedCache cache) {
        this.paths = paths;
        this.cache = cache;
        this.capabilityKey = sanitize(System.getProperty("os.arch", "unknown")) + "-"
                + sanitize(System.getProperty("os.name", "unknown")) + "-simd-"
                + Integer.toUnsignedString(NativeBakedModel.capability()) + "-bake-"
                + BakedModelConstant.CURRENT_VERSION;
    }

    public GeoModel loadOrBake(ModelHash modelHash, ModelHash descriptorHash, String resourceName,
                               byte[] textureHash, GeoModelOuterClass.GeoModel source,
                               TexturePixelsSupplier texture, int originVersion, boolean forceCulling,
                               boolean forceTranslucent, boolean hasPbr, GeoLocatorType locatorType) throws IOException {
        var bakeHash = bakeHash(descriptorHash, resourceName, textureHash, source.getSerializedSize(),
                originVersion, forceCulling, forceTranslucent, hasPbr);
        var target = paths.baked().resolve(NATIVE_ABI).resolve(capabilityKey)
                .resolve(modelHash.toString()).resolve(bakeHash + CACHE_SUFFIX);
        cache.materialize("baked", modelHash + "/" + bakeHash, target,
                candidate -> validate(candidate, bakeHash),
                candidate -> bake(candidate, bakeHash, source, texture, originVersion,
                        forceCulling, forceTranslucent, hasPbr));
        return read(target, bakeHash, locatorType);
    }

    public static GeoModel bakeResident(GeoModelOuterClass.GeoModel source,
                                        TexturePixelsSupplier textureSource,
                                        int originVersion, boolean forceCulling,
                                        boolean forceTranslucent, boolean hasPbr,
                                        GeoLocatorType locatorType) throws IOException {
        var texture = textureSource.get();
        var baked = NativeBakedModel.bake(source, texture, originVersion,
                forceCulling, forceTranslucent, hasPbr);
        try (var bakedData = baked.bakedData()) {
            var nativeModel = NativeBakedModel.read(
                    bakedData, baked.sortedBoneIndices().length);
            return new GeoModel(source, locatorType, nativeModel);
        }
    }

    private static void bake(Path destination, ModelHash bakeHash, GeoModelOuterClass.GeoModel source,
                             TexturePixelsSupplier textureSource, int originVersion, boolean forceCulling,
                             boolean forceTranslucent, boolean hasPbr) throws IOException {
        var texture = textureSource.get();
        try (var result = NativeBakedModel.bake(source, texture, originVersion,
                forceCulling, forceTranslucent, hasPbr).bakedData();
             var writer = new BakedModelWriter();
             var channel = FileChannel.open(destination, StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.setData(bakeHash.bytes(), source, result);
            writer.write(channel);
            channel.force(true);
        }
    }

    private static GeoModel read(Path file, ModelHash expected, GeoLocatorType locatorType) throws IOException {
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            var view = new BakedModelView(channel);
            if (!Arrays.equals(view.modelHash(), expected.bytes())) {
                throw new IOException("Baked model cache key mismatch");
            }
            var data = view.readModelData(channel);
            if (data == null) {
                throw new IOException("Baked model cache has no native payload");
            }
            try (data) {
                var nativeModel = NativeBakedModel.read(data, view.modelIndex().getBones().length());
                return new GeoModel(view.modelIndex(), locatorType, nativeModel);
            }
        }
    }

    private static boolean validate(Path file, ModelHash expected) {
        try (var channel = FileChannel.open(file, StandardOpenOption.READ)) {
            var view = new BakedModelView(channel);
            if (!Arrays.equals(view.modelHash(), expected.bytes())) {
                return false;
            }
            try (var data = view.readModelData(channel)) {
                return data != null;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static ModelHash bakeHash(ModelHash descriptorHash, String resourceName, byte[] textureHash,
                                      int serializedSize, int originVersion, boolean forceCulling,
                                      boolean forceTranslucent, boolean hasPbr) {
        var name = resourceName.getBytes(StandardCharsets.UTF_8);
        var input = ByteBuffer.allocate(ModelHash.SIZE + Integer.BYTES * 2 + name.length
                + textureHash.length + 3);
        input.put(descriptorHash.bytes()).putInt(serializedSize).putInt(originVersion)
                .put((byte) (forceCulling ? 1 : 0)).put((byte) (forceTranslucent ? 1 : 0))
                .put((byte) (hasPbr ? 1 : 0)).put(name).put(textureHash);
        return ModelHashing.blake3(input.array());
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    @FunctionalInterface
    public interface TexturePixelsSupplier {
        /** Returns borrowed pixels; the caller that created the supplier retains ownership. */
        NativeImage get() throws IOException;
    }
}
