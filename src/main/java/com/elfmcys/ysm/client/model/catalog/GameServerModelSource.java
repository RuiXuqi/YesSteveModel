package com.elfmcys.ysm.client.model.catalog;

import com.elfmcys.ysm.model.catalog.RemoteCatalogSnapshot;
import com.elfmcys.ysm.model.source.CatalogCursor;
import com.elfmcys.ysm.model.source.ModelSource;
import com.elfmcys.ysm.model.source.ModelSourceDescriptor;
import com.elfmcys.ysm.model.source.ModelSources;
import com.elfmcys.ysm.model.source.ResolvedCatalog;
import com.elfmcys.ysm.model.source.SourceKind;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Adapts the active game-server catalog into a source-neutral catalog. */
public final class GameServerModelSource implements ModelSource {
    private static final ModelSourceDescriptor DESCRIPTOR = new ModelSourceDescriptor(
            ModelSources.GAME_SERVER, SourceKind.GAME_SERVER, ModelSources.GAME_SERVER_PRIORITY);

    private final ResolvedCatalog catalog;

    public GameServerModelSource(RemoteCatalogSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        var epoch = ByteBuffer.allocate(16)
                .putLong(snapshot.epoch().getMostSignificantBits())
                .putLong(snapshot.epoch().getLeastSignificantBits())
                .array();
        catalog = new ResolvedCatalog(
                new CatalogCursor(ModelSources.GAME_SERVER, epoch, snapshot.revision()),
                snapshot.models().values().stream().toList(), snapshot.packs());
    }

    @Override
    public ModelSourceDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ResolvedCatalog catalog() {
        return catalog;
    }
}
