package com.elfmcys.ysm.network.protocol;

import com.elfmcys.ysm.proto.network.protocol.v0.AssetTransferV0;
import com.elfmcys.ysm.proto.network.protocol.v0.ControlV0;
import com.elfmcys.ysm.proto.network.protocol.v0.HandshakeV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import com.elfmcys.ysm.proto.network.protocol.v0.minecraft.MinecraftStateV0;

public final class ProtocolMessages {
    public static final int CLIENT_HELLO_ID = 0;
    public static final int SERVER_HELLO_ID = 1;
    public static final int PLAYER_STATE_REPORT_ID = 2;
    public static final int PLAYER_STATE_UPDATE_ID = 3;
    public static final int SELECT_MODEL_REQUEST_ID = 4;
    public static final int AUTHORIZED_MODELS_SNAPSHOT_ID = 5;
    public static final int STARRED_MODELS_SNAPSHOT_ID = 6;
    public static final int UPDATE_STARRED_MODEL_REQUEST_ID = 7;
    public static final int ENTITY_ANIMATION_ACTION_REQUEST_ID = 8;
    public static final int EXECUTE_MOLANG_EVENT_ID = 9;
    public static final int SUBMIT_ROULETTE_EXPRESSION_REQUEST_ID = 10;
    public static final int EMIT_MOLANG_SYNC_ID = 11;
    public static final int MOLANG_SYNC_EVENT_ID = 12;
    public static final int SWING_HAND_REQUEST_ID = 13;
    public static final int PROJECTILE_MODEL_STATE_ID = 14;
    public static final int VEHICLE_MODEL_STATE_ID = 15;
    public static final int ASSET_FRAGMENT_ID = 16;
    public static final int MODEL_ASSET_BATCH_REQUEST_ID = 17;
    public static final int MODEL_ASSET_BATCH_FAILURE_ID = 18;
    public static final int CATALOG_RESYNC_REQUEST_ID = 19;
    public static final int MODEL_ASSET_BATCH_CANCEL_ID = 20;
    public static final int ASSET_TRANSFER_RELEASE_ID = 21;

    public static final ProtocolMessageRegistry REGISTRY = ProtocolMessageRegistry.builder()
            .add(spec(CLIENT_HELLO_ID, MessageDirection.CLIENT_TO_SERVER,
                    HandshakeV0.ClientHello.class, ProtocolLimits.MAX_HELLO_BYTES))
            .add(spec(SERVER_HELLO_ID, MessageDirection.SERVER_TO_CLIENT,
                    HandshakeV0.ServerHello.class, ProtocolLimits.MAX_HELLO_BYTES))
            .add(spec(PLAYER_STATE_REPORT_ID, MessageDirection.CLIENT_TO_SERVER,
                    PlayerStateV0.PlayerStateReport.class, ProtocolLimits.MAX_PLAYER_STATE_BYTES))
            .add(spec(PLAYER_STATE_UPDATE_ID, MessageDirection.SERVER_TO_CLIENT,
                    PlayerStateV0.PlayerStateUpdate.class, ProtocolLimits.MAX_PLAYER_STATE_BYTES))
            .add(spec(SELECT_MODEL_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    ControlV0.SelectModelRequest.class, ProtocolLimits.MAX_MODEL_SELECTION_BYTES))
            .add(spec(AUTHORIZED_MODELS_SNAPSHOT_ID, MessageDirection.SERVER_TO_CLIENT,
                    ControlV0.AuthorizedModelsSnapshot.class, ProtocolLimits.MAX_MODEL_SET_BYTES))
            .add(spec(STARRED_MODELS_SNAPSHOT_ID, MessageDirection.SERVER_TO_CLIENT,
                    ControlV0.StarredModelsSnapshot.class, ProtocolLimits.MAX_MODEL_SET_BYTES))
            .add(spec(UPDATE_STARRED_MODEL_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    ControlV0.UpdateStarredModelRequest.class, ProtocolLimits.MAX_STAR_UPDATE_BYTES))
            .add(spec(ENTITY_ANIMATION_ACTION_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    ControlV0.EntityAnimationActionRequest.class, ProtocolLimits.MAX_ENTITY_ACTION_BYTES))
            .add(spec(EXECUTE_MOLANG_EVENT_ID, MessageDirection.SERVER_TO_CLIENT,
                    ControlV0.ExecuteMolangEvent.class, ProtocolLimits.MAX_MOLANG_EVENT_BYTES))
            .add(spec(SUBMIT_ROULETTE_EXPRESSION_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    ControlV0.SubmitRouletteExpressionRequest.class, ProtocolLimits.MAX_ENTITY_ACTION_BYTES))
            .add(spec(EMIT_MOLANG_SYNC_ID, MessageDirection.CLIENT_TO_SERVER,
                    ControlV0.EmitMolangSync.class, ProtocolLimits.MAX_STAR_UPDATE_BYTES))
            .add(spec(MOLANG_SYNC_EVENT_ID, MessageDirection.SERVER_TO_CLIENT,
                    ControlV0.MolangSyncEvent.class, ProtocolLimits.MAX_MOLANG_SYNC_BYTES))
            .add(spec(SWING_HAND_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    ControlV0.SwingHandRequest.class, ProtocolLimits.MAX_SWING_HAND_BYTES))
            .add(spec(PROJECTILE_MODEL_STATE_ID, MessageDirection.SERVER_TO_CLIENT,
                    MinecraftStateV0.ProjectileModelState.class, ProtocolLimits.MAX_MINECRAFT_STATE_BYTES))
            .add(spec(VEHICLE_MODEL_STATE_ID, MessageDirection.SERVER_TO_CLIENT,
                    MinecraftStateV0.VehicleModelState.class, ProtocolLimits.MAX_MINECRAFT_STATE_BYTES))
            .add(spec(ASSET_FRAGMENT_ID, MessageDirection.SERVER_TO_CLIENT,
                    AssetTransferV0.AssetFragment.class, ProtocolLimits.MAX_FRAGMENT_MESSAGE_BYTES))
            .add(spec(MODEL_ASSET_BATCH_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    AssetTransferV0.ModelAssetBatchRequest.class, ProtocolLimits.MAX_ASSET_REQUEST_BYTES))
            .add(spec(MODEL_ASSET_BATCH_FAILURE_ID, MessageDirection.SERVER_TO_CLIENT,
                    AssetTransferV0.ModelAssetBatchFailure.class, ProtocolLimits.MAX_ASSET_CONTROL_BYTES))
            .add(spec(CATALOG_RESYNC_REQUEST_ID, MessageDirection.CLIENT_TO_SERVER,
                    AssetTransferV0.CatalogResyncRequest.class, ProtocolLimits.MAX_ASSET_CONTROL_BYTES))
            .add(spec(MODEL_ASSET_BATCH_CANCEL_ID, MessageDirection.CLIENT_TO_SERVER,
                    AssetTransferV0.ModelAssetBatchCancel.class, ProtocolLimits.MAX_ASSET_CANCEL_BYTES))
            .add(spec(ASSET_TRANSFER_RELEASE_ID, MessageDirection.CLIENT_TO_SERVER,
                    AssetTransferV0.AssetTransferRelease.class, ProtocolLimits.MAX_ASSET_RELEASE_BYTES))
            .build();

    private ProtocolMessages() {
    }

    private static <T> ProtocolMessageSpec<T> spec(int id, MessageDirection direction,
                                                    Class<T> type, int maxBytes) {
        return new ProtocolMessageSpec<>(id, direction, type, maxBytes);
    }
}
