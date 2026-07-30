package com.elfmcys.ysm.model.domain;

import java.time.Instant;
import java.util.List;

public record ModelScanReport(Instant startedAt, Instant completedAt, List<ModelScanError> errors) {
    public ModelScanReport {
        errors = List.copyOf(errors);
    }

    public static ModelScanReport empty() {
        var now = Instant.now();
        return new ModelScanReport(now, now, List.of());
    }

    public int errorCount() {
        return errors.size();
    }
}
