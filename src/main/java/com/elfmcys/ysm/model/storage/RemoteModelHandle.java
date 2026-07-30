package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.model.domain.ModelDescriptor;

public record RemoteModelHandle(ModelDescriptor descriptor, RemoteChunkDataSource chunks)
        implements ModelDataHandle {
    public ModelFileView view() {
        return descriptor.view();
    }
}
