package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelPackDescriptor;
import com.elfmcys.ysm.model.domain.ModelScanReport;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.util.List;

public record CatalogScanResult(List<ModelFileHandle> models, List<ModelPackDescriptor> packs, ModelScanReport report) {
    public CatalogScanResult {
        models = List.copyOf(models);
        packs = List.copyOf(packs);
    }
}
