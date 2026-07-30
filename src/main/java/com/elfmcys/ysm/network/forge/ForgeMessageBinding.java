package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.ProtocolMessageSpec;
import net.minecraftforge.network.NetworkEvent;
import us.hebi.quickbuf.ProtoMessage;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public record ForgeMessageBinding<T extends ProtoMessage<T>>(
        ProtocolMessageSpec<T> spec,
        ForgeProtoCodec.Parser<T> parser,
        BiConsumer<NetworkPayload<T>, Supplier<NetworkEvent.Context>> handler) {
}
