package com.elfmcys.ysm.model.catalog;

import java.util.Objects;

public record ReloadResult(long reloadGeneration, boolean committed,
                           boolean catalogChanged, int modelCount, int packCount,
                           int errorCount, ReloadStats stats, String failureMessage) {
    public ReloadResult {
        Objects.requireNonNull(stats, "stats");
        failureMessage = Objects.requireNonNullElse(failureMessage, "");
    }

    public static ReloadResult failed(ReloadableCatalogSnapshot current, String message) {
        return new ReloadResult(current.reloadGeneration(), false, false,
                current.models().size(), current.packs().size(),
                current.report().errorCount(), ReloadStats.empty(), message);
    }

    public static ReloadResult unchanged(ReloadableCatalogSnapshot current) {
        return new ReloadResult(current.reloadGeneration(), true, false,
                current.models().size(), current.packs().size(),
                current.report().errorCount(), ReloadStats.empty(), "");
    }
}
