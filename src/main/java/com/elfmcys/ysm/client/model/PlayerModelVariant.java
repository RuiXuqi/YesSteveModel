package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Objects;

public final class PlayerModelVariant {
    private final GeoModel mainModel;
    private final GeoModel armModel;
    private final AbstractTexture texture;

    public PlayerModelVariant(GeoModel mainModel, GeoModel armModel, AbstractTexture texture) {
        this.mainModel = Objects.requireNonNull(mainModel, "mainModel");
        this.armModel = Objects.requireNonNull(armModel, "armModel");
        this.texture = Objects.requireNonNull(texture, "texture");
    }

    public GeoModel mainModel() {
        return mainModel;
    }

    public GeoModel armModel() {
        return armModel;
    }

    public AbstractTexture texture() {
        return texture;
    }
}
