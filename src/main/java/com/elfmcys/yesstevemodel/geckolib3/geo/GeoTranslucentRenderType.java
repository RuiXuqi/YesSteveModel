package com.elfmcys.yesstevemodel.geckolib3.geo;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class GeoTranslucentRenderType extends RenderType {
    private static final Function<ResourceLocation, GeoTranslucentRenderType> CUSTOM_TRANSLUCENT = Util.memoize(GeoTranslucentRenderType::new);

    private GeoTranslucentRenderType(ResourceLocation textureLocation) {
        this(RenderType.entityTranslucent(textureLocation));
    }

    private GeoTranslucentRenderType(RenderType inner) {
        super("entity_translucent_geo", inner.format(), inner.mode(), inner.bufferSize(), inner.affectsCrumbling(), false, inner::setupRenderState, inner::clearRenderState);
    }

    public static GeoTranslucentRenderType create(ResourceLocation textureLocation) {
        return CUSTOM_TRANSLUCENT.apply(textureLocation);
    }
}
