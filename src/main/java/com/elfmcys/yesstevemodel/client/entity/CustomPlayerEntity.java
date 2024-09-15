package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.AnimationManager;
import com.elfmcys.yesstevemodel.client.animation.controller.NewAnimationManager;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatableModel;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationFactory;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.util.GeckoLibUtil;
import com.elfmcys.yesstevemodel.molang.runtime.HashMapStruct;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public class CustomPlayerEntity implements IAnimatable<AbstractClientPlayer> {
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this, false);
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private String texture = ModelIdUtil.DEFAULT_TEXTURE_NAME;
    private Struct remoteStruct;
    private String previewAnimation = "";
    private String hoverAnimation = "";
    private String focusAnimation = "";
    private AbstractClientPlayer player;

    private int instanceIdOverride;
    private Object2FloatOpenHashMap<String> initialVariables;
    private final boolean localPlayer;

    public CustomPlayerEntity(AbstractClientPlayer player, boolean localPlayer) {
        this.player = player;
        this.localPlayer = localPlayer;
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

    /**
     * 越往后优先级越高
     */
    @Override
    @SuppressWarnings("all")
    public void registerControllers(AnimationData data, IAnimatableModel<?> model) {
        ClientModelManager.getModel(this.getModelId()).ifPresent(clientModel -> {
            var controllers = clientModel.animationControllers();

            // 如果动画控制器为空，那么使用旧版本动画
            if (controllers.isEmpty()) {
                registerOldControllers(data, model);
                return;
            }

            // 否则，全部使用新版动画控制器
            controllers.forEach((id, controller) ->
                    data.addAnimationController(new AnimationController(this, model, id, 2,
                            (event, evaluator) -> NewAnimationManager.predicate(event, evaluator, controller))));
        });
    }

    @SuppressWarnings("all")
    private void registerOldControllers(AnimationData data, IAnimatableModel<?> model) {
        AnimationManager manager = AnimationManager.getInstance();
        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("pre_parallel_%d_controller", i);
            String animationName = String.format("pre_parallel%d", i);
            data.addAnimationController(new AnimationController(this, model, controllerName, 0, (event, evaluator) -> manager.predicateParallel(event, animationName)));
        }
        data.addAnimationController(new AnimationController(this, model, MAIN_CONTROLLER, 2, (event, evaluator) -> manager.predicateMain(event)));
        data.addAnimationController(new AnimationController(this, model, HOLD_OFFHAND_CONTROLLER, 0, (event, evaluator) -> manager.predicateOffhandHold(event)));
        data.addAnimationController(new AnimationController(this, model, HOLD_MAINHAND_CONTROLLER, 0, (event, evaluator) -> manager.predicateMainhandHold(event)));
        data.addAnimationController(new AnimationController(this, model, FIRE_MAINHAND_CONTROLLER, 0, (event, evaluator) -> manager.predicateMainhandFire(event)));
        data.addAnimationController(new AnimationController(this, model, SWING_CONTROLLER, 0, (event, evaluator) -> manager.predicateSwing(event)));
        data.addAnimationController(new AnimationController(this, model, USE_CONTROLLER, 2, (event, evaluator) -> manager.predicateUse(event)));
        if (CarryOnCompat.isCarryOnLoaded()) {
            data.addAnimationController(new AnimationController(this, model, CARRY_ON_CONTROLLER, 2, (event, evaluator) -> CarryOnCompat.predicateCarryOn(event)));
        }
        data.addAnimationController(new AnimationController(this, model, PASSENGER_CONTROLLER, 2, (event, evaluator) -> manager.predicatePassengerAnimation(event)));
        data.addAnimationController(new AnimationController(this, model, CAP_CONTROLLER, 2, (event, evaluator) -> manager.predicateCap(event)));

        data.addAnimationController(new AnimationController(this, model, HOVER_CONTROLLER, 0, (event, evaluator) -> manager.predicateHover(event)));
        data.addAnimationController(new AnimationController(this, model, FOCUS_CONTROLLER, 0, (event, evaluator) -> manager.predicateFocus(event)));

        for (int i = 0; i < 8; i++) {
            String controllerName = String.format("parallel_%d_controller", i);
            String animationName = String.format("parallel%d", i);
            data.addAnimationController(new AnimationController(this, model, controllerName, 0, (event, evaluator) -> manager.predicateParallel(event, animationName)));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = String.format("%s_controller", slot.getName());
                data.addAnimationController(new AnimationController(this, model, controllerName, 0, (event, evaluator) -> manager.predicateArmor(event, slot)));
            }
        }
    }

    public String getModelId() {
        if (ClientModelManager.getModel(modelId).isPresent()) {
            return modelId;
        }
        return ModelIdUtil.DEFAULT_MODEL_ID;
    }

    public String getModelIdUnsafe() {
        return this.modelId;
    }

    public void setModel(String modelId) {
        this.modelId = modelId;
    }

    public float getHeightScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().heightScale()).orElse(0.7f);
    }

    public float getWidthScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().widthScale()).orElse(0.7f);
    }

    @Override
    public AbstractClientPlayer getEntity() {
        return player;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public String getPreviewAnimation() {
        return previewAnimation;
    }

    public void setPreviewAnimation(String previewAnimation) {
        this.previewAnimation = previewAnimation;
    }

    public boolean hasPreviewAnimation() {
        return StringUtils.isNoneBlank(this.previewAnimation);
    }

    public boolean hasPreviewAnimation(String previewAnimation) {
        return hasPreviewAnimation() && previewAnimation.equals(this.previewAnimation);
    }

    public String getHoverAnimation() {
        return hoverAnimation;
    }

    public String getFocusAnimation() {
        return focusAnimation;
    }

    public void setHoverAnimation(String hoverAnimation) {
        this.hoverAnimation = hoverAnimation;
    }

    public void setFocusAnimation(String focusAnimation) {
        this.focusAnimation = focusAnimation;
    }

    @Nullable
    public Struct getRemoteStruct() {
        if (initialVariables != null) {
            if (remoteStruct instanceof RoamingStruct roamingStruct) {
                roamingStruct.reset(instanceIdOverride, initialVariables);
            } else {
                remoteStruct = new HashMapStruct();
                for (var entry : initialVariables.object2FloatEntrySet()) {
                    this.remoteStruct.putProperty(StringPool.computeIfAbsent(entry.getKey()), entry.getFloatValue());
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

    public boolean isLocalPlayer() {
        return localPlayer;
    }
}
