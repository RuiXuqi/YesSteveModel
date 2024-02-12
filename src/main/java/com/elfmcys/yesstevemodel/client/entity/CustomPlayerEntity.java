package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationManager;
import com.elfmcys.yesstevemodel.client.data.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationFactory;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.geckolib3.util.GeckoLibUtil;
import com.elfmcys.yesstevemodel.molang.runtime.HashMapStruct;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public class CustomPlayerEntity implements IAnimatable<AbstractClientPlayer> {
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this, false);
    private ResourceLocation mainModel = CustomPlayerModel.DEFAULT_MAIN_MODEL;
    private ResourceLocation modelId = CustomPlayerModel.DEFAULT_MODEL;
    private ResourceLocation texture = CustomPlayerModel.DEFAULT_TEXTURE;
    private Struct remoteStruct;
    private String previewAnimation = "";
    private AbstractClientPlayer player;

    private int instanceIdOverride;
    private Object2FloatOpenHashMap<String> initialVariables;

    public CustomPlayerEntity(AbstractClientPlayer player, boolean localPlayer) {
        this.player = player;
        if (localPlayer) {
            remoteStruct = new RoamingStruct();
        } else {
            remoteStruct = new HashMapStruct();
        }
    }

    // 只允许重置为 LocalPlayer，用于 GUI 渲染
    public void setPlayer(LocalPlayer player) {
        this.player = player;
    }

    @NotNull
    private static PlayState playLoopAnimation(AnimationEvent<CustomPlayerEntity> event, String animationName) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName, ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }

    /**
     * 越往后优先级越高
     */
    @Override
    @SuppressWarnings("all")
    public void registerControllers(AnimationData data, IAnimatableModel<?> model) {
        AnimationManager manager = AnimationManager.getInstance();
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("pre_parallel_%d_controller", i);
            String animationName = String.format("pre_parallel%d", i);
            data.addAnimationController(new AnimationController(this, model, controllerName, 0, e -> manager.predicateParallel(e, animationName)));
        }
        data.addAnimationController(new AnimationController(this, model, MAIN_CONTROLLER, 2, manager::predicateMain));
        data.addAnimationController(new AnimationController(this, model, HOLD_OFFHAND_CONTROLLER, 0, manager::predicateOffhandHold));
        data.addAnimationController(new AnimationController(this, model, HOLD_MAINHAND_CONTROLLER, 0, manager::predicateMainhandHold));
        data.addAnimationController(new AnimationController(this, model, SWING_CONTROLLER, 0, manager::predicateSwing));
        data.addAnimationController(new AnimationController(this, model, USE_CONTROLLER, 2, manager::predicateUse));
        if (CarryOnCompat.isCarryOnLoaded()) {
            data.addAnimationController(new AnimationController(this, model, CARRY_ON_CONTROLLER, 2, CarryOnCompat::predicateCarryOn));
        }
        data.addAnimationController(new AnimationController(this, model, CAP_CONTROLLER, 2, manager::predicateCap));
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("parallel_%d_controller", i);
            String animationName = String.format("parallel%d", i);
            data.addAnimationController(new AnimationController(this, model, controllerName, 0, e -> manager.predicateParallel(e, animationName)));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = String.format("%s_controller", slot.getName());
                data.addAnimationController(new AnimationController(this, model, controllerName, 0, e -> manager.predicateArmor(e, slot)));
            }
        }
    }

    public ResourceLocation getMainModel() {
        if (GeckoLibCache.getInstance().getGeoModels().containsKey(this.mainModel)) {
            return mainModel;
        }
        return CustomPlayerModel.DEFAULT_MAIN_MODEL;
    }

    public ResourceLocation getMainModelUnsafe() {
        return this.mainModel;
    }

    public void setMainModel(ResourceLocation mainModel) {
        this.mainModel = mainModel;
        this.modelId = ModelIdUtil.getModelIdFromMainId(mainModel);
    }

    public ResourceLocation getAnimation() {
        if (GeckoLibCache.getInstance().getAnimations().containsKey(this.mainModel)) {
            return mainModel;
        }
        return CustomPlayerModel.DEFAULT_MAIN_ANIMATION;
    }

    public float getHeightScale() {
        ClientModelInfo modelInfo = ClientModelManager.getModelInfo().get(modelId);
        return modelInfo == null ? 0.7f : (float) modelInfo.heightScale();
    }

    public float getWidthScale() {
        ClientModelInfo modelInfo = ClientModelManager.getModelInfo().get(modelId);
        return modelInfo == null ? 0.7f : (float) modelInfo.widthScale();
    }

    @Override
    public AbstractClientPlayer getEntity() {
        return player;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public String getPreviewAnimation() {
        return previewAnimation;
    }

    public void setPreviewAnimation(String previewAnimation) {
        this.previewAnimation = previewAnimation;
    }

    public void clearPreviewAnimation() {
        this.previewAnimation = "";
    }

    public boolean hasPreviewAnimation() {
        return StringUtils.isNoneBlank(this.previewAnimation);
    }

    public boolean hasPreviewAnimation(String previewAnimation) {
        return hasPreviewAnimation() && previewAnimation.equals(this.previewAnimation);
    }

    @Nullable
    public Struct getRemoteStruct() {
        if (initialVariables != null) {
            if (remoteStruct instanceof RoamingStruct roamingStruct) {
                roamingStruct.reset(instanceIdOverride, initialVariables);
            } else {
                remoteStruct = new HashMapStruct();
                for (var entry : initialVariables.object2FloatEntrySet()) {
                    this.remoteStruct.putProperty(StringPool.getName(entry.getKey()), entry.getFloatValue());
                }
            }
            initialVariables = null;
        }
        return remoteStruct;
    }

    public void setRemoteVariables(int instanceId, Object2FloatOpenHashMap<String> initialVariables) {
        this.instanceIdOverride = instanceId;
        this.initialVariables = initialVariables;
    }
}
