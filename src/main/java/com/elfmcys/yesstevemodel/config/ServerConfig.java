package com.elfmcys.yesstevemodel.config;

import net.minecraftforge.common.ForgeConfigSpec;

// Native Access
public class ServerConfig {
    // Native Access: 重载开始时读取
    public static ForgeConfigSpec.IntValue THREAD_COUNT;
    // Native Access: 同步开始时读取
    public static ForgeConfigSpec.IntValue BANDWIDTH_LIMIT;
    // Native Access: 同步开始时读取
    public static ForgeConfigSpec.IntValue CLIENT_SYNC_TIMEOUT;
    public static ForgeConfigSpec.BooleanValue CAN_SWITCH_MODEL;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.comment("Only available on dedicated servers.");
        builder.push("server_scheduler");

        builder.comment("Concurrent level for processing models. Value 0 means AUTO.");
        THREAD_COUNT = builder.defineInRange("ThreadCount", 0, 0, Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

        builder.comment("Bandwidth limitation during distributing models to players.(In Mbps)");
        BANDWIDTH_LIMIT = builder.defineInRange("BandwidthLimit", 5, 1, 999);

        builder.comment("Timeout for players to respond to synchronization.(In seconds)");
        CLIENT_SYNC_TIMEOUT = builder.defineInRange("PlayerSyncTimeout", 10, 5, 60);

        builder.comment("Whether or not players are allowed to switch models");
        CAN_SWITCH_MODEL = builder.define("CanSwitchModel", true);

        builder.pop();
    }
}
