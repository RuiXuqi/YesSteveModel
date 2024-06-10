package com.elfmcys.yesstevemodel.info.stats;

// Native Access
public class GeoModelStats {
    public final int bones;
    public final int cubes;
    public final int faces;

    // Native Access
    public GeoModelStats(int bones, int cubes, int faces) {
        this.bones = bones;
        this.cubes = cubes;
        this.faces = faces;
    }

    public int bones() {
        return bones;
    }

    public int cubes() {
        return cubes;
    }

    public int faces() {
        return faces;
    }
}
