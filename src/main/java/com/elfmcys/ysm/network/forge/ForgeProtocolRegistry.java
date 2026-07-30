package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.network.protocol.MessageDirection;
import us.hebi.quickbuf.ProtoMessage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ForgeProtocolRegistry {
    private static final Map<Integer, ForgeMessageBinding<?>> CLIENTBOUND = new ConcurrentHashMap<>();
    private static final Map<Integer, ForgeMessageBinding<?>> SERVERBOUND = new ConcurrentHashMap<>();

    private ForgeProtocolRegistry() {
    }

    public static synchronized <T extends ProtoMessage<T>> void add(ForgeMessageBinding<T> binding) {
        var bindings = binding.spec().direction() == MessageDirection.CLIENT_TO_SERVER
                ? SERVERBOUND : CLIENTBOUND;
        if (bindings.putIfAbsent(binding.spec().id(), binding) != null) {
            throw new IllegalArgumentException("Duplicate Forge protocol binding: " + binding.spec().id());
        }
    }

    public static Optional<ForgeMessageBinding<?>> find(int id, MessageDirection direction) {
        return Optional.ofNullable((direction == MessageDirection.CLIENT_TO_SERVER
                ? SERVERBOUND : CLIENTBOUND).get(id));
    }
}
