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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("all")
public class CustomPlayerModel extends AnimatedGeoModel<CustomPlayerEntity> {
    public static float FIRST_PERSON_HEAD_POS;

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

    @Override
    public boolean forceUpdate() {
        return RenderUtil.isRenderingEntitiesInInventory();
    }

    @Deprecated
    private void codeAnimation(AnimationEvent<CustomPlayerEntity> animationEvent, EntityModelData data, Player player, boolean update) {
        // 2023/6/21 这一块设计应该改成 molang 的，而且这个寻找效率低下
        // 2023/11/07 改善了寻找效率
        IBone head = getBone("Head");
        boolean isLocalPlayer = player instanceof LocalPlayer;
        if (update) {
            if (isLocalPlayer) {
                FIRST_PERSON_HEAD_POS = 24;
            }
            if (head != null) {
                head.setRotationX(head.getRotationX() + (float) Math.toRadians(data.headPitch));
                head.setRotationY(head.getRotationY() + (float) Math.toRadians(data.netHeadYaw));
                if (isLocalPlayer) {
                    FIRST_PERSON_HEAD_POS = head.getPivotY() * animationEvent.getAnimatable().getHeightScale();
                }
            }
        }
        GeoModelState model = getCurrentModel();
        if (isLocalPlayer && model != null) {
            if (model.firstPersonViewLocator() != null) {
                float heightScale = animationEvent.getAnimatable().getHeightScale();
                IBone locator = model.firstPersonViewLocator();
                FIRST_PERSON_HEAD_POS = locator.getPivotY() * heightScale;
            }
            if (FirstPersonCompat.isInstalled() && model.firstPersonHead() != null) {
                FirstPersonCompat.hideHead(model.firstPersonHead());
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
