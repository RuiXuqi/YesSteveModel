package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.network.NetworkPayload;

public abstract class ForgeEnvelope implements AutoCloseable {
    private final ForgeMessageBinding<?> binding;
    private final NetworkPayload<?> payload;
    private final ForgeProtoCodec.PreparedProtobuf encoded;

    protected ForgeEnvelope(ForgeMessageBinding<?> binding, NetworkPayload<?> payload, boolean outbound) {
        this.binding = binding;
        this.payload = payload;
        this.encoded = prepare(binding, payload, outbound);
    }

    private static ForgeProtoCodec.PreparedProtobuf prepare(
            ForgeMessageBinding<?> binding, NetworkPayload<?> payload, boolean outbound) {
        if (!outbound) {
            return null;
        }
        try {
            return ForgeProtoCodec.prepare(payload.protobuf(), binding.spec().maxEncodedBytes());
        } catch (Throwable error) {
            payload.close();
            throw error;
        }
    }

    static ForgeEnvelope inbound(ForgeMessageBinding<?> binding, NetworkPayload<?> payload) {
        return binding.spec().direction() == com.elfmcys.ysm.network.protocol.MessageDirection.CLIENT_TO_SERVER
                ? new ServerboundEnvelope(binding, payload, false)
                : new ClientboundEnvelope(binding, payload, false);
    }

    ForgeMessageBinding<?> binding() {
        return binding;
    }

    NetworkPayload<?> payload() {
        return payload;
    }

    ForgeProtoCodec.PreparedProtobuf encoded() {
        if (encoded == null) {
            throw new IllegalStateException("Inbound envelope has no prepared encoding");
        }
        return encoded;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handle(java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        try {
            ((ForgeMessageBinding) binding).handler().accept(payload, context);
        } catch (RuntimeException error) {
            YesSteveModel.LOGGER.error("Failed to handle YSM protocol message id={} type={}",
                    binding.spec().id(), binding.spec().messageType().getSimpleName(), error);
            throw error;
        }
    }

    @Override
    public void close() {
        if (encoded != null) {
            encoded.close();
        }
        payload.close();
    }
}
