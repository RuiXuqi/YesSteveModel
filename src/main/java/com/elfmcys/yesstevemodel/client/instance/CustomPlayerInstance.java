package com.elfmcys.yesstevemodel.client.instance;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public class CustomPlayerInstance extends GeoInstance<CustomPlayerEntity, CustomPlayerModel> {
    protected ResourceLocation modelId = CustomPlayerModel.DEFAULT_MAIN_MODEL;
    protected String textureName = ModelIdUtil.getSubNameFromId(CustomPlayerModel.DEFAULT_TEXTURE);
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
        ResourceLocation modelId = animatable.getMainModelUnsafe();
        return modelId != null && GeckoLibCache.getInstance().getGeoModels().get(modelId) != null;
    }

    @Override
    public String getTextureName() {
        return textureName;
    }

    @Override
    public float getWidthScale() {
        return animatable.getWidthScale();
    }

    @Override
    public float getHeightScale() {
        return animatable.getHeightScale();
    }

    public void setModelAndTexture(ResourceLocation modelId, ResourceLocation textureLocation) {
        setInitialized();
        setModel(modelId);
        setTexture(textureLocation);
    }

    public void setTexture(ResourceLocation textureLocation) {
        animatable.setTexture(textureLocation);
        textureName = ModelIdUtil.getSubNameFromId(textureLocation);
    }

    public void setModel(ResourceLocation modelId) {
        this.modelId = modelId;
        this.animatableModel.getModelLocation(animatable);
        animatable.setMainModel(ModelIdUtil.getMainId(modelId));
    }

    public void playAnimation(String animationName) {
        this.animationName = animationName;
        this.isPlayingAnimation = true;
        this.isAnimationDirty = true;
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

    public ResourceLocation getModelId() {
        return this.modelId;
    }
}
