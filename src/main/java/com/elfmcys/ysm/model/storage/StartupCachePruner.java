package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.catalog.RootInventoryState;
import com.elfmcys.ysm.model.catalog.StartupSourceInventory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Bounded structural cleanup performed only while the lifecycle lease is exclusive. */
final class StartupCachePruner {
    private static final int ENTRY_BUDGET = 10_000;
    private static final long TIME_BUDGET_NANOS = java.time.Duration.ofSeconds(2).toNanos();

    private StartupCachePruner() {
    }

    static Report prune(SharedCachePaths paths, ConversionProfileId current) {
        return prune(paths, current, new StartupSourceInventory(java.util.Map.of()));
    }

    static Report prune(SharedCachePaths paths, ConversionProfileId current,
                        StartupSourceInventory inventory) {
        var report = new MutableReport();
        pruneProfiles(paths.convertedObjects(), current.pathComponent(), report);
        pruneProfiles(paths.convertedIndex(), current.pathComponent(), report);
        pruneIncompleteObjects(paths.convertedObjects(current), current, report);
        pruneSourceIndexes(paths, current, inventory, report);
        pruneContents(paths.convertedTemporary(), report);
        pruneContents(paths.convertedQuarantine(), report);
        return report.freeze();
    }

    private static void pruneIncompleteObjects(Path root, ConversionProfileId current,
                                               MutableReport report) {
        if (!safeDirectoryRoot(root, report)) {
            return;
        }
        try (var entries = Files.list(root)) {
            var files = entries.toList();
            for (var file : files) {
                var name = file.getFileName().toString();
                if (Files.isSymbolicLink(file)) {
                    try {
                        if (!report.permit()) {
                            break;
                        }
                        Files.deleteIfExists(file);
                        report.incompleteObjects++;
                    } catch (IOException | RuntimeException error) {
                        report.errors++;
                    }
                    continue;
                }
                Path companion = null;
                if (name.endsWith(".mxc")) {
                    companion = file.resolveSibling(name.substring(0, name.length() - 4) + ".meta");
                } else if (name.endsWith(".meta")) {
                    companion = file.resolveSibling(name.substring(0, name.length() - 5) + ".mxc");
                } else if (!name.contains(".corrupt-")) {
                    continue;
                }
                if (companion == null
                        || !Files.isRegularFile(companion, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        if (!report.permit()) {
                            break;
                        }
                        Files.deleteIfExists(file);
                        report.incompleteObjects++;
                    } catch (IOException | RuntimeException error) {
                        report.errors++;
                    }
                } else if (name.endsWith(".meta")
                        && !validObjectPair(file, companion, current)) {
                    try {
                        var deleted = deleteFile(file, report);
                        deleted |= deleteFile(companion, report);
                        if (deleted) {
                            report.incompleteObjects++;
                        }
                    } catch (IOException | RuntimeException error) {
                        report.errors++;
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            report.errors++;
        }
    }

    private static boolean validObjectPair(Path metadataFile, Path object,
                                           ConversionProfileId current) {
        try {
            var metadata = ConvertedObjectMetadata.read(metadataFile);
            return metadata.key().profile().equals(current)
                    && object.getFileName().toString()
                    .equals(metadata.key().modelHash() + ".mxc")
                    && metadata.containerSize() == Files.size(object);
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
    }

    private static void pruneSourceIndexes(SharedCachePaths paths, ConversionProfileId current,
                                           StartupSourceInventory inventory,
                                           MutableReport report) {
        var root = paths.convertedIndex(current);
        if (!safeDirectoryRoot(root, report)) {
            return;
        }
        var completeRoots = inventory.roots().values().stream()
                .filter(RootInventoryState.Complete.class::isInstance)
                .map(RootInventoryState.Complete.class::cast).toList();
        var store = new ConvertedSourceIndexStore(paths, new AtomicSharedCache(paths), current);
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (!file.getFileName().toString().endsWith(".idx")) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        var index = store.readPath(file);
                        var delete = index.isEmpty();
                        if (index.isPresent()) {
                            var value = index.get();
                            var owner = completeRoots.stream()
                                    .filter(state -> state.root().equals(value.sourceKey().root()))
                                    .findFirst().orElse(null);
                            if (owner != null) {
                                var observation = owner.sources().get(value.sourceKey());
                                delete = observation == null || !observation.stamp()
                                        .equals(value.lastObservedStamp());
                            }
                        }
                        if (delete) {
                            if (deleteFile(file, report)) {
                                report.sourceIndexes++;
                            }
                        }
                    } catch (IOException | RuntimeException error) {
                        report.errors++;
                    }
                    return report.budgetExhausted
                            ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException error) {
            report.errors++;
        }
    }

    private static boolean deleteFile(Path file, MutableReport report) throws IOException {
        if (!report.permit()) {
            return false;
        }
        return Files.deleteIfExists(file);
    }

    private static void pruneProfiles(Path root, String current, MutableReport report) {
        if (!safeDirectoryRoot(root, report)) {
            return;
        }
        try (var entries = Files.list(root)) {
            for (var entry : entries.toList()) {
                if (report.budgetExhausted) {
                    break;
                }
                if (entry.getFileName().toString().equals(current)) {
                    continue;
                }
                try {
                    if (deleteTree(root, entry, report)) {
                        report.oldProfiles++;
                    }
                } catch (IOException | RuntimeException error) {
                    report.errors++;
                }
            }
        } catch (IOException | RuntimeException error) {
            report.errors++;
        }
    }

    private static void pruneContents(Path root, MutableReport report) {
        if (!safeDirectoryRoot(root, report)) {
            return;
        }
        try (var entries = Files.list(root)) {
            for (var entry : entries.toList()) {
                if (report.budgetExhausted) {
                    break;
                }
                try {
                    if (deleteTree(root, entry, report)) {
                        report.temporaryEntries++;
                    }
                } catch (IOException | RuntimeException error) {
                    report.errors++;
                }
            }
        } catch (IOException | RuntimeException error) {
            report.errors++;
        }
    }

    private static boolean safeDirectoryRoot(Path root, MutableReport report) {
        if (Files.isSymbolicLink(root)) {
            try {
                Files.deleteIfExists(root);
            } catch (IOException | RuntimeException error) {
                report.errors++;
            }
            return false;
        }
        return Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS);
    }

    private static boolean deleteTree(Path allowedRoot, Path target,
                                      MutableReport report) throws IOException {
        var root = allowedRoot.toAbsolutePath().normalize();
        var normalized = target.toAbsolutePath().normalize();
        if (normalized.equals(root) || !normalized.startsWith(root)) {
            throw new IOException("Refusing to prune outside converted cache: " + normalized);
        }
        var complete = new boolean[]{true};
        Files.walkFileTree(normalized, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                if (!report.permit()) {
                    complete[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException error)
                    throws IOException {
                if (error != null) {
                    throw error;
                }
                if (!report.permit()) {
                    complete[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                Files.deleteIfExists(directory);
                return FileVisitResult.CONTINUE;
            }
        });
        return complete[0];
    }

    record Report(boolean budgetExhausted, int oldProfilesDeleted,
                  int sourceIndexesDeleted, int incompleteObjectsDeleted,
                  int temporaryEntriesDeleted, int errors) {
    }

    private static final class MutableReport {
        private int oldProfiles;
        private int incompleteObjects;
        private int sourceIndexes;
        private int temporaryEntries;
        private int errors;
        private int remaining = ENTRY_BUDGET;
        private final long deadline = System.nanoTime() + TIME_BUDGET_NANOS;
        private boolean budgetExhausted;

        private boolean permit() {
            if (remaining == 0 || System.nanoTime() >= deadline) {
                budgetExhausted = true;
                return false;
            }
            remaining--;
            return true;
        }

        private Report freeze() {
            return new Report(budgetExhausted, oldProfiles, sourceIndexes, incompleteObjects,
                    temporaryEntries, errors);
        }
    }
}
