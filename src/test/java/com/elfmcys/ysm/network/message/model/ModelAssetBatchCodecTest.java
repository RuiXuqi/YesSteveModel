package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.util.ProtoUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelAssetBatchCodecTest {
    @Test
    void roundTripsMixedModelAndPackResources() throws Exception {
        var sessionId = UUID.randomUUID();
        var modelHash = hash((byte) 1);
        var descriptorHash = hash((byte) 2);
        var coverHash = hash((byte) 3);
        var request = AssetTransferV0.ModelAssetBatchRequest.newInstance()
                .setCursor(AssetTransferV0.CatalogCursor.newInstance()
                        .setEpoch(new byte[16]).setRevision(99))
                .addResources(AssetTransferV0.AssetResourceRequest.newInstance()
                        .setSubject(ModelAssetProtoMapper.toProto(new ModelAssetSubject.Model(
                                modelHash, descriptorHash)))
                        .setSelector(ModelAssetProtoMapper.toProto(ModelAssetSelector.preview())))
                .addResources(AssetTransferV0.AssetResourceRequest.newInstance()
                        .setSubject(ModelAssetProtoMapper.toProto(
                                new ModelAssetSubject.Pack("remote", "packs/demo")))
                        .setSelector(ModelAssetProtoMapper.toProto(ModelAssetSelector.packCover(coverHash))));
        ProtocolUuid.set(request.getMutableRequestId(), sessionId);

        var decoded = AssetTransferV0.ModelAssetBatchRequest.parseFrom(
                ProtoUtil.serializeToArray(request));

        assertEquals(sessionId, ProtocolUuid.get(decoded.getRequestId()));
        assertEquals(99, decoded.getCursor().getRevision());
        assertEquals(2, decoded.getResources().length());
        assertArrayEquals(modelHash.bytes(),
                com.elfmcys.ysm.util.ProtoBytes.copy(
                        decoded.getResources().get(0).getSubject().getModel().getModelHash()));
        assertArrayEquals(descriptorHash.bytes(),
                com.elfmcys.ysm.util.ProtoBytes.copy(
                        decoded.getResources().get(0).getSubject().getModel().getDescriptorHash()));
        assertEquals("packs/demo", decoded.getResources().get(1).getSubject().getPack().getHierarchy());
        assertArrayEquals(coverHash.bytes(),
                com.elfmcys.ysm.util.ProtoBytes.copy(decoded.getResources().get(1)
                        .getSelector().getPackCover().getExpectedContentHash()));
    }

    @Test
    void fragmentRoundTripPreservesResourceIndex() throws Exception {
        var transferId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var fragment = AssetTransferV0.AssetFragment.newInstance()
                .setResourceIndex(3)
                .setKind(TransferKind.MODEL_ASSET.toProto())
                .setDecodedSize(128)
                .setTransferSize(3)
                .setFragmentOffset(0)
                .setFragmentIndex(0)
                .setFragmentCount(1)
                .setPayloadEncoding(AssetTransferV0.TransferPayloadEncoding
                        .TRANSFER_PAYLOAD_ENCODING_PROTOBUF)
                .setProtobufFragment(new byte[]{1, 2, 3});
        ProtocolUuid.set(fragment.getMutableTransferId(), transferId);
        ProtocolUuid.set(fragment.getMutableRequestId(), sessionId);

        var decoded = AssetTransferV0.AssetFragment.parseFrom(ProtoUtil.serializeToArray(fragment));

        assertEquals(transferId, ProtocolUuid.get(decoded.getTransferId()));
        assertEquals(sessionId, ProtocolUuid.get(decoded.getRequestId()));
        assertEquals(3, decoded.getResourceIndex());
        assertEquals(TransferKind.MODEL_ASSET, TransferKind.fromProto(decoded.getKind()));
        assertEquals(3, decoded.getProtobufFragment().length());
    }

    private static ModelHash hash(byte fill) {
        var bytes = new byte[ModelHash.SIZE];
        java.util.Arrays.fill(bytes, fill);
        return new ModelHash(bytes);
    }
}
