package com.elfmcys.ysm.model.source;

import java.util.Objects;

public record ModelSourceDescriptor(SourceId id,
                                    SourceKind kind,
                                    int priority) {
    public ModelSourceDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
    }
}
