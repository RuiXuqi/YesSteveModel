package com.elfmcys.yesstevemodel.client.instance;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.model.CustomArrowModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class CustomArrowInstance extends GeoInstance<CustomArrowEntity, CustomArrowModel> {
    public CustomArrowInstance(AbstractArrow arrow) {
        super(new CustomArrowModel(), new CustomArrowEntity(arrow), true);
        animatableModel.getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        setInitialized();
    }

    @Override
    public DebugSource getDebugSource() {
        if(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public boolean isModelPresent() {
        return ClientModelManager.getModel(animatable.getModelId()).map(model -> model.projectileModels().containsKey(ProjectileType.ARROW)).orElse(false);
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
