package com.elfmcys.ysm.model.source;

public final class ModelSources {
    public static final SourceId LOCAL_CUSTOM = new SourceId("local.custom");
    public static final SourceId LOCAL_AUTH = new SourceId("local.auth");
    public static final SourceId BUILTIN = new SourceId("builtin");
    public static final SourceId GAME_SERVER = new SourceId("game-server");

    public static final int LOCAL_CUSTOM_PRIORITY = 300;
    public static final int LOCAL_AUTH_PRIORITY = 250;
    public static final int BUILTIN_PRIORITY = 200;
    public static final int GAME_SERVER_PRIORITY = 100;

    private ModelSources() {
    }

    public static int defaultPriority(SourceId sourceId) {
        if (sourceId.equals(LOCAL_CUSTOM)) return LOCAL_CUSTOM_PRIORITY;
        if (sourceId.equals(LOCAL_AUTH)) return LOCAL_AUTH_PRIORITY;
        if (sourceId.equals(BUILTIN)) return BUILTIN_PRIORITY;
        if (sourceId.equals(GAME_SERVER)) return GAME_SERVER_PRIORITY;
        return 0;
    }
}
