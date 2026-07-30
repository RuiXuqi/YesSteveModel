package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.MessageDirection;
import com.elfmcys.ysm.network.protocol.ProtocolMessageSpec;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeProtoCodecTest {
    private static final int MESSAGE_ID = 10_001;
    private static final ForgeMessageBinding<AssetTransferV0.AssetFragment> BINDING =
            new ForgeMessageBinding<>(new ProtocolMessageSpec<>(MESSAGE_ID,
                    MessageDirection.CLIENT_TO_SERVER, AssetTransferV0.AssetFragment.class, 1024 * 1024),
                    AssetTransferV0.AssetFragment::parseFrom, (payload, context) -> payload.close());

    static {
        ForgeProtocolRegistry.add(BINDING);
    }

    @Test
    void smallProtobufStaysRaw() {
        var message = AssetTransferV0.AssetFragment.newInstance().setResourceIndex(1);
        try (var prepared = ForgeProtoCodec.prepare(message, 1024)) {
            assertEquals(ForgeProtoCodec.ProtobufEncoding.RAW, prepared.encoding());
            assertTrue(prepared.decodedSize() < ForgeProtoCodec.COMPRESSION_THRESHOLD);
        }
    }

    @Test
    void compressionWithoutBenefitFallsBackToSimpleFrameEvenAboveThreshold() {
        var random = new byte[256];
        new Random(7).nextBytes(random);
        var message = AssetTransferV0.AssetFragment.newInstance().setProtobufFragment(random);
        try (var prepared = ForgeProtoCodec.prepare(message, 1024)) {
            assertEquals(ForgeProtoCodec.ProtobufEncoding.RAW, prepared.encoding());
            assertTrue(prepared.decodedSize() >= ForgeProtoCodec.COMPRESSION_THRESHOLD);
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ForgeProtoCodec.encode(new ServerboundEnvelope(
                BINDING, NetworkPayload.protobuf(message), true), buffer);
        assertEquals(MESSAGE_ID << 1, buffer.readVarInt());
        buffer.release();
    }

    @Test
    void compressibleDynamicProtobufUsesZstdTenAndFullFrame() {
        var message = AssetTransferV0.AssetFragment.newInstance()
                .setProtobufFragment(new byte[4096]);
        try (var prepared = ForgeProtoCodec.prepare(message, 8192)) {
            assertEquals(ForgeProtoCodec.ProtobufEncoding.ZSTD, prepared.encoding());
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ForgeProtoCodec.encode(new ServerboundEnvelope(
                BINDING, NetworkPayload.protobuf(message), true), buffer);
        assertEquals((MESSAGE_ID << 1) | 1, buffer.readVarInt());
        assertEquals(ForgeProtoCodec.FLAG_PROTOBUF_ZSTD, buffer.readVarInt());
        buffer.release();
    }

    @Test
    void rawTailForcesFullFrameAndRoundTripsByteIdentically() {
        var expected = new byte[]{9, 8, 7, 6, 5};
        var message = AssetTransferV0.AssetFragment.newInstance().setResourceIndex(4);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        ForgeProtoCodec.encode(new ServerboundEnvelope(BINDING,
                NetworkPayload.withRaw(message, ArrayBuffer.move(expected.clone())), true), buffer);

        buffer.markReaderIndex();
        var tag = buffer.readVarInt();
        buffer.resetReaderIndex();
        assertTrue((tag & 1) != 0);
        try (var decoded = ForgeProtoCodec.decode(buffer, MessageDirection.CLIENT_TO_SERVER);
             var raw = decoded.payload().raw().orElseThrow().acquire();
             var array = raw.acquireArray()) {
            assertEquals(4, ((AssetTransferV0.AssetFragment)
                    decoded.payload().protobuf()).getResourceIndex());
            assertArrayEquals(expected, java.util.Arrays.copyOfRange(array.array(),
                    array.arrayOffset(), array.arrayOffset() + array.size()));
        }
        buffer.release();
    }
}
