package com.elfmcys.ysm.geckolib3.geo.render.built;

import java.util.Objects;

// 不要用枚举
public class GeoLocator {
    private final GeoLocatorType type;
    private final String name;
    private final byte seq;

    GeoLocator(GeoLocatorType type, String name, byte seg) {
        this.type = type;
        this.name = name;
        this.seq = seg;
    }

    public String name() {
        return name;
    }

    public byte seq() {
        return seq;
    }

    public GeoLocatorType type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, seq);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof GeoLocator other) {
            return type == other.type && seq == other.seq;
        }
        return false;
    }
}
