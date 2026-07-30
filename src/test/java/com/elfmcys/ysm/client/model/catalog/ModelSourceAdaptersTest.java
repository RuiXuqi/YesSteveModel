package com.elfmcys.ysm.client.model.catalog;

import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.catalog.RemoteCatalogSnapshot;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.source.ModelSources;
import com.elfmcys.ysm.model.source.SourceKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelSourceAdaptersTest {
    @Test
    void localAdapterExposesOnlyItsRoot() {
        var customPack = pack(CatalogRootKind.CUSTOM, "custom/");
        var authPack = pack(CatalogRootKind.AUTH, "auth/");

        var source = new LocalModelSource(CatalogRootKind.AUTH, List.of(),
                List.of(customPack, authPack));

        assertEquals(ModelSources.LOCAL_AUTH, source.descriptor().id());
        assertEquals(SourceKind.LOCAL, source.descriptor().kind());
        assertEquals(List.of("auth/"), source.catalog().packs().stream()
                .map(offer -> offer.subject().hierarchy()).toList());
        assertEquals(AccessPolicy.SESSION_AUTHORIZED,
                source.catalog().packs().get(0).accessPolicy());
    }

    @Test
    void gameServerAdapterPreservesTheRemoteCursor() {
        var epoch = UUID.randomUUID();
        var source = new GameServerModelSource(
                new RemoteCatalogSnapshot(epoch, 42, Map.of(), List.of()));

        assertEquals(ModelSources.GAME_SERVER, source.descriptor().id());
        assertEquals(SourceKind.GAME_SERVER, source.descriptor().kind());
        assertEquals(42, source.catalog().cursor().revision());
        assertEquals(epoch, new UUID(
                java.nio.ByteBuffer.wrap(source.catalog().cursor().epoch()).getLong(),
                java.nio.ByteBuffer.wrap(source.catalog().cursor().epoch(), 8, 8).getLong()));
    }

    private static ModelPackDescriptor pack(CatalogRootKind rootKind, String hierarchy) {
        return new ModelPackDescriptor(rootKind, hierarchy, hierarchy, "", Map.of(),
                new byte[0], "", 0);
    }
}
