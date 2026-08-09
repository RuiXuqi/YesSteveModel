package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.client.model.internal.asset.ClientAssetRepository;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.PackOffer;
import com.elfmcys.ysm.natives.image.ImageSource;

import java.util.concurrent.CompletableFuture;

/** Explicitly collects asset loads that may share one network distribution session. */
public final class ClientAssetBatch {
    private final ClientAssetRepository.Batch delegate;

    ClientAssetBatch(ClientAssetRepository.Batch delegate) {
        this.delegate = delegate;
    }

    public CompletableFuture<ImageSource> preview(Hash256 hash) {
        return delegate.preview(hash);
    }

    public CompletableFuture<ImageSource> packCover(PackOffer pack) {
        return delegate.packCover(pack);
    }

    public CompletableFuture<ImageSource> presentation(Hash256 hash,
                                                       ModelAssetSelector.PresentationAsset asset,
                                                       int index) {
        return delegate.presentation(hash, asset, index);
    }

    public void submit() {
        delegate.submit();
    }
}
