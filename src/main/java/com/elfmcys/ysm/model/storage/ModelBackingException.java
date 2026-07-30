package com.elfmcys.ysm.model.storage;

import java.io.IOException;
import java.util.Objects;

/** A failure caused by the currently published model backing rather than a consumer bug. */
public final class ModelBackingException extends IOException {
    private final ModelBackingIdentity backingIdentity;

    public ModelBackingException(ModelBackingIdentity backingIdentity, String message, Throwable cause) {
        super(message, cause);
        this.backingIdentity = Objects.requireNonNull(backingIdentity, "backingIdentity");
    }

    public ModelBackingIdentity backingIdentity() {
        return backingIdentity;
    }
}
