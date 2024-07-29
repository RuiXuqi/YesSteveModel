package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("all")
public class CustomPlayerModel extends AnimatedGeoModel<CustomPlayerEntity> {
    private volatile boolean renderedWithTempChanges = false;

    @Override
    public GeoModel getModel(String location) {
        return ClientModelManager.getModel(location).map(model -> model.mainModel()).orElse(ClientModelManager.getDefaultModel().mainModel());
    }

    @Nullable
    @Override
    public Animation getAnimation(String name, CustomPlayerEntity animatable) {
        return ClientModelManager.getPlayerAnimation(animatable.getModelId(), name)
                .orElse(null);
    }

    @Override
    public String getModelLocation(CustomPlayerEntity customPlayer) {
        return customPlayer.getModelId();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(CustomPlayerEntity customPlayer) {
        return ClientModelManager.getPlayerTextureLocation(customPlayer.getModelId(), customPlayer.getTexture()).orElse(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    public boolean setCustomAnimations(CustomPlayerEntity customPlayer, AnimationContext<?> ctx, @NotNull AnimationEvent<CustomPlayerEntity> animationEvent) {
        List extraData = animationEvent.getExtraData();
        if (!Minecraft.getInstance().isPaused() && extraData.size() == 1 && extraData.get(0) instanceof EntityModelData
                && customPlayer.getEntity() != null) {
            Player player = customPlayer.getEntity();

            EntityModelData data = (EntityModelData) extraData.get(0);
            boolean update = super.setCustomAnimations(customPlayer, ctx, animationEvent);
            this.codeAnimation(animationEvent, data, player, update);
            return update;
        } else {
            return super.setCustomAnimations(customPlayer, ctx, animationEvent);
        }
    }

    /**
     * 注意非幂等
     */
    @Override
    public boolean forceUpdate() {
        if (FirstPersonCompat.isRenderingPlayer() || RenderUtil.isRenderingEntitiesInInventory()) {
            renderedWithTempChanges = true;
            return true;
        }
        if (renderedWithTempChanges) {
            renderedWithTempChanges = false;
            return true;
        }
        return false;
    }

    public void codeAnimationForShadowRendering() {
        GeoModelState model = getCurrentModel();
        if (model != null && model.firstPersonHead() != null) {
            model.firstPersonHead().setHidden(false);
        }
    }

    @Deprecated
    private void codeAnimation(AnimationEvent<CustomPlayerEntity> animationEvent, EntityModelData data, Player player, boolean update) {
        // 2023/6/21 这一块设计应该改成 molang 的，而且这个寻找效率低下
        // 2023/11/07 改善了寻找效率
        IBone head = getBone("Head");
        GeoModelState model = getCurrentModel();

        // 更新头部旋转
        if (update && head != null) {
            head.setRotationX(head.getRotationX() + (float) Math.toRadians(data.headPitch));
            head.setRotationY(head.getRotationY() + (float) Math.toRadians(data.netHeadYaw));
        }

        // 更新第一人称相机偏移与头部隐藏
        if (animationEvent.getAnimatable().isLocalPlayer() && FirstPersonCompat.isInstalled()) {
            if (model.firstPersonHead() != null) {
                model.firstPersonHead().setHidden(FirstPersonCompat.shouldHideHead());
            }

            if (model != null && model.firstPersonViewLocator() != null) {
                FirstPersonCompat.setHeadPos(model.firstPersonViewLocator().getPivotY() * animationEvent.getAnimatable().getHeightScale());
            } else if (update) {
                FirstPersonCompat.setHeadPos(head == null ? 24f : (head.getPivotY() * animationEvent.getAnimatable().getHeightScale()));
            }
        }
    }

    @Override
    @Nullable
    public IBone getBone(String boneName) {
        return getAnimationProcessor().getBone(boneName);
    }

    @Override
    @Nullable
    public Struct getRemoteStruct(CustomPlayerEntity object) {
        return object.getRemoteStruct();
    }
}
