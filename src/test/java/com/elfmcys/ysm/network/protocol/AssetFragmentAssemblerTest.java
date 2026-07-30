package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetFragmentAssemblerTest {
    private static final UUID TRANSFER_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();

    @Test
    void assemblesRawAttachmentsIntoOneFinalBuffer() {
        try (var first = fragment(0, 0, 2, 4, new byte[]{1, 2});
             var assembler = assembler(first);
             var second = fragment(1, 2, 2, 4, new byte[]{3, 4})) {
            assembler.add(second);

            assertTrue(assembler.isComplete());
            try (var content = assembler.acquireContent();
                 var array = content.acquireArray()) {
                assertArrayEquals(new byte[]{1, 2, 3, 4}, java.util.Arrays.copyOfRange(
                        array.array(), array.arrayOffset(), array.arrayOffset() + array.size()));
            }
            assertThrows(IllegalStateException.class, assembler::acquireContent);
        }
    }

    @Test
    void acceptsIdenticalDuplicateButRejectsConflictingDuplicate() {
        try (var first = fragment(0, 0, 2, 4, new byte[]{1, 2});
             var assembler = assembler(first);
             var duplicate = fragment(0, 0, 2, 4, new byte[]{1, 2})) {
            assembler.add(duplicate);
            assertFalse(assembler.isComplete());
            try (var conflicting = fragment(0, 0, 2, 4, new byte[]{9, 2})) {
                assertThrows(IllegalArgumentException.class, () -> assembler.add(conflicting));
            }
        }
    }

    @Test
    void rejectsOverlappingRangesAndMissingCoverage() {
        try (var first = fragment(0, 0, 2, 5, new byte[]{1, 2, 3});
             var overlap = assembler(first);
             var second = fragment(1, 2, 2, 5, new byte[]{4, 5, 6})) {
            assertThrows(IllegalArgumentException.class, () -> overlap.add(second));
        }

        try (var first = fragment(0, 0, 2, 5, new byte[]{1, 2});
             var gap = assembler(first);
             var second = fragment(1, 3, 2, 5, new byte[]{4, 5})) {
            gap.add(second);
            assertFalse(gap.isComplete());
            assertThrows(IllegalStateException.class, gap::acquireContent);
        }
    }

    @Test
    void rejectsFragmentMetadataThatWouldExceedThePayloadDerivedIndexBound() {
        try (var fragment = fragment(0, 0, 5, 4, new byte[]{1})) {
            assertThrows(IllegalArgumentException.class, () -> assembler(fragment));
        }
    }

    private static AssetFragmentAssembler assembler(
            NetworkPayload<AssetTransferV0.AssetFragment> first) {
        return new AssetFragmentAssembler(first, 16, 16);
    }

    private static NetworkPayload<AssetTransferV0.AssetFragment> fragment(
            int index, int offset, int count, int contentSize, byte[] data) {
        var fragment = AssetTransferV0.AssetFragment.newInstance()
                .setResourceIndex(3)
                .setKind(AssetTransferV0.TransferKind.TRANSFER_KIND_MODEL_ASSET)
                .setDecodedSize(8)
                .setTransferSize(contentSize)
                .setFragmentOffset(offset)
                .setFragmentIndex(index)
                .setFragmentCount(count)
                .setPayloadEncoding(AssetTransferV0.TransferPayloadEncoding
                        .TRANSFER_PAYLOAD_ENCODING_RAW_ATTACHMENTS);
        if (index == 0) {
            fragment.setModelAssetManifest(ModelAssetsProto.ModelAssetManifest.newInstance());
        }
        ProtocolUuid.set(fragment.getMutableTransferId(), TRANSFER_ID);
        ProtocolUuid.set(fragment.getMutableRequestId(), REQUEST_ID);
        return NetworkPayload.withRaw(fragment, ArrayBuffer.move(data));
    }
}
