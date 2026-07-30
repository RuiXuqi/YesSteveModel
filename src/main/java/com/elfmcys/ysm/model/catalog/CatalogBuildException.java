package com.elfmcys.ysm.model.catalog;

import java.io.IOException;

public sealed class CatalogBuildException extends IOException permits
        CatalogInfrastructureException, CatalogInventoryChangedException,
        ModelSourceException, ModelPackException {
    CatalogBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    CatalogBuildException(String message) {
        super(message);
    }
}
