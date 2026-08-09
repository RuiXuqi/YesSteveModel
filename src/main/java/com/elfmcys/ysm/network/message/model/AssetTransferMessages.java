package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.source.CatalogCursor;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.util.ProtoBytes;

import java.util.UUID;

/** Validation and construction helpers for the transport-independent asset protocol. */
public final class AssetTransferMessages {
    private AssetTransferMessages() {
    }

    public static AssetTransferV0.CatalogCursor cursor(CatalogCursor cursor) {
        return AssetTransferV0.CatalogCursor.newInstance()
                .setEpoch(cursor.epoch())
                .setRevision(cursor.revision());
    }

    public static boolean matches(AssetTransferV0.CatalogCursor cursor, byte[] epoch, long revision) {
        return cursor.getEpoch().length() == ProtocolUuid.SIZE
                && ProtoBytes.equals(epoch, cursor.getEpoch())
                && cursor.getRevision() == revision;
    }

    public static UUID requestId(AssetTransferV0.ModelAssetBatchRequest request) {
        return ProtocolUuid.get(request.getRequestId());
    }

    public static UUID requestId(AssetTransferV0.ModelAssetBatchFailure failure) {
        return ProtocolUuid.get(failure.getRequestId());
    }

    public static AssetTransferV0.ModelAssetBatchFailure sessionFailure(
            UUID requestId, AssetTransferV0.AssetFailureReason reason) {
        var failure = AssetTransferV0.ModelAssetBatchFailure.newInstance();
        ProtocolUuid.set(failure.getMutableRequestId(), requestId);
        return failure.setSession(AssetTransferV0.SessionFailure.newInstance().setReason(reason));
    }

    public static AssetTransferV0.ModelAssetBatchFailure resourceFailure(
            UUID requestId, int resourceIndex, AssetTransferV0.AssetFailureReason reason) {
        if (resourceIndex < 0) {
            throw new IllegalArgumentException("Resource index must not be negative");
        }
        var failure = AssetTransferV0.ModelAssetBatchFailure.newInstance();
        ProtocolUuid.set(failure.getMutableRequestId(), requestId);
        return failure.setResource(AssetTransferV0.ResourceFailure.newInstance()
                .setResourceIndex(resourceIndex)
                .setReason(reason));
    }

    public static Hash256 modelHash(us.hebi.quickbuf.RepeatedByte value, String field) {
        if (value.length() != Hash256.SIZE) {
            throw new IllegalArgumentException(field + " must contain exactly " + Hash256.SIZE + " bytes");
        }
        return new Hash256(value.array(), 0, value.length());
    }

    public static AssetTransferV0.CatalogResyncRequest resync(CatalogCursor known) {
        var request = AssetTransferV0.CatalogResyncRequest.newInstance();
        if (known != null) {
            request.setKnownCursor(cursor(known));
        }
        return request;
    }

    public static AssetTransferV0.ModelAssetBatchCancel cancel(UUID requestId) {
        var cancel = AssetTransferV0.ModelAssetBatchCancel.newInstance();
        ProtocolUuid.set(cancel.getMutableRequestId(), requestId);
        return cancel;
    }

    public static AssetTransferV0.AssetTransferRelease release(
            UUID transferId, UUID requestId, int resourceIndex) {
        var release = AssetTransferV0.AssetTransferRelease.newInstance()
                .setResourceIndex(resourceIndex);
        ProtocolUuid.set(release.getMutableTransferId(), transferId);
        ProtocolUuid.set(release.getMutableRequestId(), requestId);
        return release;
    }
}
