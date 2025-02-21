package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.MaidMiscPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.YsmMaidMainPredicate;
import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.instance.CustomDebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationMolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

/**
 * 基于 CustomPlayerEntity 复制来的，基本上没做删除，试想尝试让女仆能调用轮盘动画之类的,所以就先预留着
 */
@OnlyIn(Dist.CLIENT)
public class CustomYsmMaidEntity extends AnimatableEntity<EntityMaid> implements IGeoEntity {
    private String modelId = ModelIdUtil.DEFAULT_MODEL_ID;
    private String textureName = ModelIdUtil.DEFAULT_TEXTURE_NAME;
    private final Vector2f headRot = new Vector2f();
    private volatile boolean renderedWithTempChanges = false;
    /**
     * 专为 tacz 枪械事件使用的，用来将枪械动画重置
     */
    private boolean tacGunAnimationNeedReload = false;
    private MaidModelInfo maidInfo = new MaidModelInfo();

    public CustomYsmMaidEntity(EntityMaid player, boolean asyncUpdate) {
        super(player, asyncUpdate);
        getDebugInfo().setEnabled(DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE);
        registerControllers();
    }

    /**
     * 越往后优先级越高
     */
    @SuppressWarnings("all")
    public void registerControllers() {
        for (int i = 0; i < 8; i++) {
            String controllerName = PRE_PARALLEL_CONTROLLER + i;
            String animationName = String.format("pre_parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0, new ParallelPredicate(animationName)));
        }

        addAnimationController(new HybridAnimationController(this, VEHICLE_CONTROLLER, 0.1f, new VehiclePredicate()));
        addAnimationController(new HybridAnimationController(this, MAIN_CONTROLLER, 0.1f, new YsmMaidMainPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_OFFHAND_CONTROLLER, 0, new OffhandPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_MAINHAND_CONTROLLER, 0, new MainhandPredicate()));
        TACZCompat.addTaczPredicate(this);
        addAnimationController(new HybridAnimationController(this, SWING_CONTROLLER, 0, new SwingPredicate()));
        addAnimationController(new HybridAnimationController(this, USE_CONTROLLER, 0.1f, new UsePredicate()));
        addAnimationController(new HybridAnimationController(this, MAID_MISC, 0.1f, new MaidMiscPredicate()));
        addAnimationController(new HybridAnimationController(this, PASSENGER_CONTROLLER, 0.1f, new PassengerPredicate()));

        for (int i = 0; i < 8; i++) {
            String controllerName = PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = ARMOR_CONTROLLER + slot.getName();
                addAnimationController(new HybridAnimationController(this, controllerName, 0, new ArmorPredicate(slot)));
            }
        }
    }

    @Override
    public String getModelId() {
        if (isModelPresent()) {
            return modelId;
        }
        return ModelIdUtil.DEFAULT_MODEL_ID;
    }

    @Override
    public float getHeightScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().heightScale()).orElse(0.7f);
    }

    @Override
    public float getWidthScale() {
        return ClientModelManager.getModel(modelId).map(model -> model.modelInfo().properties().widthScale()).orElse(0.7f);
    }

    @Override
    public GeoModel getModel() {
        return ClientModelManager.getModel(modelId).map(ClientModel::mainModel).orElse(ClientModelManager.getDefaultModel().mainModel());
    }

    @Nullable
    @Override
    public Animation getAnimation(String name) {
        return ClientModelManager.getPlayerAnimation(modelId, name).orElse(null);
    }

    @Nullable
    @Override
    public GeoAnimationController getAnimationControllerData(String animationControllerName) {
        return ClientModelManager.getModel(getModelId()).map(model -> model.animationControllers().get(animationControllerName)).orElse(null);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        if (isModelPresent()) {
            return ClientModelManager.getPlayerTextureLocation(modelId, textureName).orElse(MissingTextureAtlasSprite.getLocation());
        } else {
            return ModelIdUtil.DEFAULT_TEXTURE_LOCATION;
        }
    }

    @Override
    @SuppressWarnings("all")
    public boolean setCustomAnimations(AnimationMolangContext ctx, @NotNull AnimationEvent animationEvent) {
        List extraData = animationEvent.getExtraData();
        if (!Minecraft.getInstance().isPaused() && extraData.size() == 1 && extraData.get(0) instanceof EntityModelData
            && entity != null) {
            EntityModelData data = (EntityModelData) extraData.get(0);
            boolean update = super.setCustomAnimations(ctx, animationEvent);
            this.codeAnimation(animationEvent, data, entity, update);
            return update;
        } else {
            return super.setCustomAnimations(ctx, animationEvent);
        }
    }

    @Override
    public boolean shouldForceUpdate() {
        if (RenderUtil.isRenderingEntitiesInInventory()) {
            renderedWithTempChanges = true;
            return true;
        }
        if (renderedWithTempChanges) {
            renderedWithTempChanges = false;
            return true;
        }
        return false;
    }

    @Deprecated
    private void codeAnimation(AnimationEvent<CustomYsmMaidEntity> animationEvent, EntityModelData data, EntityMaid player, boolean update) {
        // 2023/6/21 这一块设计应该改成 molang 的，而且这个寻找效率低下
        // 2023/11/07 改善了寻找效率
        IBone head = getBone("Head");

        // 更新头部旋转
        if (head != null) {
            if (update) {
                headRot.set(head.getRotationX(), head.getRotationY());
            }
            head.setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
            head.setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
        }
    }

    @Override
    public DebugSource getDebugSource() {
        if (DebugAnimationKey.TYPE != DebugAnimationKey.DebugType.NONE) {
            return CustomDebugSource.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public boolean isModelPresent() {
        return ClientModelManager.getModels().containsKey(modelId);
    }

    @Override
    public int getTextureIndex() {
        if (isModelPresent()) {
            return ClientModelManager.getModel(modelId)
                    .map(model -> model.textures().keyList().indexOf(textureName))
                    .filter(i -> i >= 0)
                    .orElse(0);
        } else {
            return 0;
        }
    }

    @Override
    public IMaid getMaid() {
        return this.entity;
    }

    @Override
    public MaidModelInfo getMaidInfo() {
        return maidInfo;
    }

    @Override
    public void setMaidInfo(MaidModelInfo maidModelInfo) {
        if (this.maidInfo != maidModelInfo) {
            this.maidInfo = maidModelInfo;
        }
    }

    @Override
    public ILocationModel getGeoModel() {
        return this.getCurrentModel().getTlmAnimatedGeoModel();
    }

    @Override
    public void setYsmModel(String modelId, String texture) {
        this.modelId = modelId;
        this.textureName = texture;
    }

    @Override
    public boolean isTacGunAnimationNeedReload() {
        return tacGunAnimationNeedReload;
    }

    @Override
    public void setTacGunAnimationNeedReload(boolean tacGunAnimationNeedReload) {
        this.tacGunAnimationNeedReload = tacGunAnimationNeedReload;
    }
}
