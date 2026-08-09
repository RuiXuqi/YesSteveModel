package com.elfmcys.ysm.client.model.catalog;

import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.source.ModelOffer;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelBackingIdentity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record ClientCatalogEntry(Hash256 modelHash, ModelContentVersion contentVersion,
                                 ModelBackingIdentity backingIdentity,
                                 @Nullable ModelFileHandle local, @Nullable ModelOffer server) {
    public ClientCatalogEntry {
        Objects.requireNonNull(modelHash, "modelHash");
        Objects.requireNonNull(contentVersion, "contentVersion");
        Objects.requireNonNull(backingIdentity, "backingIdentity");
        if (local == null && server == null) {
            throw new IllegalArgumentException("A client catalog entry needs a local or server source");
        }
        if (local != null && !local.descriptor().modelHash().equals(modelHash)) {
            throw new IllegalArgumentException("Local model hash mismatch");
        }
        if (server != null && !server.descriptor().modelHash().equals(modelHash)) {
            throw new IllegalArgumentException("Server model hash mismatch");
        }
        if (local != null && !local.backingIdentity().equals(backingIdentity)) {
            throw new IllegalArgumentException("Selected local backing identity mismatch");
        }
    }

    public ModelDescriptor displayDescriptor() {
        return local != null ? local.descriptor() : Objects.requireNonNull(server).descriptor();
    }

    public String displayPath() {
        return local != null ? local.location().path().value() : Objects.requireNonNull(server).path().value();
    }

    public boolean builtin() {
        return local != null && local.location().rootKind() == CatalogRootKind.BUILTIN;
    }

    public boolean locallyAvailable() {
        return local != null;
    }

    /** The current server policy wins even when the same model content also exists locally. */
    public boolean authorizationRequired() {
        if (server != null) {
            return server.accessPolicy() == AccessPolicy.SESSION_AUTHORIZED;
        }
        return local != null && local.location().rootKind().accessPolicy() == AccessPolicy.SESSION_AUTHORIZED;
    }
}
