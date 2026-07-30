package com.elfmcys.ysm.model.catalog;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public record ReloadRequest(Set<ReloadReason> reasons, AuditLevel auditLevel,
                            Set<Path> touchedPaths,
                            Set<BackingRecoveryRequest> recoveries) {
    public ReloadRequest {
        reasons = Set.copyOf(reasons);
        Objects.requireNonNull(auditLevel, "auditLevel");
        touchedPaths = touchedPaths.stream().map(path -> path.toAbsolutePath().normalize())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        recoveries = Set.copyOf(recoveries);
    }

    public static ReloadRequest startup() {
        return new ReloadRequest(Set.of(ReloadReason.STARTUP), AuditLevel.FULL_CONTENT,
                Set.of(), Set.of());
    }

    public static ReloadRequest manual() {
        return new ReloadRequest(Set.of(ReloadReason.MANUAL_AUDIT),
                AuditLevel.FULL_CONTENT, Set.of(), Set.of());
    }
}
