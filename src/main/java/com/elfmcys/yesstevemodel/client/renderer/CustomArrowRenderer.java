package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.instance.CustomArrowInstance;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoProjectilesRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
}
