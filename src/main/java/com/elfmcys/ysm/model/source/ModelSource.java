package com.elfmcys.ysm.model.source;

/** One immutable catalog input to the client-side source merge. */
public interface ModelSource {
    ModelSourceDescriptor descriptor();

    ResolvedCatalog catalog();
}
