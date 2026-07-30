package com.elfmcys.ysm.model.domain;

import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Objects;

public record ModelScanError(Instant occurredAt, CatalogRootKind rootKind, String source,
                             Category category, String code, String message, String detail) {
    public ModelScanError(Instant occurredAt, CatalogRootKind rootKind, String source,
                          String message, String detail) {
        this(occurredAt, rootKind, source, Category.MODEL_SOURCE, "UNCLASSIFIED",
                message, detail);
    }

    public ModelScanError {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(rootKind, "rootKind");
        source = Objects.requireNonNullElse(source, "");
        Objects.requireNonNull(category, "category");
        code = Objects.requireNonNullElse(code, "UNCLASSIFIED");
        message = Objects.requireNonNullElse(message, "Unknown model scan error");
        detail = Objects.requireNonNullElse(detail, "");
    }

    public static ModelScanError from(CatalogRootKind rootKind, String source, Throwable error) {
        var text = new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        return new ModelScanError(Instant.now(), rootKind, source,
                Objects.requireNonNullElse(error.getMessage(), error.getClass().getName()), text.toString());
    }

    public static ModelScanError infrastructure(CatalogRootKind rootKind, String source,
                                                String code, Throwable error) {
        var text = new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        return new ModelScanError(Instant.now(), rootKind, source,
                Category.INFRASTRUCTURE, code,
                Objects.requireNonNullElse(error.getMessage(), error.getClass().getName()),
                text.toString());
    }

    public enum Category {
        MODEL_SOURCE,
        MODEL_PACK,
        CONFLICT,
        CACHE_HINT,
        INFRASTRUCTURE
    }
}
