package com.elfmcys.yesstevemodel.config;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

// Native Access
public class ServerConfig {
    // Native Access: 重载开始时读取
    public static ForgeConfigSpec.IntValue THREAD_COUNT;
    // Native Access: 同步开始时读取
    public static ForgeConfigSpec.IntValue BANDWIDTH_LIMIT;
    // Native Access: 同步开始时读取
    public static ForgeConfigSpec.IntValue CLIENT_SYNC_TIMEOUT;
    public static ForgeConfigSpec.BooleanValue CAN_SWITCH_MODEL;

    // 禁止在玩家客户端 GUI 界面显示的模型 ID
    public static ForgeConfigSpec.ConfigValue<List<String>> CLIENT_NOT_DISPLAY_MODELS;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ServerConfig.init(builder);
        return builder.build();
    }

    private static void init(ForgeConfigSpec.Builder builder) {
        builder.push("server_scheduler");

        builder.comment("Concurrent level for processing models. Value 0 means AUTO.");
        THREAD_COUNT = builder.defineInRange("ThreadCount", 0, 0, Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

        builder.comment("Bandwidth limitation during distributing models to players.(In Mbps)");
        BANDWIDTH_LIMIT = builder.defineInRange("BandwidthLimit", 5, 1, 999);

        builder.comment("Timeout for players to respond to synchronization. Value not greater than 10 means AUTO.(In seconds)");
        CLIENT_SYNC_TIMEOUT = builder.defineInRange("PlayerSyncTimeout", 0, 0, 120);

        builder.comment("Whether or not players are allowed to switch models");
        CAN_SWITCH_MODEL = builder.define("CanSwitchModel", true);

        builder.comment("Models that are not displayed on the client model selection screen");
        builder.comment("Example: [\"default\", \"default_boy\", \"alex\", \"steve\", \"qingluka\", \"wine_fox\", \"wine_fox_jk\"]");
        CLIENT_NOT_DISPLAY_MODELS = builder.define("ClientNotDisplayModels", Lists.newArrayList());

        builder.pop();
    }
}
