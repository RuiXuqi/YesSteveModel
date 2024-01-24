package com.elfmcys.yesstevemodel.client.instance;

import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.model.CustomArrowModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class CustomArrowInstance extends GeoInstance<CustomArrowEntity, CustomArrowModel> {
    public CustomArrowInstance(AbstractArrow arrow, String modelName) {
        super(new CustomArrowModel(), new CustomArrowEntity(arrow, modelName), true);
        animatableModel.getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        setInitialized();
    }

    @Override
    public DebugSource getDebugSource() {
        if(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return YSMDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public boolean isModelPresent() {
        return GeckoLibCache.getInstance().getGeoModels().get(animatable.getMainModel()) != null;
    }

    @Override
    public String getTextureName() {
        return ModelIdUtil.ARROW_TEXTURE_NAME;
    }

    @Override
    public float getWidthScale() {
        return 0.7F;
    }

    @Override
    public float getHeightScale() {
        return 0.7F;
    }
}
