package com.elfmcys.ysm.geckolib3.geo.render.built;

import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import mixel.asset.model.data.GeoModelOuterClass;
import org.joml.Vector3f;

public class GeoBone {
    private final String name;
    private final int pooledName;
    private final GeoLocator locatorType;
    private final Vector3f rotation;
    private final Vector3f pivot;
    private final boolean debug;

    public GeoBone(GeoModelOuterClass.Bone bone, GeoLocator locator) {
        if (!bone.hasName() || bone.getName().isEmpty() ||
                !bone.hasRotate() || bone.getRotate().length() != 3 ||
                !bone.hasPivot() || bone.getPivot().length() != 3) {
            throw new IllegalArgumentException("Invalid bone metadata");
        }
        this.name = bone.getName();
        this.pooledName = StringPool.computeIfAbsent(name);
        this.locatorType = locator;
        this.rotation = new Vector3f(bone.getRotate().get(0),
                bone.getRotate().get(1), bone.getRotate().get(2));
        this.pivot = new Vector3f(bone.getPivot().get(0),
                bone.getPivot().get(1), bone.getPivot().get(2));
        this.debug = bone.hasDebug() && bone.getDebug();
    }

    public String name() {
        return name;
    }

    public int pooledName() {
        return pooledName;
    }

    public GeoLocator locatorType() {
        return locatorType;
    }

    public Vector3f rotation() {
        return rotation;
    }

    public Vector3f pivot() {
        return pivot;
    }

    public boolean debug() {
        return debug;
    }
}
