package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.NativeBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.buffer.UniBufferIO;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;

import java.util.TreeMap;
import java.util.UUID;

/** Validates and assembles one transfer directly into its final content buffer. */
public final class AssetFragmentAssembler implements AutoCloseable {
    private final UUID transferId;
    private final UUID requestId;
    private final int resourceIndex;
    private final AssetTransferV0.TransferKind kind;
    private final AssetTransferV0.TransferPayloadEncoding encoding;
    private final int decodedSize;
    private final NativeBuffer content;
    private final ModelAssetsProto.ModelAssetManifest manifest;
    private final boolean[] received;
    private final int[] offsets;
    private final int[] lengths;
    private final TreeMap<Integer, Integer> ranges = new TreeMap<>();
    private final long createdAt = System.nanoTime();
    private int receivedCount;
    private int receivedBytes;
    private boolean claimed;
    private boolean closed;

    public AssetFragmentAssembler(NetworkPayload<AssetTransferV0.AssetFragment> first,
                                  int maxEncodedSize, int maxDecodedSize) {
        var header = first.protobuf();
        transferId = ProtocolUuid.get(header.getTransferId());
        requestId = ProtocolUuid.get(header.getRequestId());
        resourceIndex = header.getResourceIndex();
        kind = requireKind(header.getKind());
        encoding = requireEncoding(header.getPayloadEncoding());
        decodedSize = checkedSize(header.getDecodedSize(), maxDecodedSize, "decoded");
        var contentSize = checkedSize(header.getTransferSize(), maxEncodedSize, "content");
        var count = header.getFragmentCount();
        var minimumCount = Math.max(1, (contentSize + ProtocolLimits.FRAGMENT_DATA_BYTES - 1)
                / ProtocolLimits.FRAGMENT_DATA_BYTES);
        var maximumCount = contentSize < ProtocolLimits.MIN_FRAGMENT_DATA_BYTES
                ? Math.max(1, contentSize) : (contentSize + ProtocolLimits.MIN_FRAGMENT_DATA_BYTES - 1)
                / ProtocolLimits.MIN_FRAGMENT_DATA_BYTES;
        if (count < minimumCount || count > maximumCount) {
            throw new IllegalArgumentException("Invalid model transfer fragment count");
        }
        if (kind == AssetTransferV0.TransferKind.TRANSFER_KIND_MODEL_ASSET) {
            if (encoding != AssetTransferV0.TransferPayloadEncoding
                    .TRANSFER_PAYLOAD_ENCODING_RAW_ATTACHMENTS || !header.hasModelAssetManifest()) {
                throw new IllegalArgumentException("Model asset transfer has no manifest or raw attachments");
            }
            manifest = header.getModelAssetManifest();
        } else {
            if (encoding == AssetTransferV0.TransferPayloadEncoding
                    .TRANSFER_PAYLOAD_ENCODING_RAW_ATTACHMENTS || header.hasModelAssetManifest()) {
                throw new IllegalArgumentException("Catalog transfer contains model-asset metadata");
            }
            manifest = null;
        }
        content = NativeBuffer.allocate(contentSize);
        received = new boolean[count];
        offsets = new int[count];
        lengths = new int[count];
        try {
            add(first);
        } catch (RuntimeException error) {
            content.close();
            throw error;
        }
    }

    public synchronized void add(NetworkPayload<AssetTransferV0.AssetFragment> payload) {
        var fragment = payload.protobuf();
        if (!transferId.equals(ProtocolUuid.get(fragment.getTransferId()))
                || !requestId.equals(ProtocolUuid.get(fragment.getRequestId()))
                || resourceIndex != fragment.getResourceIndex()
                || kind != requireKind(fragment.getKind())
                || encoding != requireEncoding(fragment.getPayloadEncoding())
                || decodedSize != fragment.getDecodedSize()
                || content.size() != fragment.getTransferSize()
                || received.length != fragment.getFragmentCount()) {
            throw new IllegalArgumentException("Inconsistent model transfer fragments");
        }
        var index = fragment.getFragmentIndex();
        if (index < 0 || index >= received.length) {
            throw new IllegalArgumentException("Invalid model transfer fragment index");
        }
        if (index != 0 && fragment.hasModelAssetManifest()) {
            throw new IllegalArgumentException("Model asset manifest is only valid on the first fragment");
        }
        var source = fragmentData(payload);
        try {
            var fragmentOffset = fragment.getFragmentOffset();
            if (source.size() > ProtocolLimits.MAX_FRAGMENT_BYTES
                    || fragmentOffset < 0 || fragmentOffset > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid model transfer fragment range");
            }
            var offset = (int) fragmentOffset;
            var length = source.size();
            if (length == 0 && content.size() != 0) {
                throw new IllegalArgumentException("Empty fragment in a non-empty model transfer");
            }
            if (offset < 0 || offset > content.size() - length) {
                throw new IllegalArgumentException("Model transfer fragment exceeds content size");
            }
            if (received[index]) {
                if (offsets[index] != offset || lengths[index] != length
                        || !UniBufferIO.equals(content, offset, source, 0, length)) {
                    throw new IllegalArgumentException("Conflicting duplicate model transfer fragment");
                }
                return;
            }
            var previous = ranges.floorEntry(offset);
            if (previous != null && previous.getValue() > offset) {
                throw new IllegalArgumentException("Overlapping model transfer fragments");
            }
            var next = ranges.ceilingEntry(offset);
            if (next != null && offset + length > next.getKey()) {
                throw new IllegalArgumentException("Overlapping model transfer fragments");
            }
            UniBufferIO.copy(source, 0, content, offset, length);
            ranges.put(offset, offset + length);
            received[index] = true;
            offsets[index] = offset;
            lengths[index] = length;
            receivedCount++;
            receivedBytes += length;
        } finally {
            source.close();
        }
    }

    private UniBuffer fragmentData(NetworkPayload<AssetTransferV0.AssetFragment> payload) {
        var fragment = payload.protobuf();
        if (encoding == AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_PROTOBUF) {
            if (payload.raw().isPresent()) {
                throw new IllegalArgumentException("Protobuf transfer fragment contains a raw tail");
            }
            return fragment.hasProtobufFragment()
                    ? ArrayBuffer.borrow(fragment.getProtobufFragment()) : ArrayBuffer.allocate(0);
        }
        if (fragment.hasProtobufFragment() && fragment.getProtobufFragment().length() != 0) {
            throw new IllegalArgumentException("Raw transfer fragment contains protobuf data");
        }
        return payload.raw().map(UniBuffer::acquire)
                .orElseGet(() -> ArrayBuffer.allocate(0));
    }

    public synchronized boolean isComplete() {
        return receivedCount == received.length && receivedBytes == content.size();
    }

    public synchronized NativeBuffer acquireContent() {
        if (!isComplete()) {
            throw new IllegalStateException("Model transfer is incomplete");
        }
        if (claimed) {
            throw new IllegalStateException("Model transfer content was already claimed");
        }
        claimed = true;
        return content.acquire();
    }

    public UUID requestId() { return requestId; }
    public int resourceIndex() { return resourceIndex; }
    public AssetTransferV0.TransferKind kind() { return kind; }
    public AssetTransferV0.TransferPayloadEncoding encoding() { return encoding; }
    public int decodedSize() { return decodedSize; }
    public ModelAssetsProto.ModelAssetManifest manifest() { return manifest; }
    public long createdAt() { return createdAt; }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            content.close();
        }
    }

    private static int checkedSize(long value, int maximum, String label) {
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException("Invalid " + label + " model transfer size");
        }
        return (int) value;
    }

    private static AssetTransferV0.TransferKind requireKind(AssetTransferV0.TransferKind kind) {
        if (kind == AssetTransferV0.TransferKind.TRANSFER_KIND_UNSPECIFIED) {
            throw new IllegalArgumentException("Model transfer kind is unspecified");
        }
        return kind;
    }

    private static AssetTransferV0.TransferPayloadEncoding requireEncoding(
            AssetTransferV0.TransferPayloadEncoding encoding) {
        if (encoding == AssetTransferV0.TransferPayloadEncoding.TRANSFER_PAYLOAD_ENCODING_UNSPECIFIED) {
            throw new IllegalArgumentException("Model transfer payload encoding is unspecified");
        }
        return encoding;
    }
}
