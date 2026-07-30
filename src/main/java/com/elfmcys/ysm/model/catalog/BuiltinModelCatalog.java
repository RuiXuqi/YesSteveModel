package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.model.importer.LegacyImporter;
import com.elfmcys.ysm.model.importer.RawModelImporter;
import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.storage.ModelStorageInfrastructure;
import com.elfmcys.ysm.model.storage.SharedCachePaths;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Set;

/** Process-stable builtin catalog and its generated default-model backing. */
public final class BuiltinModelCatalog implements AutoCloseable {
    private final BuiltinModelIndex contract;
    private final CatalogScanResult snapshot;
    private final ModelFileHandle defaultHandle;
    private Path defaultTemporaryDirectory;

    private BuiltinModelCatalog(ModelStorageInfrastructure storage,
                                BuiltinModelIndex contract) {
        this.contract = java.util.Objects.requireNonNull(contract, "contract");
        var source = ModelCatalogSources.builtin();
        refreshExamples(source.path());

        var paths = storage.paths();
        var scanner = new BuiltinModelCatalogScanner(storage.objects(),
                new RawModelImporter(new LegacyImporter(), contract));
        final CatalogScanResult loaded;
        GeneratedDefault generatedDefault = null;
        try {
            generatedDefault = generateDefault(source.path(), paths, contract);
            var optional = scanner.scan(source, contract,
                    Set.of(new ModelPath("default")));
            var models = new ArrayList<ModelFileHandle>();
            models.add(generatedDefault.handle());
            models.addAll(optional.models());
            loaded = new CatalogScanResult(models, optional.packs(), optional.report());
        } catch (IOException error) {
            cleanupGeneratedDefault(generatedDefault, error);
            YesSteveModel.LOGGER.error("Failed to load the builtin model index/catalog", error);
            throw new UncheckedIOException("Failed to scan builtin model resources", error);
        } catch (RuntimeException | Error error) {
            cleanupGeneratedDefault(generatedDefault, error);
            throw error;
        }

        for (var error : loaded.report().errors()) {
            YesSteveModel.LOGGER.debug(
                    "Builtin model scan rejected source={}: {}\n{}",
                    error.source(), error.message(), error.detail());
        }
        if (loaded.models().stream().noneMatch(BuiltinModelCatalog::isDefault)) {
            throw new IllegalStateException("The builtin default model could not be generated");
        }

        var generated = java.util.Objects.requireNonNull(generatedDefault, "generatedDefault");
        snapshot = loaded;
        defaultHandle = generated.handle();
        defaultTemporaryDirectory = generated.temporaryDirectory();
        YesSteveModel.LOGGER.info(
                "Loaded builtin model catalog: mode={}, models={}, packs={}, errors={}",
                "unconditional-default+indexed-optional",
                loaded.models().size(), loaded.packs().size(), loaded.report().errorCount());
    }

    public static BuiltinModelCatalog open(ModelStorageInfrastructure storage,
                                           BuiltinModelIndex contract) {
        var catalog = new BuiltinModelCatalog(storage, contract);
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            return catalog;
        }
        try {
            catalog.finalizeDefaultResidency();
            return catalog;
        } catch (RuntimeException | Error error) {
            try {
                catalog.close();
            } catch (RuntimeException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }

    public BuiltinModelIndex contract() {
        return contract;
    }

    public CatalogScanResult snapshot() {
        return snapshot;
    }

    private static boolean isDefault(ModelFileHandle handle) {
        var path = handle.location().path().value();
        return handle.location().rootKind() == CatalogRootKind.BUILTIN
                && (path.equals("default") || path.equals("default.mxc"));
    }

    public synchronized void finalizeDefaultResidency() {
        try {
            if (!defaultHandle.residentOnly()) {
                defaultHandle.retainChunksInMemory(DefaultLazyChunkPlan.collect(defaultHandle.view()));
            }
            if (defaultHandle.descriptor().hasEncodedRepresentation()) {
                defaultHandle.discardEncodedRepresentation();
            }
            if (defaultTemporaryDirectory != null) {
                deleteRecursively(defaultTemporaryDirectory);
                defaultTemporaryDirectory = null;
            }
        } catch (IOException error) {
            throw new UncheckedIOException(
                    "Failed to retain builtin default lazy chunks", error);
        }
    }

    private static GeneratedDefault generateDefault(Path builtinRoot,
                                                    SharedCachePaths paths,
                                                    BuiltinModelIndex index)
            throws IOException {
        Files.createDirectories(paths.temporary());
        var temporaryDirectory = Files.createTempDirectory(
                paths.temporary(), "builtin-default-startup-");
        var source = builtinRoot.resolve("default");
        try {
            final Path generated;
            try (var vfs = new Directory(source)) {
                generated = ModelParser.parseBuiltinDefault(vfs, temporaryDirectory);
            }
            var location = new CatalogModelLocation(
                    CatalogRootKind.BUILTIN, new ModelPath("default"));
            var handle = ModelFileHandle.openResidentDefault(generated, location);
            var expected = index.require(location.path());
            if (!expected.equals(handle.descriptor().modelHash())) {
                throw new IOException("Builtin default model hash mismatch: index="
                        + expected + ", generated=" + handle.descriptor().modelHash());
            }
            return new GeneratedDefault(handle, temporaryDirectory);
        } catch (IOException | RuntimeException error) {
            try {
                deleteRecursively(temporaryDirectory);
            } catch (IOException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
    }

    private static void refreshExamples(Path builtinRoot) {
        refreshExamples(builtinRoot, FMLPaths.GAMEDIR.get().resolve(YesSteveModel.MOD_ID));
    }

    static void refreshExamples(Path builtinRoot, Path config) {
        try {
            deleteRecursively(config.resolve("builtin"));
        } catch (IOException | RuntimeException error) {
            YesSteveModel.LOGGER.error("Failed to remove the legacy builtin model directory", error);
        }

        var example = config.resolve("example");
        try {
            deleteRecursively(example);
            copyDirectory(builtinRoot, example);
        } catch (IOException | RuntimeException error) {
            YesSteveModel.LOGGER.error("Failed to extract builtin models into the example directory", error);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                if (error != null) {
                    throw error;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file).toString()),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void cleanupGeneratedDefault(GeneratedDefault generated,
                                                Throwable failure) {
        if (generated == null) {
            return;
        }
        try {
            deleteRecursively(generated.temporaryDirectory());
        } catch (IOException cleanupError) {
            failure.addSuppressed(cleanupError);
        }
    }

    @Override
    public synchronized void close() {
        if (defaultTemporaryDirectory == null) {
            return;
        }
        try {
            deleteRecursively(defaultTemporaryDirectory);
            defaultTemporaryDirectory = null;
        } catch (IOException error) {
            throw new UncheckedIOException(
                    "Failed to delete the builtin default temporary directory", error);
        }
    }

    private record GeneratedDefault(ModelFileHandle handle, Path temporaryDirectory) {
    }
}
