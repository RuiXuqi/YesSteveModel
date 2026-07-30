package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.model.domain.ModelDescriptor;

/** Stable metadata-plus-chunk handle; callers never receive an ephemeral ByteBuffer view. */
public interface ModelDataHandle {
    ModelDescriptor descriptor();

    ModelFileView view();

    ChunkDataSource chunks();
}
