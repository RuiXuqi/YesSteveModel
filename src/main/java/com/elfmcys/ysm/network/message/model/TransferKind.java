package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;

public enum TransferKind {
    CATALOG_FULL,
    CATALOG_DELTA,
    MODEL_ASSET;

    public AssetTransferV0.TransferKind toProto() {
        return switch (this) {
            case CATALOG_FULL -> AssetTransferV0.TransferKind.TRANSFER_KIND_CATALOG_FULL;
            case CATALOG_DELTA -> AssetTransferV0.TransferKind.TRANSFER_KIND_CATALOG_DELTA;
            case MODEL_ASSET -> AssetTransferV0.TransferKind.TRANSFER_KIND_MODEL_ASSET;
        };
    }

    public static TransferKind fromProto(AssetTransferV0.TransferKind value) {
        return switch (value) {
            case TRANSFER_KIND_CATALOG_FULL -> CATALOG_FULL;
            case TRANSFER_KIND_CATALOG_DELTA -> CATALOG_DELTA;
            case TRANSFER_KIND_MODEL_ASSET -> MODEL_ASSET;
            default -> throw new IllegalArgumentException("Unknown model transfer kind: " + value);
        };
    }
}
