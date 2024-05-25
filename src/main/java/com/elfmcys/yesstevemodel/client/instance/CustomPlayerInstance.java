package com.elfmcys.yesstevemodel.client.instance;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public class CustomPlayerInstance extends GeoInstance<CustomPlayerEntity, CustomPlayerModel> {
    protected boolean isPlayingAnimation = false;
    protected String animationName = "idle";
    protected boolean isAnimationDirty = false;

    public CustomPlayerInstance(AbstractClientPlayer player, boolean asyncUpdate, boolean localPlayer) {
        super(new CustomPlayerModel(), new CustomPlayerEntity(player, localPlayer), asyncUpdate);
        animatableModel.getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        if (player instanceof LocalPlayer) {
            setInitialized();
        }
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
        String modelId = animatable.getModelIdUnsafe();
        return modelId != null && ClientModelManager.getModels().containsKey(modelId);
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return animatableModel.getTextureLocation(animatable);
    }

    public String getTextureName() {
        return animatable.getTexture();
    }

    @Override
    public float getWidthScale() {
        return animatable.getWidthScale();
    }

    @Override
    public float getHeightScale() {
        return animatable.getHeightScale();
    }

    public void setModelAndTexture(String modelId, String textureName) {
        setInitialized();
        setModel(modelId);
        setTexture(textureName);
    }

    public void setTexture(String textureLocation) {
        animatable.setTexture(textureLocation);
    }

    public void setModel(String modelId) {
        animatable.setModel(modelId);
    }

    public void playAnimation(String animationName) {
        if (ClientModelManager.getPlayerAnimation(getModelId(), animationName).isPresent()) {
            this.animationName = animationName;
            this.isPlayingAnimation = true;
            this.isAnimationDirty = true;
        } else {
            this.isPlayingAnimation = false;
        }
    }

    public boolean isAnimationDirty() {
        return isAnimationDirty;
    }

    public void clearAnimationDirty() {
        this.isAnimationDirty = false;
    }

    public boolean isPlayingAnimation() {
        return isPlayingAnimation;
    }

    public String getAnimationName() {
        return this.animationName;
    }

    public void stopAnimation() {
        this.isPlayingAnimation = false;
    }

    public String getModelId() {
        return this.animatable.getModelId();
    }
}
