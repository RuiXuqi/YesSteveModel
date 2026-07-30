package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.network.NetworkPayload;

public final class ServerboundEnvelope extends ForgeEnvelope {
    public ServerboundEnvelope(ForgeMessageBinding<?> binding, NetworkPayload<?> payload, boolean outbound) {
        super(binding, payload, outbound);
    }
}
