package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client;

import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidMiscPredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidRoulettePredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidStatuePredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.YsmMaidMainPredicate;
import com.elfmcys.yesstevemodel.client.entity.CustomHumanoidEntity;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

/**
 * 基于 CustomPlayerEntity 复制来的，基本上没做删除，试想尝试让女仆能调用轮盘动画之类的,所以就先预留着
 */
@OnlyIn(Dist.CLIENT)
public class CustomYsmMaidEntity extends CustomHumanoidEntity<EntityMaid> implements IGeoEntity {
    private final PhysicsManager physicsGuiManager;

    private MaidModelInfo maidInfo = new MaidModelInfo();

    public CustomYsmMaidEntity(EntityMaid player, boolean asyncUpdate) {
        super(player, asyncUpdate);
        registerControllers();
        physicsGuiManager = new PhysicsManager();
    }

    @Override
    protected @NotNull ResourceHolder createResourceHolder(ClientModel model) {
        return new HumanoidResourceHolder(model, true, true, 30 * 20);
    }

    @Override
    protected MaidStateTracker createStateTracker(EntityMaid entity) {
        return new MaidStateTracker(entity);
    }

    @Override
    public MaidStateTracker getStateTracker() {
        return (MaidStateTracker) super.getStateTracker();
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

        addAnimationController(new HybridAnimationController(this, PRE_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, MAIN_CONTROLLER, 0.1f, new YsmMaidMainPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_MAIN_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_HOLD_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_OFFHAND_CONTROLLER, 0.1f, new OffhandPredicate()));
        addAnimationController(new HybridAnimationController(this, HOLD_MAINHAND_CONTROLLER, 0.1f, new MainhandPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_HOLD_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, GUN_FIRE_CONTROLLER, 0f, new GunFirePredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_SWING_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, SWING_CONTROLLER, 0, new SwingPredicate()));
        addAnimationController(new HybridAnimationController(this, POST_SWING_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_USE_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, USE_CONTROLLER, 0.1f, new UsePredicate()));
        addAnimationController(new HybridAnimationController(this, POST_USE_CONTROLLER, 0, new EmptyPredicate()));

        addAnimationController(new HybridAnimationController(this, MAID_MISC, 0.1f, new MaidMiscPredicate()));
        addAnimationController(new HybridAnimationController(this, PASSENGER_CONTROLLER, 0.1f, new PassengerPredicate()));

        // 下面不需要自定义动画控制器
        {
            addAnimationController(new CodedAnimationController(this, CAP_CONTROLLER, 0.1f, new MaidRoulettePredicate()));
        }

        // 高并行动画
        for (int i = 0; i < 8; i++) {
            String controllerName = PARALLEL_CONTROLLER + i;
            String animationName = String.format("parallel%d", i);
            addAnimationController(new HybridAnimationController(this, controllerName, 0,
                    new ParallelPredicate(animationName), true));
        }

        // 护甲动画
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                String controllerName = ARMOR_CONTROLLER + slot.getName();
                addAnimationController(new HybridAnimationController(this, controllerName, 0, new ArmorPredicate(slot)));
            }
        }

        // 雕像动画控制器优先级最高，可以拿来禁用前面所有的动画，避免雕像或者手办还在运动
        addAnimationController(new HybridAnimationController(this, MAID_STATUE, 0, new MaidStatuePredicate()));
    }

    public boolean shouldResetRouletteAnim() {
        return this.entity.rouletteAnimDirty;
    }

    public void clearRouletteAnimDirty() {
        this.entity.rouletteAnimDirty = false;
    }

    public boolean isRouletteAnimPlaying() {
        return this.entity.rouletteAnimPlaying;
    }

    public String getRouletteAnim() {
        return this.entity.rouletteAnim;
    }

    public void setRemoteStruct(Object2FloatOpenHashMap<String> roamingVars) {
        // TODO
    }

    @Override
    public void updateRoamingVars(Object2FloatOpenHashMap<String> roamingVars) {
        // TODO
    }

    public Struct getRemoteStruct() {
        // TODO
        return null;
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        super.preAnimationSetup(seekTime);

        getAnimationProcessor().putRemoteStruct(getRemoteStruct());

        // 更新物理
        if (getPhysicsManager() == physicsGuiManager)
            physicsGuiManager.update(seekTime);
    }

    @Override
    public boolean shouldForceUpdate() {
        return currentFrameRenderTimes > 1 || !NativeRenderer.isAsyncScope();
    }

    @Override
    public PhysicsManager getPhysicsManager() {
        if (NativeRenderer.isAsyncScope() || RenderUtil.isRenderingEntitiesInPaperDoll()) {
            return physicsManager;
        } else {
            return physicsGuiManager;
        }
    }

    @Override
    protected void onLoadGeoModel(GeoModelState model) {
        super.onLoadGeoModel(model);
        physicsGuiManager.reset();
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
        return this.getLoadedGeoModel().getTlmAnimatedGeoModel();
    }

    @Override
    public void setYsmModel(String modelId, String texture) {
        updateModelAndTexture(modelId, texture);
    }
}
