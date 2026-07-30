package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelScanError;

import java.util.Map;
import java.util.Objects;

public sealed interface RootInventoryState permits RootInventoryState.Complete,
        RootInventoryState.Incomplete {
    CatalogRootIdentity root();

    record Complete(CatalogRootIdentity root,
                    Map<ModelSourceKey, SourceObservation> sources,
                    Map<ModelPackSourceKey, PackObservation> packs)
            implements RootInventoryState {
        public Complete {
            Objects.requireNonNull(root, "root");
            sources = Map.copyOf(sources);
            packs = Map.copyOf(packs);
        }
    }

    record Incomplete(CatalogRootIdentity root, ModelScanError error)
            implements RootInventoryState {
        public Incomplete {
            Objects.requireNonNull(root, "root");
            Objects.requireNonNull(error, "error");
        }
    }
}
