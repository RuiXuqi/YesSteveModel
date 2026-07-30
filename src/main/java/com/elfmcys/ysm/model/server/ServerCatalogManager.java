package com.elfmcys.ysm.model.server;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.model.catalog.CatalogScanResult;
import com.elfmcys.ysm.model.catalog.CatalogDiff;
import com.elfmcys.ysm.model.catalog.ReloadableCatalogTransition;
import com.elfmcys.ysm.model.catalog.ReloadableModelCatalog;
import com.elfmcys.ysm.model.catalog.ServerCatalogEncoder;
import com.elfmcys.ysm.model.catalog.ServerCatalogSnapshot;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.network.message.model.ModelListSnapshotEncoder;
import com.elfmcys.ysm.network.message.model.PreparedTransfer;
import com.elfmcys.ysm.network.message.model.TransferKind;
import com.elfmcys.ysm.network.message.model.ZstdPayloadCompression;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Server publisher for the JVM-owned reloadable catalog. It never scans sources. */
final class ServerCatalogManager implements AutoCloseable {
    private final MinecraftServer server;
    private final CatalogScanResult builtins;
    private final Consumer<OutboundCatalog> broadcast;
    private final AtomicReference<State> state = new AtomicReference<>();
    private final AtomicReference<ModelScanReport> lastReport =
            new AtomicReference<>(ModelScanReport.empty());
    private final CompletableFuture<Void> initialLoad = new CompletableFuture<>();
    private final ReloadableModelCatalog.Subscription subscription;

    ServerCatalogManager(MinecraftServer server, Executor publisherExecutor,
                         ReloadableModelCatalog reloadable,
                         CatalogScanResult builtins,
                         Consumer<OutboundCatalog> broadcast) {
        this.server = server;
        this.builtins = Objects.requireNonNull(builtins, "builtins");
        this.broadcast = Objects.requireNonNull(broadcast, "broadcast");
        subscription = reloadable.subscribe(publisherExecutor, this::apply).join();
    }

    Optional<ServerCatalogSnapshot> snapshot() {
        var current = state.get();
        return current == null ? Optional.empty() : Optional.of(current.snapshot());
    }

    ModelScanReport lastReport() {
        return lastReport.get();
    }

    CompletableFuture<PreparedTransfer> fullCatalog() {
        var current = state.get();
        if (current != null) {
            return CompletableFuture.completedFuture(current.fullCatalog().acquire());
        }
        return initialLoad.thenApply(ignored -> state.get().fullCatalog().acquire());
    }

    CompletableFuture<Void> awaitApplied(long generation) {
        return subscription.awaitApplied(generation);
    }

    private CompletableFuture<Void> apply(ReloadableCatalogTransition transition) {
        final PreparedPublication prepared;
        try {
            prepared = prepare(transition);
        } catch (Exception error) {
            return CompletableFuture.failedFuture(error);
        }
        var completion = new CompletableFuture<Void>();
        server.execute(() -> commit(prepared, completion));
        return completion;
    }

    private PreparedPublication prepare(ReloadableCatalogTransition transition) throws Exception {
        var previous = state.get();
        var oldSnapshot = previous == null ? null : previous.snapshot();
        var builtin = builtins;
        var models = new ArrayList<>(builtin.models());
        models.addAll(transition.current().models().values());
        var packs = new ArrayList<>(builtin.packs());
        packs.addAll(transition.current().packs());
        var report = combinedReport(builtin.report(), transition.current().report());

        var epoch = oldSnapshot == null ? UUID.randomUUID() : oldSnapshot.epoch();
        var tentative = new ServerCatalogSnapshot(epoch,
                oldSnapshot == null ? 1 : oldSnapshot.revision(), models, packs, report);
        var diff = oldSnapshot == null ? null : CatalogDiff.between(oldSnapshot, tentative);
        var wireChanged = oldSnapshot == null || diff.replacePacks() || !diff.operations().isEmpty();
        var revision = oldSnapshot == null ? 1
                : wireChanged ? oldSnapshot.revision() + 1 : oldSnapshot.revision();
        var snapshot = revision == tentative.revision() ? tentative
                : new ServerCatalogSnapshot(epoch, revision, models, packs, report);

        if (!wireChanged && previous != null) {
            return new PreparedPublication(transition.current().reloadGeneration(), snapshot,
                    previous.fullCatalog().acquire(), null, false);
        }

        PreparedTransfer full = null;
        try {
            try (var serialized = ServerCatalogEncoder.encodeList(snapshot)) {
                full = ModelListSnapshotEncoder.encode(serialized);
            }
            PreparedTransfer delta = null;
            if (oldSnapshot != null) {
                try (var serialized = ServerCatalogEncoder.encodeDelta(
                        oldSnapshot, snapshot, CatalogDiff.between(oldSnapshot, snapshot))) {
                    delta = ZstdPayloadCompression.prepareDynamic(serialized);
                }
            }
            return new PreparedPublication(transition.current().reloadGeneration(), snapshot,
                    full, delta, oldSnapshot != null);
        } catch (Throwable error) {
            if (full != null) {
                full.close();
            }
            throw error;
        }
    }

    private void commit(PreparedPublication prepared, CompletableFuture<Void> completion) {
        var installed = false;
        try {
            var previous = state.getAndSet(
                    new State(prepared.generation(), prepared.snapshot(), prepared.full()));
            installed = true;
            if (previous != null) {
                previous.close();
            }
            lastReport.set(prepared.snapshot().report());
            reportSummary(prepared.snapshot(), prepared.generation());
            initialLoad.complete(null);

            if (prepared.broadcast()) {
                var useDelta = prepared.delta() != null
                        && prepared.delta().contentSize() < prepared.full().contentSize();
                var outbound = new OutboundCatalog(
                        useDelta ? TransferKind.CATALOG_DELTA : TransferKind.CATALOG_FULL,
                        useDelta ? prepared.delta() : prepared.full().acquire());
                if (!useDelta && prepared.delta() != null) {
                    prepared.delta().close();
                }
                try (outbound) {
                    broadcast.accept(outbound);
                }
            } else if (prepared.delta() != null) {
                prepared.delta().close();
            }
            completion.complete(null);
        } catch (Throwable error) {
            if (!installed) {
                prepared.closeUncommitted();
            }
            completion.completeExceptionally(error);
        }
    }

    private static ModelScanReport combinedReport(ModelScanReport builtin,
                                                  ModelScanReport reloadable) {
        var errors = new ArrayList<>(builtin.errors());
        errors.addAll(reloadable.errors());
        var started = builtin.startedAt().isBefore(reloadable.startedAt())
                ? builtin.startedAt() : reloadable.startedAt();
        var completed = builtin.completedAt().isAfter(reloadable.completedAt())
                ? builtin.completedAt() : reloadable.completedAt();
        return new ModelScanReport(started, completed, errors);
    }

    private static void reportSummary(ServerCatalogSnapshot snapshot, long reloadGeneration) {
        var report = snapshot.report();
        if (YesSteveModel.LOGGER.isDebugEnabled()) {
            for (var error : report.errors()) {
                YesSteveModel.LOGGER.debug(
                        "Model scan rejected root={} source={}: {}\n{}",
                        error.rootKind(), error.source(), error.message(), error.detail());
            }
        }
        YesSteveModel.LOGGER.info(
                "Published server model catalog: models={}, packs={}, errors={}, reloadGeneration={}, revision={}, elapsedMs={}{}",
                snapshot.models().size(), snapshot.packs().size(), report.errorCount(),
                reloadGeneration, snapshot.revision(),
                Duration.between(report.startedAt(), report.completedAt()).toMillis(),
                report.errorCount() == 0 ? "" : "; use /ysm model errors for details");
    }

    @Override
    public void close() {
        subscription.close();
        var previous = state.getAndSet(null);
        if (previous != null) {
            previous.close();
        }
    }

    record OutboundCatalog(TransferKind kind, PreparedTransfer payload) implements AutoCloseable {
        @Override
        public void close() {
            payload.close();
        }
    }

    private record PreparedPublication(long generation, ServerCatalogSnapshot snapshot,
                                       PreparedTransfer full, PreparedTransfer delta,
                                       boolean broadcast) {
        private void closeUncommitted() {
            full.close();
            if (delta != null) {
                delta.close();
            }
        }
    }

    private record State(long reloadGeneration, ServerCatalogSnapshot snapshot,
                         PreparedTransfer fullCatalog) implements AutoCloseable {
        @Override
        public void close() {
            fullCatalog.close();
        }
    }
}
