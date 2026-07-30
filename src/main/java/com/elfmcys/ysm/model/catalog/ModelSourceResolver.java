package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.format.parser.RawCompileResult;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.importer.RawModelImporter;
import com.elfmcys.ysm.model.storage.ConvertedObjectStore;
import com.elfmcys.ysm.model.storage.ConvertedSourceIndex;
import com.elfmcys.ysm.model.storage.ConvertedSourceIndexStore;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelHashMismatchException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

/** Resolves one observed source without owning catalog conflict or publication policy. */
public final class ModelSourceResolver {
    private final RawModelImporter importer;
    private final ConvertedSourceIndexStore indexes;
    private final ConvertedObjectStore objects;

    public ModelSourceResolver(RawModelImporter importer,
                               ConvertedSourceIndexStore indexes,
                               ConvertedObjectStore objects) {
        this.importer = Objects.requireNonNull(importer, "importer");
        this.indexes = Objects.requireNonNull(indexes, "indexes");
        this.objects = Objects.requireNonNull(objects, "objects");
    }

    public Resolution resolve(SourceObservation observation,
                              Optional<ModelSourceState.Ready> previous,
                              HashProbeBudget probeBudget,
                              boolean recovery) throws CatalogBuildException {
        var location = new CatalogModelLocation(observation.key().root().rootKind(),
                observation.key().relativePath());
        if (observation.key().sourceKind() == ModelSourceKind.DIRECT_CONTAINER) {
            return validateDirect(observation, location);
        }

        final Optional<ConvertedSourceIndex> index;
        try {
            index = indexes.read(observation.key());
        } catch (IOException error) {
            throw new CatalogInfrastructureException(
                    "Failed to read converted source index", error);
        }

        var candidate = index.filter(value -> value.lastObservedStamp().equals(observation.stamp()));
        if (candidate.isEmpty() && previous.isPresent()
                && previous.get().observation().stamp().equals(observation.stamp())) {
            var descriptor = previous.get().handle().descriptor();
            candidate = Optional.of(new ConvertedSourceIndex(observation.key(), observation.stamp(),
                    objects.profile(), descriptor.modelHash(), descriptor.descriptorHash()));
        }

        if (!recovery && candidate.isPresent() && importer.supportsHashProbe(observation.absolutePath())
                && probeBudget.mayProbe()) {
            final boolean structurallyVisible;
            try {
                structurallyVisible = objects.isStructurallyVisible(candidate.get().modelHash());
            } catch (IOException error) {
                throw new CatalogInfrastructureException(
                        "Failed to inspect converted object metadata", error);
            }
            if (structurallyVisible) {
                var probed = scanStable(observation);
                try {
                    var existing = objects.openVerified(probed.modelHash(), location);
                    if (existing.isPresent()) {
                        var handle = ModelFileHandle.openConverted(existing.get().file(), location);
                        writeIndexHint(probed.observation(), handle);
                        return new Resolution(probed.observation(), handle, Route.PROBE_HIT);
                    }
                } catch (IOException error) {
                    throw new CatalogInfrastructureException(
                            "Failed to open verified converted object", error);
                }
                probeBudget.recordMiss();
                return compileStable(probed.observation(), location, probed.modelHash());
            }
        }
        return compileStable(observation, location, null);
    }

    public void forgetIndex(ModelSourceKey key) {
        try {
            indexes.delete(key);
        } catch (IOException error) {
            YesSteveModel.LOGGER.debug(
                    "Failed to delete obsolete converted source index source={}",
                    key.relativePath(), error);
        }
    }

    private Resolution validateDirect(SourceObservation initial,
                                      CatalogModelLocation location) throws ModelSourceException {
        var before = initial;
        for (var attempt = 0; attempt < 2; attempt++) {
            try {
                var handle = ModelFileHandle.openDirect(before.absolutePath(), location);
                var after = ModelSourceDiscovery.observe(before.key(), before.absolutePath());
                if (before.stamp().equals(after.stamp())) {
                    return new Resolution(after, handle, Route.DIRECT_VALIDATED);
                }
                before = after;
            } catch (IOException | RuntimeException error) {
                if (attempt == 1) {
                    throw new ModelSourceException("Failed to validate direct model source", error);
                }
                try {
                    before = ModelSourceDiscovery.observe(before.key(), before.absolutePath());
                } catch (IOException refreshError) {
                    throw new ModelSourceException("Direct model source changed while loading",
                            refreshError);
                }
            }
        }
        throw new ModelSourceException("Direct model source changed while loading");
    }

    private StableProbe scanStable(SourceObservation initial) throws ModelSourceException {
        var before = initial;
        for (var attempt = 0; attempt < 2; attempt++) {
            try {
                var hash = importer.scanModelHash(before.absolutePath());
                var after = ModelSourceDiscovery.observe(before.key(), before.absolutePath());
                if (before.stamp().equals(after.stamp())) {
                    return new StableProbe(hash, after);
                }
                before = after;
            } catch (RuntimeException | IOException error) {
                if (attempt == 1) {
                    throw new ModelSourceException("Failed to scan raw model source", error);
                }
                try {
                    before = ModelSourceDiscovery.observe(before.key(), before.absolutePath());
                } catch (IOException refreshError) {
                    throw new ModelSourceException("Raw model source changed while scanning",
                            refreshError);
                }
            }
        }
        throw new ModelSourceException("Raw model source changed while scanning");
    }

    private Resolution compileStable(SourceObservation initial,
                                     CatalogModelLocation location,
                                     ModelHash expectedProbeHash)
            throws CatalogBuildException {
        var before = initial;
        for (var attempt = 0; attempt < 2; attempt++) {
            RawCompileResult compiled = null;
            java.nio.file.Path temporaryDirectory = null;
            try {
                Files.createDirectories(objectsTemporary());
                temporaryDirectory = Files.createTempDirectory(objectsTemporary(), "resolve-");
                compiled = importer.convert(before.absolutePath(), temporaryDirectory);
                var after = ModelSourceDiscovery.observe(before.key(), before.absolutePath());
                if (!before.stamp().equals(after.stamp())
                        || expectedProbeHash != null
                        && !expectedProbeHash.equals(compiled.modelHash())) {
                    cleanup(compiled, temporaryDirectory);
                    compiled = null;
                    temporaryDirectory = null;
                    before = after;
                    continue;
                }

                try {
                    var staged = ModelFileHandle.openDirect(compiled.stagedContainer(), location);
                    if (!staged.descriptor().modelHash().equals(compiled.modelHash())) {
                        throw new ModelHashMismatchException(location, compiled.modelHash(),
                                staged.descriptor().modelHash());
                    }
                } catch (IOException | RuntimeException error) {
                    throw new ModelSourceException("Raw conversion produced an invalid model", error);
                }

                final com.elfmcys.ysm.model.storage.VerifiedConvertedObject object;
                try {
                    object = objects.commit(compiled, location);
                } catch (ModelHashMismatchException error) {
                    throw new ModelSourceException("Raw conversion hash changed", error);
                } catch (IOException error) {
                    throw new CatalogInfrastructureException(
                            "Failed to commit converted object", error);
                }
                var handle = ModelFileHandle.openConverted(object.file(), location);
                writeIndexHint(after, handle);
                return new Resolution(after, handle, Route.CONVERTED);
            } catch (ModelSourceException | CatalogInfrastructureException error) {
                throw error;
            } catch (RuntimeException error) {
                throw new ModelSourceException("Failed to convert raw model source", error);
            } catch (IOException error) {
                throw new CatalogInfrastructureException("Failed to prepare raw conversion", error);
            } finally {
                try {
                    cleanup(compiled, temporaryDirectory);
                } catch (IOException error) {
                    YesSteveModel.LOGGER.debug("Failed to clean raw conversion staging", error);
                }
            }
        }
        throw new ModelSourceException("Raw model source changed while converting");
    }

    private java.nio.file.Path objectsTemporary() {
        return objects.temporaryRoot();
    }

    private void writeIndexHint(SourceObservation observation, ModelFileHandle handle) {
        try {
            indexes.write(new ConvertedSourceIndex(observation.key(), observation.stamp(),
                    objects.profile(), handle.descriptor().modelHash(),
                    handle.descriptor().descriptorHash()));
        } catch (IOException error) {
            YesSteveModel.LOGGER.debug(
                    "Failed to write converted source index source={}",
                    observation.key().relativePath(), error);
        }
    }

    private static void cleanup(RawCompileResult compiled,
                                java.nio.file.Path temporaryDirectory) throws IOException {
        if (compiled != null && temporaryDirectory != null
                && compiled.stagedContainer().startsWith(temporaryDirectory)) {
            Files.deleteIfExists(compiled.stagedContainer());
        }
        if (temporaryDirectory != null) {
            Files.deleteIfExists(temporaryDirectory);
        }
    }

    public record Resolution(SourceObservation observation, ModelFileHandle handle, Route route) {
    }

    public enum Route {
        DIRECT_VALIDATED,
        PROBE_HIT,
        CONVERTED
    }

    private record StableProbe(ModelHash modelHash, SourceObservation observation) {
    }
}
