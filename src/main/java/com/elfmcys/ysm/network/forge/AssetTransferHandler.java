package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.model.server.ServerModelService;
import com.elfmcys.ysm.network.NetworkPayload;
import com.elfmcys.ysm.network.protocol.ProtocolUuid;
import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class AssetTransferHandler {
    private AssetTransferHandler() {
    }

    public static void handleFragmentPayload(NetworkPayload<AssetTransferV0.AssetFragment> payload,
                                             Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (context.getSender() == null) {
            try {
                context.enqueueWork(() -> {
                    try (payload) {
                        ClientModelService.current().ifPresent(service -> service.receive(payload));
                    }
                });
            } catch (Throwable error) {
                payload.close();
                throw error;
            }
        } else {
            payload.close();
        }
        context.setPacketHandled(true);
    }

    public static void handleBatchRequest(AssetTransferV0.ModelAssetBatchRequest request,
                                          Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> ServerModelService.current()
                    .ifPresent(service -> service.handleRequest(sender, request)));
        }
        context.setPacketHandled(true);
    }

    public static void handleBatchFailure(AssetTransferV0.ModelAssetBatchFailure failure,
                                          Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (context.getSender() == null) {
            context.enqueueWork(() -> ClientModelService.current()
                    .ifPresent(service -> service.requestFailed(failure)));
        }
        context.setPacketHandled(true);
    }

    public static void handleCatalogResync(AssetTransferV0.CatalogResyncRequest request,
                                           Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> ServerModelService.current()
                    .ifPresent(service -> service.sendCatalog(sender)));
        }
        context.setPacketHandled(true);
    }

    public static void handleBatchCancel(AssetTransferV0.ModelAssetBatchCancel cancel,
                                         Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null) {
            try {
                var requestId = ProtocolUuid.get(cancel.getRequestId());
                context.enqueueWork(() -> ServerModelService.current()
                        .ifPresent(service -> service.cancelSession(sender.getUUID(), requestId)));
            } catch (RuntimeException ignored) {
            }
        }
        context.setPacketHandled(true);
    }

    public static void handleTransferRelease(AssetTransferV0.AssetTransferRelease release,
                                             Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        var sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> ServerModelService.current()
                    .ifPresent(service -> service.releaseTransfer(sender.getUUID(), release)));
        }
        context.setPacketHandled(true);
    }
}
