package com.elfmcys.ysm.model.importer;

import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.parser.DefaultAnimationFilter;
import com.elfmcys.ysm.format.parser.RawCompileResult;
import com.elfmcys.ysm.format.vfs.Directory;
import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.natives.NativeArchive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class RawModelImporter {
    private final LegacyImporter v3Importer;
    private final DefaultAnimationFilter defaultAnimations;

    public RawModelImporter(LegacyImporter v3Importer,
                            DefaultAnimationFilter defaultAnimations) {
        this.v3Importer = Objects.requireNonNull(v3Importer, "v3Importer");
        this.defaultAnimations = Objects.requireNonNull(defaultAnimations, "defaultAnimations");
    }

    public RawCompileResult convert(Path source, Path outputDirectory) {
        if (v3Importer.recognizes(source)) {
            try {
                var output = v3Importer.importModel(source, outputDirectory);
                var handle = ModelFileHandle.openDirect(output, new CatalogModelLocation(
                        CatalogRootKind.CUSTOM, new ModelPath("legacy-import.mxc")));
                return new RawCompileResult(handle.descriptor().modelHash(), output);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (Files.isDirectory(source)) {
            try (var vfs = new Directory(source)) {
                return ModelParser.compile(vfs, outputDirectory, defaultAnimations);
            }
        }
        try (var vfs = new NativeArchive(source.toString())) {
            return ModelParser.compile(vfs, outputDirectory, defaultAnimations);
        }
    }

    public boolean supportsHashProbe(Path source) {
        return !v3Importer.recognizes(source);
    }

    public com.elfmcys.ysm.model.domain.ModelHash scanModelHash(Path source) {
        if (!supportsHashProbe(source)) {
            throw new IllegalArgumentException("Legacy model source does not support hash probing");
        }
        if (Files.isDirectory(source)) {
            try (var vfs = new Directory(source)) {
                return ModelParser.scanModelHash(vfs);
            }
        }
        try (var vfs = new NativeArchive(source.toString())) {
            return ModelParser.scanModelHash(vfs);
        }
    }
}
