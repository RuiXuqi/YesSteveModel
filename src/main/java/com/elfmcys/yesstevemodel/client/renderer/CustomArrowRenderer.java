package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.instance.CustomArrowInstance;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoProjectilesRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

import javax.annotation.Nullable;

public class CustomArrowRenderer extends GeoProjectilesRenderer<CustomArrowInstance> {
    public CustomArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Nullable
    @Override
    protected CustomArrowInstance getGeoInstance(AbstractArrow entity) {
        if (entity instanceof IArrowExtraInfo) {
            return (CustomArrowInstance) ((IArrowExtraInfo) entity).getGeoInstance();
        } else {
            return null;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractArrow entity) {
        if (entity instanceof IArrowExtraInfo extraInfo && extraInfo.getGeoInstance() instanceof GeoInstance<?,?> instance) {
            return instance.getTextureLocation();
        } else {
            return MissingTextureAtlasSprite.getLocation();
        }
    }
}
