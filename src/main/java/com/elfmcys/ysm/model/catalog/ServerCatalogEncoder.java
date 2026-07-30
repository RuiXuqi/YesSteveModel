package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.proto.network.model.ModelCatalogProto;
import com.elfmcys.ysm.proto.network.model.ModelCatalogDeltaProto;
import com.elfmcys.ysm.util.ProtoBytes;
import com.elfmcys.ysm.util.ProtoUtil;

import java.io.IOException;
import java.util.Map;

public final class ServerCatalogEncoder {
    private ServerCatalogEncoder() {
    }

    public static ArrayBuffer encodeList(ServerCatalogSnapshot snapshot) throws IOException {
        var list = ModelCatalogProto.ModelList.newInstance()
                .setEpoch(snapshot.epochBytes())
                .setRevision(snapshot.revision());
        snapshot.models().values().stream()
                .filter(handle -> !isIntrinsicDefault(handle.location()))
                .sorted((left, right) -> left.location().compareTo(right.location()))
                .map(handle -> entry(handle.descriptor(), handle.location()))
                .forEach(list::addModels);
        snapshot.packs().stream().map(ServerCatalogEncoder::pack).forEach(list::addPacks);
        return ProtoUtil.serializeToBuffer(list);
    }

    public static ArrayBuffer encodeDelta(ServerCatalogSnapshot previous, ServerCatalogSnapshot next,
                                          CatalogDiff diff) throws IOException {
        var delta = ModelCatalogDeltaProto.CatalogDelta.newInstance()
                .setEpoch(next.epochBytes())
                .setBaseRevision(previous.revision())
                .setRevision(next.revision())
                .setReplacePacks(diff.replacePacks());
        for (var operation : diff.operations()) {
            var location = operation.location() != null
                    ? operation.location() : operation.previousLocation();
            if (isIntrinsicDefault(location)) {
                continue;
            }
            var proto = ModelCatalogDeltaProto.CatalogOperation.newInstance()
                    .setType(switch (operation.type()) {
                        case ADD -> ModelCatalogDeltaProto.OperationType.OPERATION_TYPE_ADD;
                        case REMOVE -> ModelCatalogDeltaProto.OperationType.OPERATION_TYPE_REMOVE;
                        case MOVE -> ModelCatalogDeltaProto.OperationType.OPERATION_TYPE_MOVE;
                    });
            ProtoBytes.set(proto.getMutableModelHash(), operation.hash());
            if (operation.descriptor() != null) {
                proto.setModel(entry(operation.descriptor(), operation.location()));
            }
            if (operation.previousLocation() != null) {
                proto.setOldNamespace(operation.previousLocation().rootKind().namespace());
                proto.setOldRelativePath(operation.previousLocation().path().value());
            }
            delta.addOperations(proto);
        }
        if (diff.replacePacks()) {
            next.packs().stream().map(ServerCatalogEncoder::pack).forEach(delta::addPacks);
        }
        return ProtoUtil.serializeToBuffer(delta);
    }

    public static ModelCatalogProto.ModelEntry entry(ModelDescriptor descriptor,
                                                     CatalogModelLocation location) {
        var entry = ModelCatalogProto.ModelEntry.newInstance()
                .setNamespace(location.rootKind().namespace())
                .setRelativePath(location.path().value())
                .setAccessPolicy(accessPolicy(location.rootKind().accessPolicy()));
        ProtoBytes.set(entry.getMutableModelHash(), descriptor.modelHash());
        ProtoBytes.set(entry.getMutableDescriptorHash(), descriptor.descriptorHash());
        entry.getMutableContainerPreamble().setInternalArray(descriptor.containerPreamble());
        entry.getMutableSchemaManifest().setInternalArray(descriptor.schemaManifest());
        return entry;
    }

    public static ModelCatalogProto.ModelPackEntry pack(ModelPackDescriptor descriptor) {
        var pack = ModelCatalogProto.ModelPackEntry.newInstance()
                .setNamespace(descriptor.rootKind().namespace())
                .setHierarchy(descriptor.hierarchy())
                .setName(descriptor.name())
                .setDescription(descriptor.description())
                .setCoverFormat(descriptor.coverFormat())
                .setCoverSize(descriptor.coverSize())
                .setAccessPolicy(accessPolicy(descriptor.rootKind().accessPolicy()));
        pack.getMutableCoverHash().setInternalArray(descriptor.coverHash());
        descriptor.translations().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> pack.addTranslations(ModelCatalogProto.LocalizedPackText.newInstance()
                        .setLocale(entry.getKey())
                        .setName(entry.getValue().name())
                        .setDescription(entry.getValue().description())));
        return pack;
    }

    private static ModelCatalogProto.AccessPolicy accessPolicy(
            com.elfmcys.ysm.model.source.AccessPolicy policy) {
        return switch (policy) {
            case PUBLIC -> ModelCatalogProto.AccessPolicy.ACCESS_POLICY_PUBLIC;
            case SESSION_AUTHORIZED -> ModelCatalogProto.AccessPolicy.ACCESS_POLICY_SESSION_AUTHORIZED;
            case LOCAL_ONLY -> throw new IllegalArgumentException(
                    "Local-only entries cannot be sent over the unstable protocol");
        };
    }

    public static boolean isIntrinsicDefault(CatalogModelLocation location) {
        return location != null && location.rootKind() == CatalogRootKind.BUILTIN
                && location.path().value().equals("default");
    }

}
