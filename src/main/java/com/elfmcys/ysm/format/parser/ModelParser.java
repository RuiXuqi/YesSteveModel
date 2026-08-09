package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.vfs.VirtualFileSystem;
import com.elfmcys.ysm.model.domain.Hash256;

import java.nio.file.Path;
import java.util.Objects;

/** Coordinates raw source access and model assembly. The caller retains ownership of the VFS. */
public final class ModelParser {
    public static final String CANONICALIZER_PROFILE_VERSION = "1";
    public static final String PARSER_PROFILE_VERSION = "1";
    public static final String IMAGE_POLICY_PROFILE_VERSION = "1";

    private ModelParser() {
    }

    public static Path parse(VirtualFileSystem vfs, Path outputDirectory,
                             DefaultAnimationFilter defaultAnimations) {
        return compile(vfs, outputDirectory, defaultAnimations).stagedContainer();
    }

    public static RawCompileResult compile(VirtualFileSystem vfs, Path outputDirectory,
                                           DefaultAnimationFilter defaultAnimations) {
        var result = parse(vfs, Objects.requireNonNull(outputDirectory, "outputDirectory"),
                false, defaultAnimations, true);
        return new RawCompileResult(result.modelHash(), Objects.requireNonNull(result.output(), "output"));
    }

    /** Generates the builtin default without applying the default-animation filter to itself. */
    public static Path parseBuiltinDefault(VirtualFileSystem vfs, Path outputDirectory) {
        return Objects.requireNonNull(parse(vfs,
                Objects.requireNonNull(outputDirectory, "outputDirectory"),
                false, DefaultAnimationFilter.keepAll(), false).output(),
                "output");
    }

    /** Scans the parse resource set without decoding assets or writing a model container. */
    public static Hash256 scanModelHash(VirtualFileSystem vfs) {
        return parse(vfs, null, true, DefaultAnimationFilter.keepAll(), false).modelHash();
    }

    private static RawModelAssembler.Result parse(VirtualFileSystem vfs, Path outputDirectory,
                                                  boolean dryRun,
                                                  DefaultAnimationFilter defaultAnimations,
                                                  boolean recompressImages) {
        var source = new RawModelSource(Objects.requireNonNull(vfs, "vfs"));
        try (var assembler = new RawModelAssembler(source, outputDirectory, dryRun,
                Objects.requireNonNull(defaultAnimations, "defaultAnimations"), recompressImages)) {
            return assembler.parse();
        }
    }
}
