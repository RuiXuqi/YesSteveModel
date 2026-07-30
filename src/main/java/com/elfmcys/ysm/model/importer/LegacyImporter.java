package com.elfmcys.ysm.model.importer;

import com.elfmcys.ysm.format.legacy.LegacyYsmHeader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Placeholder for the optional encrypted V3 importer. V3 is intentionally not
 * part of the normal raw VFS path and no legacy protection mechanism is restored.
 */
public final class LegacyImporter {
    public static final String PROFILE_VERSION = "0.1.0-unstable:no-v3-importer";

    public boolean recognizes(Path source) {
        if (!source.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                .endsWith(".ysm")) {
            return false;
        }
        try {
            return LegacyYsmHeader.probe(source) == LegacyYsmHeader.Version.V3_ENCRYPTED;
        } catch (IOException error) {
            throw new UncheckedIOException("Invalid .ysm source header", error);
        }
    }

    public Path importModel(Path source, Path outputDirectory) throws IOException {
        throw new IOException(
                "Encrypted V3 .ysm import is unavailable in 0.1.0-unstable");
    }
}
