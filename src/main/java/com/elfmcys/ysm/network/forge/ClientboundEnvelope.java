package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.network.NetworkPayload;

public final class ClientboundEnvelope extends ForgeEnvelope {
    public ClientboundEnvelope(ForgeMessageBinding<?> binding, NetworkPayload<?> payload, boolean outbound) {
        super(binding, payload, outbound);
    }
}
