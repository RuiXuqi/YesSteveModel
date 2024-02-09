package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.config.DisableSwitch;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncDisableSwitch {
    private final boolean canSwitch;

    public SyncDisableSwitch(boolean canSwitch) {
        this.canSwitch = canSwitch;
    }

    public static void encode(SyncDisableSwitch message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.canSwitch);
    }

    public static SyncDisableSwitch decode(FriendlyByteBuf buf) {
        return new SyncDisableSwitch(buf.readBoolean());
    }

    public static void handle(SyncDisableSwitch message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> DisableSwitch.CAN_SWITCH = message.canSwitch);
        }
        context.setPacketHandled(true);
    }
}
