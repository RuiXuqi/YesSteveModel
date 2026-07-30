package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.domain.ModelScanError;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.model.importer.RawModelImporter;
import com.elfmcys.ysm.model.storage.ConvertedObjectStore;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelHashMismatchException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** One-shot optional builtin materialization; reloadable roots use CatalogReconciler. */
final class BuiltinModelCatalogScanner {
    private final ConvertedObjectStore objects;
    private final RawModelImporter importer;
    private final ModelPackScanner packScanner = new ModelPackScanner();

    BuiltinModelCatalogScanner(ConvertedObjectStore objects, RawModelImporter importer) {
        this.objects = objects;
        this.importer = importer;
    }

    CatalogScanResult scan(ModelCatalogSource root, BuiltinModelIndex index,
                           Set<ModelPath> excluded) throws IOException {
        if (root.rootKind() != CatalogRootKind.BUILTIN || !Files.isDirectory(root.path())) {
            throw new IOException("Builtin model root is unavailable: " + root.path());
        }
        var startedAt = Instant.now();
        var discovery = ModelSourceDiscovery.discover(root.path());
        if (!discovery.failures().isEmpty()) {
            var failure = discovery.failures().get(0);
            throw new IOException("Failed to discover builtin model source: " + failure.path(),
                    failure.error());
        }
        var paths = new ArrayList<ModelPath>();
        for (var source : discovery.models()) {
            if (!Files.isDirectory(source)) {
                throw new IOException("Builtin index supports only raw model directories: "
                        + relative(root.path(), source));
            }
            paths.add(relativePath(root.path(), source));
        }
        index.validateCoverage(paths);

        var models = new ArrayList<ModelFileHandle>();
        var packs = new ArrayList<ModelPackDescriptor>();
        var errors = new ArrayList<ModelScanError>();
        for (var directory : discovery.packDirectories()) {
            try {
                packScanner.scan(root, directory).ifPresent(packs::add);
            } catch (IOException | RuntimeException error) {
                errors.add(ModelScanError.from(root.rootKind(),
                        relative(root.path(), directory.resolve("ysm-pack.json")), error));
            }
        }
        for (var source : discovery.models()) {
            var relative = relativePath(root.path(), source);
            if (excluded.contains(relative)) {
                continue;
            }
            var location = new CatalogModelLocation(root.rootKind(), relative);
            try {
                var object = objects.resolveKnownHash(source, location, index.require(relative),
                        importer::convert);
                models.add(ModelFileHandle.openConverted(object.file(), location));
            } catch (ModelHashMismatchException error) {
                throw error;
            } catch (IOException | RuntimeException error) {
                errors.add(ModelScanError.from(root.rootKind(), relative.value(), error));
            }
        }
        return new CatalogScanResult(removeModelConflicts(models, errors), packs.stream().sorted().toList(),
                new ModelScanReport(startedAt, Instant.now(), errors));
    }

    private static List<ModelFileHandle> removeModelConflicts(List<ModelFileHandle> input,
                                                               List<ModelScanError> errors) {
        var byHash = new HashMap<ModelHash, List<ModelFileHandle>>();
        var byLocation = new HashMap<CatalogModelLocation, List<ModelFileHandle>>();
        input.forEach(model -> {
            byHash.computeIfAbsent(model.descriptor().modelHash(), ignored -> new ArrayList<>()).add(model);
            byLocation.computeIfAbsent(model.location(), ignored -> new ArrayList<>()).add(model);
        });
        var rejected = new HashSet<ModelFileHandle>();
        byHash.values().stream().filter(group -> group.size() > 1).forEach(group -> {
            rejected.addAll(group);
            group.forEach(model -> errors.add(ModelScanError.from(model.location().rootKind(),
                    model.location().path().value(), new IOException(
                    "Duplicate builtin model hash " + model.descriptor().modelHash()))));
        });
        byLocation.values().stream().filter(group -> group.size() > 1).forEach(group -> {
            rejected.addAll(group);
            group.forEach(model -> errors.add(ModelScanError.from(model.location().rootKind(),
                    model.location().path().value(), new IOException("Duplicate builtin model path"))));
        });
        return input.stream().filter(model -> !rejected.contains(model))
                .sorted((left, right) -> left.location().compareTo(right.location()))
                .collect(Collectors.toList());
    }

    private static ModelPath relativePath(Path root, Path source) {
        return ModelPath.relativeTo(root, source);
    }

    private static String relative(Path root, Path source) {
        try {
            return relativePath(root, source).value();
        } catch (RuntimeException ignored) {
            return source.toString();
        }
    }
}
