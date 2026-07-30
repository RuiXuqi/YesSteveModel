package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.catalog.CatalogModelLocation;
import com.elfmcys.ysm.model.domain.ModelHash;

import java.io.IOException;

/** Indicates that packaged raw builtin content no longer matches its generated index. */
public final class ModelHashMismatchException extends IOException {
    public ModelHashMismatchException(CatalogModelLocation location, ModelHash expected,
                                      ModelHash actual) {
        super("Builtin model hash mismatch at " + location.path() + ": expected=" + expected
                + ", actual=" + actual);
    }
}
