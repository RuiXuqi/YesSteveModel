package com.elfmcys.ysm.client.model.catalog;

import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.source.CatalogCursor;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.model.source.ModelOffer;
import com.elfmcys.ysm.model.source.ModelSource;
import com.elfmcys.ysm.model.source.ModelSourceDescriptor;
import com.elfmcys.ysm.model.source.ModelSources;
import com.elfmcys.ysm.model.source.PackOffer;
import com.elfmcys.ysm.model.source.ResolvedCatalog;
import com.elfmcys.ysm.model.source.SourceId;
import com.elfmcys.ysm.model.source.SourceKind;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.util.Collection;
import java.util.Objects;

/** Adapts one scanner-local root into a source-neutral catalog. */
public final class LocalModelSource implements ModelSource {
    private static final byte[] PROCESS_EPOCH = new byte[16];

    private final ModelSourceDescriptor descriptor;
    private final ResolvedCatalog catalog;

    public LocalModelSource(CatalogRootKind rootKind,
                            Collection<ModelFileHandle> models,
                            Collection<ModelPackDescriptor> packs) {
        Objects.requireNonNull(rootKind, "rootKind");
        Objects.requireNonNull(models, "models");
        Objects.requireNonNull(packs, "packs");
        var sourceId = sourceId(rootKind);
        descriptor = new ModelSourceDescriptor(sourceId, sourceKind(rootKind),
                ModelSources.defaultPriority(sourceId));
        catalog = new ResolvedCatalog(new CatalogCursor(sourceId, PROCESS_EPOCH, 0),
                models.stream()
                        .filter(handle -> handle.location().rootKind() == rootKind)
                        .map(LocalModelSource::offer)
                        .toList(),
                packs.stream()
                        .filter(pack -> pack.rootKind() == rootKind)
                        .map(LocalModelSource::offer)
                        .toList());
    }

    @Override
    public ModelSourceDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public ResolvedCatalog catalog() {
        return catalog;
    }

    private static ModelOffer offer(ModelFileHandle handle) {
        var location = handle.location();
        return new ModelOffer(sourceId(location.rootKind()), location.rootKind().namespace(),
                location.path(), location.rootKind().accessPolicy(), handle.descriptor());
    }

    private static PackOffer offer(ModelPackDescriptor pack) {
        return new PackOffer(sourceId(pack.rootKind()),
                new ModelAssetSubject.Pack(pack.rootKind().namespace(), pack.hierarchy()),
                pack.name(), pack.description(), pack.translations(),
                pack.coverHash().length == 0 ? null : new Hash256(pack.coverHash()),
                pack.coverFormat(), pack.coverSize(), pack.rootKind().accessPolicy());
    }

    private static SourceId sourceId(CatalogRootKind rootKind) {
        return switch (rootKind) {
            case BUILTIN -> ModelSources.BUILTIN;
            case CUSTOM -> ModelSources.LOCAL_CUSTOM;
            case AUTH -> ModelSources.LOCAL_AUTH;
        };
    }

    private static SourceKind sourceKind(CatalogRootKind rootKind) {
        return rootKind == CatalogRootKind.BUILTIN ? SourceKind.BUILTIN : SourceKind.LOCAL;
    }
}
