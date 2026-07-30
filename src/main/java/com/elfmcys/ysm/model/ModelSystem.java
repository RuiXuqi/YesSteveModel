package com.elfmcys.ysm.model;

import com.elfmcys.ysm.model.catalog.BuiltinModelCatalog;
import com.elfmcys.ysm.model.catalog.BuiltinModelIndex;
import com.elfmcys.ysm.model.catalog.ModelCatalogSources;
import com.elfmcys.ysm.model.catalog.ModelSourceDiscovery;
import com.elfmcys.ysm.model.catalog.ReloadableModelCatalog;
import com.elfmcys.ysm.model.catalog.StartupSourceInventory;
import com.elfmcys.ysm.model.storage.ConversionProfileId;
import com.elfmcys.ysm.model.storage.ConversionProfileInputs;
import com.elfmcys.ysm.model.storage.ModelStorageInfrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.stream.Collectors;

/** Process composition root for immutable contracts, storage, and shared catalogs. */
public final class ModelSystem implements AutoCloseable {
    private final ModelStorageInfrastructure storage;
    private final BuiltinModelCatalog builtins;
    private final ReloadableModelCatalog reloadableCatalog;
    private boolean closed;

    private ModelSystem(ModelStorageInfrastructure storage,
                        BuiltinModelCatalog builtins,
                        ReloadableModelCatalog reloadableCatalog) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.builtins = Objects.requireNonNull(builtins, "builtins");
        this.reloadableCatalog = Objects.requireNonNull(reloadableCatalog, "reloadableCatalog");
    }

    public static ModelSystem openDefault() {
        final BuiltinModelIndex contract;
        try {
            contract = BuiltinModelIndex.read(ModelCatalogSources.builtinIndex());
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to load the builtin model contract", error);
        }
        var profile = ConversionProfileId.from(
                ConversionProfileInputs.production(contract.dedupProfileHash()));
        var inventoryStates = ModelCatalogSources.reloadableSources().stream()
                .map(ModelSourceDiscovery::inventory).toList();
        var inventory = new StartupSourceInventory(inventoryStates.stream().collect(
                Collectors.toMap(state -> state.root(), state -> state)));
        var storage = ModelStorageInfrastructure.openDefault(profile, inventory);
        BuiltinModelCatalog builtins = null;
        try {
            builtins = BuiltinModelCatalog.open(storage, contract);
            var reserved = builtins.snapshot().models().stream()
                    .map(handle -> handle.descriptor().modelHash())
                    .collect(Collectors.toUnmodifiableSet());
            var reloadable = new ReloadableModelCatalog(
                    storage, ModelCatalogSources.reloadableSources(), reserved, contract);
            return new ModelSystem(storage, builtins, reloadable);
        } catch (RuntimeException | Error error) {
            if (builtins != null) {
                try {
                    builtins.close();
                } catch (RuntimeException closeError) {
                    error.addSuppressed(closeError);
                }
            }
            try {
                storage.close();
            } catch (RuntimeException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }

    public BuiltinModelIndex builtinContract() {
        requireOpen();
        return builtins.contract();
    }

    public ModelStorageInfrastructure storage() {
        requireOpen();
        return storage;
    }

    public BuiltinModelCatalog builtins() {
        requireOpen();
        return builtins;
    }

    public ReloadableModelCatalog reloadableCatalog() {
        requireOpen();
        return reloadableCatalog;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        reloadableCatalog.close();
        RuntimeException failure = null;
        try {
            builtins.close();
        } catch (RuntimeException error) {
            failure = suppress(failure, error);
        }
        try {
            storage.close();
        } catch (RuntimeException error) {
            failure = suppress(failure, error);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private synchronized void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Model system is closed");
        }
    }

    private static RuntimeException suppress(RuntimeException failure,
                                             RuntimeException next) {
        if (failure == null) {
            return next;
        }
        failure.addSuppressed(next);
        return failure;
    }
}
