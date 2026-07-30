package com.elfmcys.ysm.network;

import com.elfmcys.ysm.buffer.UniBuffer;
import us.hebi.quickbuf.ProtoMessage;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns an optional raw attachment until the transport or receiver consumes it. */
public final class NetworkPayload<T extends ProtoMessage<T>> implements AutoCloseable {
    private final T protobuf;
    private final UniBuffer raw;
    private final AtomicBoolean closed = new AtomicBoolean();

    private NetworkPayload(T protobuf, UniBuffer raw) {
        this.protobuf = Objects.requireNonNull(protobuf, "protobuf");
        this.raw = raw;
    }

    public static <T extends ProtoMessage<T>> NetworkPayload<T> protobuf(T protobuf) {
        return new NetworkPayload<>(protobuf, null);
    }

    /** Transfers ownership of {@code raw} to the returned payload. */
    public static <T extends ProtoMessage<T>> NetworkPayload<T> withRaw(T protobuf, UniBuffer raw) {
        return new NetworkPayload<>(protobuf, Objects.requireNonNull(raw, "raw"));
    }

    public T protobuf() {
        return protobuf;
    }

    public Optional<UniBuffer> raw() {
        return Optional.ofNullable(raw);
    }

    public int rawSize() {
        return raw == null ? 0 : raw.size();
    }

    @Override
    public void close() {
        if (raw != null && closed.compareAndSet(false, true)) {
            raw.close();
        }
    }
}
