package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelScanError;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds one immutable reloadable catalog candidate without publishing it. */
public final class CatalogReconciler {
    private final List<ModelCatalogSource> roots;
    private final ModelSourceResolver resolver;
    private final ModelPackScanner packScanner = new ModelPackScanner();
    private final Set<Hash256> builtinReservedHashes;

    public CatalogReconciler(List<ModelCatalogSource> roots, ModelSourceResolver resolver,
                             Set<Hash256> builtinReservedHashes) {
        this.roots = List.copyOf(roots);
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.builtinReservedHashes = Set.copyOf(builtinReservedHashes);
    }

    public CatalogReconcileResult reconcile(ReloadableCatalogSnapshot base,
                                            ReloadRequest request)
            throws CatalogBuildException {
        var startedAt = Instant.now();
        var startedNanos = System.nanoTime();
        var initial = discoverComplete();
        var discoveryNanos = System.nanoTime() - startedNanos;
        var acceptedSources = new LinkedHashMap<ModelSourceKey, SourceObservation>();
        var acceptedPacks = new LinkedHashMap<ModelPackSourceKey, PackObservation>();
        initial.values().forEach(root -> {
            acceptedSources.putAll(root.sources());
            acceptedPacks.putAll(root.packs());
        });
        base.sources().keySet().stream()
                .filter(key -> !acceptedSources.containsKey(key))
                .forEach(resolver::forgetIndex);

        var sourceStates = new LinkedHashMap<ModelSourceKey, ModelSourceState>();
        var packStates = new LinkedHashMap<ModelPackSourceKey, ModelPackSourceState>();
        var errors = new ArrayList<ModelScanError>();
        var touchedDirect = new HashSet<CatalogBackingKey>();
        var revalidated = new HashSet<CatalogBackingKey>();
        var stats = new StatsBuilder();
        var probeBudget = new HashProbeBudget(2);

        for (var observation : acceptedSources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).toList()) {
            var previous = base.sources().get(observation.key());
            var recovery = previous instanceof ModelSourceState.Ready ready
                    && requestedRecovery(request, ready);
            if (canReuse(request, observation, previous, recovery)) {
                sourceStates.put(observation.key(), previous);
                stats.reused++;
                if (previous instanceof ModelSourceState.Rejected rejected) {
                    errors.add(rejected.error());
                    stats.rejected++;
                }
                continue;
            }

            try {
                var previousReady = previous instanceof ModelSourceState.Ready ready
                        ? java.util.Optional.of(ready) : java.util.Optional.<ModelSourceState.Ready>empty();
                var result = resolver.resolve(observation, previousReady, probeBudget, recovery);
                acceptedSources.put(observation.key(), result.observation());
                var ready = new ModelSourceState.Ready(result.observation(),
                        result.handle().descriptor().modelHash(), result.handle());
                sourceStates.put(observation.key(), ready);
                stats.accept(result.route());
                collectBackingSignals(request, previousReady.orElse(null), ready,
                        touchedDirect, revalidated);
            } catch (ModelSourceException error) {
                var report = new ModelScanError(Instant.now(),
                        observation.key().root().rootKind(),
                        observation.key().relativePath().value(),
                        ModelScanError.Category.MODEL_SOURCE, "SOURCE_REJECTED",
                        Objects.requireNonNullElse(error.getMessage(), "Model source rejected"),
                        stackTrace(error));
                sourceStates.put(observation.key(), new ModelSourceState.Rejected(observation, report));
                errors.add(report);
                stats.rejected++;
            }
        }

        for (var observation : acceptedPacks.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList()) {
            var previous = base.packSources().get(observation.key());
            if (request.auditLevel() == AuditLevel.INCREMENTAL
                    && previous != null && previous.observation().equals(observation)
                    && !isTouched(request, observation.directory())) {
                packStates.put(observation.key(), previous);
                if (previous instanceof ModelPackSourceState.Rejected rejected) {
                    errors.add(rejected.error());
                    stats.rejected++;
                }
                continue;
            }
            try {
                var effectiveRoot = new ModelCatalogSource(observation.key().root().rootKind(),
                        observation.key().root().canonicalAbsoluteRoot(), false);
                var descriptor = packScanner.scan(effectiveRoot, observation.directory())
                        .orElseThrow(() -> new ModelPackException(
                                "Observed model pack manifest disappeared"));
                packStates.put(observation.key(),
                        new ModelPackSourceState.Ready(observation, descriptor));
            } catch (ModelPackException error) {
                addRejectedPack(observation, error, packStates, errors, stats);
            } catch (Exception error) {
                addRejectedPack(observation,
                        new ModelPackException("Failed to read model pack", error),
                        packStates, errors, stats);
            }
        }

        verifyFinalInventory(initial, acceptedSources, acceptedPacks);

        var models = selectModels(sourceStates, errors, stats);
        var packs = packStates.values().stream()
                .filter(ModelPackSourceState.Ready.class::isInstance)
                .map(ModelPackSourceState.Ready.class::cast)
                .map(ModelPackSourceState.Ready::descriptor).sorted().toList();
        var report = new ModelScanReport(startedAt, Instant.now(), errors);
        var snapshot = new ReloadableCatalogSnapshot(base.reloadGeneration() + 1, true,
                sourceStates, packStates, models, packs, report);
        var reloadStats = stats.build(discoveryNanos,
                Duration.ofNanos(System.nanoTime() - startedNanos));
        return new CatalogReconcileResult(snapshot, touchedDirect, revalidated, reloadStats);
    }

    private Map<CatalogRootKind, RootInventoryState.Complete> discoverComplete()
            throws CatalogInfrastructureException {
        var result = new LinkedHashMap<CatalogRootKind, RootInventoryState.Complete>();
        for (var source : roots) {
            var inventory = ModelSourceDiscovery.inventory(source);
            if (inventory instanceof RootInventoryState.Incomplete incomplete) {
                throw new CatalogInfrastructureException(
                        "Model root inventory is incomplete: " + source.path(),
                        new java.io.IOException(incomplete.error().message()));
            }
            var complete = (RootInventoryState.Complete) inventory;
            result.put(source.rootKind(), complete);
        }
        return result;
    }

    private void verifyFinalInventory(Map<CatalogRootKind, RootInventoryState.Complete> initial,
                                      Map<ModelSourceKey, SourceObservation> acceptedSources,
                                      Map<ModelPackSourceKey, PackObservation> acceptedPacks)
            throws CatalogBuildException {
        for (var root : roots) {
            var finalState = ModelSourceDiscovery.inventory(root);
            if (finalState instanceof RootInventoryState.Incomplete incomplete) {
                throw new CatalogInfrastructureException(
                        "Final model root inventory is incomplete: " + root.path(),
                        new java.io.IOException(incomplete.error().message()));
            }
            var actual = (RootInventoryState.Complete) finalState;
            var expectedRoot = initial.get(root.rootKind()).root();
            var expectedSources = acceptedSources.entrySet().stream()
                    .filter(entry -> entry.getKey().root().rootKind() == root.rootKind())
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                            LinkedHashMap::putAll);
            var expectedPacks = acceptedPacks.entrySet().stream()
                    .filter(entry -> entry.getKey().root().rootKind() == root.rootKind())
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                            LinkedHashMap::putAll);
            if (!actual.root().equals(expectedRoot)
                    || !actual.sources().equals(expectedSources)
                    || !actual.packs().equals(expectedPacks)) {
                throw new CatalogInventoryChangedException(
                        "Model root changed before catalog commit: " + root.path());
            }
        }
    }

    private Map<Hash256, ModelFileHandle> selectModels(
            Map<ModelSourceKey, ModelSourceState> states,
            List<ModelScanError> errors, StatsBuilder stats) {
        var ready = states.values().stream().filter(ModelSourceState.Ready.class::isInstance)
                .map(ModelSourceState.Ready.class::cast).toList();
        var byHash = new HashMap<Hash256, List<ModelSourceState.Ready>>();
        var byLocation = new HashMap<CatalogModelLocation, List<ModelSourceState.Ready>>();
        ready.forEach(value -> {
            byHash.computeIfAbsent(value.modelHash(), ignored -> new ArrayList<>()).add(value);
            byLocation.computeIfAbsent(value.handle().location(), ignored -> new ArrayList<>()).add(value);
        });
        var rejected = new HashSet<ModelSourceState.Ready>();
        byHash.forEach((hash, group) -> {
            if (group.size() > 1) {
                rejected.addAll(group);
                group.forEach(value -> errors.add(conflict(value,
                        "DUPLICATE_MODEL_HASH", "Duplicate full model hash " + hash)));
            }
        });
        byLocation.forEach((location, group) -> {
            if (group.size() > 1) {
                rejected.addAll(group);
                group.forEach(value -> errors.add(conflict(value,
                        "DUPLICATE_MODEL_LOCATION", "Duplicate model location " + location)));
            }
        });
        ready.stream().filter(value -> builtinReservedHashes.contains(value.modelHash()))
                .forEach(value -> {
                    rejected.add(value);
                    errors.add(conflict(value, "BUILTIN_MODEL_HASH_RESERVED",
                            "Model hash is reserved by a builtin model " + value.modelHash()));
                });
        stats.rejected += rejected.size();
        var result = new LinkedHashMap<Hash256, ModelFileHandle>();
        ready.stream().filter(value -> !rejected.contains(value))
                .sorted((left, right) -> left.handle().location()
                        .compareTo(right.handle().location()))
                .forEach(value -> result.put(value.modelHash(), value.handle()));
        return result;
    }

    private static ModelScanError conflict(ModelSourceState.Ready value,
                                           String code, String message) {
        return new ModelScanError(Instant.now(), value.observation().key().root().rootKind(),
                value.observation().key().relativePath().value(),
                ModelScanError.Category.CONFLICT, code, message, "");
    }

    private static boolean canReuse(ReloadRequest request, SourceObservation observation,
                                    ModelSourceState previous, boolean recovery) {
        return request.auditLevel() == AuditLevel.INCREMENTAL
                && previous != null && previous.observation().equals(observation)
                && !recovery && !isTouched(request, observation.absolutePath());
    }

    private static boolean isTouched(ReloadRequest request, java.nio.file.Path source) {
        return request.touchedPaths().stream()
                .anyMatch(path -> path.equals(source) || path.startsWith(source)
                        || source.startsWith(path));
    }

    private static boolean requestedRecovery(ReloadRequest request,
                                             ModelSourceState.Ready previous) {
        var key = new CatalogBackingKey(previous.handle().location(),
                previous.handle().backingIdentity());
        return request.recoveries().contains(new BackingRecoveryRequest(key));
    }

    private static void collectBackingSignals(ReloadRequest request,
                                              ModelSourceState.Ready previous,
                                              ModelSourceState.Ready current,
                                              Set<CatalogBackingKey> touchedDirect,
                                              Set<CatalogBackingKey> revalidated) {
        if (previous == null) {
            return;
        }
        var oldKey = new CatalogBackingKey(previous.handle().location(),
                previous.handle().backingIdentity());
        var newKey = new CatalogBackingKey(current.handle().location(),
                current.handle().backingIdentity());
        if (!oldKey.equals(newKey)) {
            return;
        }
        if (requestedRecovery(request, previous)) {
            revalidated.add(newKey);
        } else if (current.observation().key().sourceKind()
                == ModelSourceKind.DIRECT_CONTAINER
                && isTouched(request, current.observation().absolutePath())) {
            touchedDirect.add(newKey);
        }
    }

    private static void addRejectedPack(PackObservation observation,
                                        ModelPackException error,
                                        Map<ModelPackSourceKey, ModelPackSourceState> states,
                                        List<ModelScanError> errors,
                                        StatsBuilder stats) {
        var report = new ModelScanError(Instant.now(),
                observation.key().root().rootKind(), observation.key().hierarchy(),
                ModelScanError.Category.MODEL_PACK, "PACK_REJECTED",
                Objects.requireNonNullElse(error.getMessage(), "Model pack rejected"),
                stackTrace(error));
        states.put(observation.key(), new ModelPackSourceState.Rejected(observation, report));
        errors.add(report);
        stats.rejected++;
    }

    private static String stackTrace(Throwable error) {
        var output = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(output));
        return output.toString();
    }

    private static final class StatsBuilder {
        private int reused;
        private int directValidated;
        private int hashProbeHits;
        private int converted;
        private int rejected;

        private void accept(ModelSourceResolver.Route route) {
            switch (route) {
                case DIRECT_VALIDATED -> directValidated++;
                case PROBE_HIT -> hashProbeHits++;
                case CONVERTED -> converted++;
            }
        }

        private ReloadStats build(long discoveryNanos, Duration total) {
            return new ReloadStats(reused, directValidated, 0, hashProbeHits, 0,
                    converted, rejected, 0, Duration.ofNanos(discoveryNanos),
                    Duration.ZERO, Duration.ZERO, total);
        }
    }
}
