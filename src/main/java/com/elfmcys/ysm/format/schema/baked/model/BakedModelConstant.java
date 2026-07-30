package com.elfmcys.ysm.format.schema.baked.model;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class BakedModelConstant {
    public static String SCHEMA_ID = "ysm/baked-model";
    public static String MANIFEST_CHUNK_NAME = "manifest";
    public static String MODEL_HASH_CHUNK_NAME = "model_hash";
    public static String MODEL_CHUNK_NAME = "model";
    public static DefaultArtifactVersion CURRENT_VERSION = new DefaultArtifactVersion("0.1.0-unstable");
    public static short PROP_VERSION = 0;
}
