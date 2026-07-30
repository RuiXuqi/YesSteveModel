package com.elfmcys.ysm.network.protocol;

public final class ProtocolLimits {
    public static final int MAX_HELLO_BYTES = 64 * 1024;
    public static final int MAX_MESSAGE_BYTES = 1024 * 1024;
    public static final int MAX_PLAYER_STATE_BYTES = 128 * 1024;
    public static final int MAX_MODEL_SELECTION_BYTES = 4 * 1024;
    public static final int MAX_MODEL_SET_BYTES = 768 * 1024;
    public static final int MAX_STAR_UPDATE_BYTES = 1024;
    public static final int MAX_ENTITY_ACTION_BYTES = 8 * 1024;
    public static final int MAX_MOLANG_EVENT_BYTES = 64 * 1024;
    public static final int MAX_MOLANG_SYNC_BYTES = 4 * 1024;
    public static final int MAX_SWING_HAND_BYTES = 256;
    public static final int MAX_MINECRAFT_STATE_BYTES = 32 * 1024;
    public static final int MAX_FRAGMENT_BYTES = 30 * 1024;
    public static final int MIN_FRAGMENT_DATA_BYTES = 1024;
    public static final int MAX_FRAGMENT_MESSAGE_BYTES = MAX_FRAGMENT_BYTES + 4 * 1024;
    public static final int MAX_ASSET_REQUEST_BYTES = 512 * 1024;
    public static final int MAX_ASSET_RESOURCES = 64;
    public static final int MAX_ASSET_CONTROL_BYTES = 1024;
    public static final int MAX_ASSET_CANCEL_BYTES = 256;
    public static final int MAX_ASSET_RELEASE_BYTES = 256;
    public static final int FRAGMENT_DATA_BYTES = 30 * 1024;
    public static final int MAX_IN_FLIGHT_TRANSFERS = 8;
    public static final int ASSET_TRANSFER_RELEASE_TIMEOUT_SECONDS = 30;
    public static final long MAX_ENCODED_ASSET_BYTES = 128L * 1024 * 1024;
    public static final long MAX_DECODED_ASSET_BYTES = 128L * 1024 * 1024;
    public static final long MAX_RESERVED_ASSET_BYTES = 256L * 1024 * 1024;

    private ProtocolLimits() {
    }
}
