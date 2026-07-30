package com.elfmcys.ysm.format.schema.baked.asset;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class BakedAssetConstant {
    public static String SCHEMA_ID = "ysm/baked-asset";
    public static String MANIFEST_CHUNK_NAME = "manifest";
    public static String ANIM_CHUNK_PREFIX = "animation/";
    public static DefaultArtifactVersion CURRENT_VERSION = new DefaultArtifactVersion("0.1.0-unstable");
    public static short PROP_VERSION = 0;
}
