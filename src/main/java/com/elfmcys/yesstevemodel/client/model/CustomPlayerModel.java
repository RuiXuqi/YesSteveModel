package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("all")
public class CustomPlayerModel extends AnimatedGeoModel<CustomPlayerEntity> {
    public static final ResourceLocation DEFAULT_MODEL = ModelIdUtil.DEFAULT_MODEL_ID;
    public static final ResourceLocation DEFAULT_MAIN_MODEL = ModelIdUtil.DEFAULT_MAIN_MODEL_ID;
    public static final ResourceLocation DEFAULT_MAIN_ANIMATION = ModelIdUtil.DEFAULT_MAIN_MODEL_ID;
    public static final ResourceLocation DEFAULT_TEXTURE = ModelIdUtil.DEFAULT_TEXTURE_ID;
    public static float FIRST_PERSON_HEAD_POS;

    @Override
    public ResourceLocation getModelLocation(CustomPlayerEntity customPlayer) {
        return customPlayer.getMainModel();
    }

    @Override
    public ResourceLocation getTextureLocation(CustomPlayerEntity customPlayer) {
        return customPlayer.getTexture();
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CustomPlayerEntity customPlayer) {
        return customPlayer.getAnimation();
    }

    @Override
    public boolean setCustomAnimations(CustomPlayerEntity customPlayer, AnimationContext<?> ctx, @Nonnull AnimationEvent<CustomPlayerEntity> animationEvent) {
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
