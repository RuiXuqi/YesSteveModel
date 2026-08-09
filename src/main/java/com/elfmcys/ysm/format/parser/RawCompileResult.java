package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.model.domain.Hash256;

import java.nio.file.Path;
import java.util.Objects;

/** Result of compiling one raw source before the converted object store validates it. */
public record RawCompileResult(Hash256 modelHash, Path stagedContainer) {
    public RawCompileResult {
        Objects.requireNonNull(modelHash, "modelHash");
        stagedContainer = Objects.requireNonNull(stagedContainer, "stagedContainer")
                .toAbsolutePath().normalize();
    }
}
