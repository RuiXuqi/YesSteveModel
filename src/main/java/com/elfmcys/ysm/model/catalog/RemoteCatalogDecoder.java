package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.model.source.ModelOffer;
import com.elfmcys.ysm.model.source.ModelSources;
import com.elfmcys.ysm.model.source.PackOffer;
import com.elfmcys.ysm.model.storage.ModelHashing;
import com.elfmcys.ysm.proto.network.model.ModelCatalogProto;
import com.elfmcys.ysm.proto.network.model.ModelCatalogDeltaProto;
import com.elfmcys.ysm.util.ProtoBytes;
import us.hebi.quickbuf.ProtoSource;
import us.hebi.quickbuf.RepeatedByte;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class RemoteCatalogDecoder {
    private static final int MAX_MODELS = 100_000;
    private static final int MAX_PACKS = 10_000;
    private static final int MAX_PACK_COVER = 1024 * 1024;

    private RemoteCatalogDecoder() {
    }

    public static RemoteCatalogSnapshot decodeList(ArrayBuffer data) throws IOException {
        var source = ModelCatalogProto.ModelList.parseFrom(protoSource(data));
        if (source.getModels().length() > MAX_MODELS || source.getPacks().length() > MAX_PACKS) {
            throw new IOException("Remote model catalog exceeds entry limits");
        }
        if (source.getRevision() == 0) {
            throw new IOException("Remote model catalog revision must not be zero");
        }
        var models = new LinkedHashMap<Hash256, ModelOffer>();
        for (var entry : source.getModels()) {
            var offer = offer(entry);
            if (models.putIfAbsent(offer.descriptor().modelHash(), offer) != null) {
                throw new IOException("Duplicate model hash in remote catalog: " + offer.descriptor().modelHash());
            }
        }
        return new RemoteCatalogSnapshot(epoch(source.getEpoch()), source.getRevision(), models,
                packs(source.getPacks()));
    }

    public static RemoteCatalogSnapshot applyDelta(RemoteCatalogSnapshot current, ArrayBuffer data) throws IOException {
        var delta = ModelCatalogDeltaProto.CatalogDelta.parseFrom(protoSource(data));
        var epoch = epoch(delta.getEpoch());
        if (!current.epoch().equals(epoch) || current.revision() != delta.getBaseRevision()) {
            throw new IOException("Remote model catalog delta does not match the active snapshot");
        }
        if (Long.compareUnsigned(delta.getRevision(), current.revision()) <= 0
                || delta.getOperations().length() > MAX_MODELS) {
            throw new IOException("Remote model catalog delta revision or operation count is invalid");
        }
        var models = new HashMap<>(current.models());
        for (var operation : delta.getOperations()) {
            var hash = modelHash(operation.getModelHash(), "catalog operation model hash");
            switch (operation.getType()) {
                case OPERATION_TYPE_ADD -> {
                    if (!operation.hasModel()) {
                        throw new IOException("Catalog add operation contains no model descriptor");
                    }
                    var offer = offer(operation.getModel());
                    if (!offer.descriptor().modelHash().equals(hash) || models.putIfAbsent(hash, offer) != null) {
                        throw new IOException("Invalid catalog add operation for " + hash);
                    }
                }
                case OPERATION_TYPE_REMOVE -> {
                    if (models.remove(hash) == null) {
                        throw new IOException("Catalog remove operation references an unknown model: " + hash);
                    }
                }
                case OPERATION_TYPE_MOVE -> {
                    if (!operation.hasModel()) {
                        throw new IOException("Catalog move operation contains no destination descriptor");
                    }
                    var previous = models.get(hash);
                    var offer = offer(operation.getModel());
                    final ModelPath previousPath;
                    try {
                        previousPath = new ModelPath(operation.getOldRelativePath());
                    } catch (IllegalArgumentException error) {
                        throw new IOException("Invalid catalog move source for " + hash, error);
                    }
                    if (previous == null || !offer.descriptor().modelHash().equals(hash)
                            || !previous.namespace().equals(operation.getOldNamespace())
                            || !previous.path().equals(previousPath)
                            || (previous.namespace().equals(offer.namespace()) && previous.path().equals(offer.path()))
                            || !previous.descriptor().sameRepresentation(offer.descriptor())) {
                        throw new IOException("Invalid catalog move operation for " + hash);
                    }
                    models.put(hash, offer);
                }
                default -> throw new IOException("Unsupported remote catalog operation: " + operation.getType());
            }
        }
        var packs = delta.getReplacePacks() ? packs(delta.getPacks()) : current.packs();
        return new RemoteCatalogSnapshot(epoch, delta.getRevision(), models, packs);
    }

    public static ModelOffer offer(ModelCatalogProto.ModelEntry entry) throws IOException {
        var modelHash = modelHash(entry.getModelHash(), "model hash");
        var descriptorHash = modelHash(entry.getDescriptorHash(), "descriptor hash");
        var containerPreamble = ProtoBytes.copy(entry.getContainerPreamble());
        var manifest = ProtoBytes.copy(entry.getSchemaManifest());
        if (!ModelHashing.descriptorHash(containerPreamble, manifest).equals(descriptorHash)) {
            throw new IOException("Remote catalog descriptor hash mismatch for " + modelHash);
        }
        var path = new ModelPath(entry.getRelativePath());
        var view = ModelFileView.readMetadata(containerPreamble, manifest);
        var descriptor = new ModelDescriptor(modelHash, descriptorHash, containerPreamble, manifest, view);
        if (!view.getModelHash().equals(modelHash)) {
            throw new IOException("Remote catalog manifest hash mismatch for " + modelHash);
        }
        return new ModelOffer(ModelSources.GAME_SERVER, namespace(entry.getNamespace()), path,
                accessPolicy(entry.getAccessPolicy()), descriptor);
    }

    private static List<PackOffer> packs(
            Iterable<ModelCatalogProto.ModelPackEntry> source) throws IOException {
        var result = new ArrayList<PackOffer>();
        var locations = new HashSet<String>();
        for (var entry : source) {
            var coverHash = ProtoBytes.copy(entry.getCoverHash());
            if (coverHash.length != 0 && coverHash.length != Hash256.SIZE) {
                throw new IOException("Invalid remote model pack cover hash: " + entry.getHierarchy());
            }
            if (entry.getCoverSize() > MAX_PACK_COVER) {
                throw new IOException("Remote model pack cover is too large: " + entry.getHierarchy());
            }
            var translations = new LinkedHashMap<String, ModelPackDescriptor.LocalizedText>();
            for (var translation : entry.getTranslations()) {
                if (translations.putIfAbsent(translation.getLocale(), new ModelPackDescriptor.LocalizedText(
                        translation.getName(), translation.getDescription())) != null) {
                    throw new IOException("Duplicate model pack locale: " + translation.getLocale());
                }
            }
            var namespace = namespace(entry.getNamespace());
            var hierarchy = new ModelAssetSubject.Pack(namespace, entry.getHierarchy()).hierarchy();
            var pack = new PackOffer(ModelSources.GAME_SERVER,
                    new ModelAssetSubject.Pack(namespace, hierarchy), entry.getName(), entry.getDescription(),
                    translations, coverHash.length == 0 ? null : new Hash256(coverHash),
                    entry.getCoverFormat(), entry.getCoverSize(), accessPolicy(entry.getAccessPolicy()));
            if (!locations.add(namespace + "\0" + hierarchy)) {
                throw new IOException("Duplicate remote model pack location: " + namespace + "/" + hierarchy);
            }
            result.add(pack);
        }
        if (result.size() > MAX_PACKS) {
            throw new IOException("Remote model pack list exceeds entry limit");
        }
        return result.stream().sorted((left, right) -> {
            var byNamespace = left.subject().namespace().compareTo(right.subject().namespace());
            return byNamespace != 0 ? byNamespace
                    : left.subject().hierarchy().compareTo(right.subject().hierarchy());
        }).toList();
    }

    private static String namespace(String value) throws IOException {
        var result = value == null ? "" : value.trim();
        if (result.isEmpty()) {
            throw new IOException("Catalog namespace must not be empty");
        }
        return result;
    }

    private static AccessPolicy accessPolicy(ModelCatalogProto.AccessPolicy value) throws IOException {
        return switch (value) {
            case ACCESS_POLICY_PUBLIC -> AccessPolicy.PUBLIC;
            case ACCESS_POLICY_SESSION_AUTHORIZED -> AccessPolicy.SESSION_AUTHORIZED;
            default -> throw new IOException("Invalid catalog access policy: " + value);
        };
    }

    private static Hash256 modelHash(RepeatedByte value, String name) throws IOException {
        if (value.length() != Hash256.SIZE) {
            throw new IOException("Invalid " + name);
        }
        return new Hash256(value.array(), 0, value.length());
    }

    private static UUID epoch(RepeatedByte bytes) throws IOException {
        if (bytes.length() != 16) {
            throw new IOException("Invalid remote catalog epoch");
        }
        var buffer = ByteBuffer.wrap(bytes.array(), 0, bytes.length());
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static ProtoSource protoSource(ArrayBuffer data) {
        return ProtoSource.newInstance(data.array(), data.arrayOffset(), data.size());
    }
}
