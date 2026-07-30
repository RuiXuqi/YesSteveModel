package com.elfmcys.ysm.model.catalog;

public enum ReloadReason {
    STARTUP,
    WATCH_EVENT,
    WATCH_OVERFLOW,
    MANUAL_AUDIT,
    SYSTEM_REPAIR,
    BACKING_RECOVERY
}
