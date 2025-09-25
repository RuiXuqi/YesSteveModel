package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventWrapper;
import com.elfmcys.yesstevemodel.client.animation.predicate.*;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.CodedAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.*;

public abstract class CustomPlayerEntity extends CustomHumanoidEntity<Player> {
    protected final boolean localPlayer;

    protected boolean isPlayingExtraAnimation = false;
    protected String extraAnimationName = "idle";
    protected boolean isExtraAnimationDirty = false;

    private List<IValue> syncHandler = null;

    public CustomPlayerEntity(Player player, boolean localPlayer, boolean asyncUpdate) {
        super(player, asyncUpdate);
        this.localPlayer = localPlayer;
        if (player instanceof LocalPlayer) {
            setInitialized();
        }
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

        ParCoolCompat.addParcoolPredicate(this);
        addAnimationController(new HybridAnimationController(this, VEHICLE_CONTROLLER, 0.1f, new VehiclePredicate()));

        addAnimationController(new HybridAnimationController(this, PRE_MAIN_CONTROLLER, 0, new EmptyPredicate()));
        addAnimationController(new HybridAnimationController(this, MAIN_CONTROLLER, 0.1f, new PlayerMainPredicate()));
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

        addAnimationController(new HybridAnimationController(this, PASSENGER_CONTROLLER, 0.1f, new PassengerPredicate()));
        CarryOnCompat.addCarryOnPredicate(this);

        // 下面不需要自定义动画控制器
        addAnimationController(new CodedAnimationController(this, CAP_CONTROLLER, 0, new CapPredicate()));
        if (this instanceof IPreviewEntity) {
            addAnimationController(new CodedAnimationController(this, HOVER_CONTROLLER, 0, new HoverPredicate()));
            addAnimationController(new CodedAnimationController(this, FOCUS_CONTROLLER, 0, new FocusPredicate()));
        }

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

    @Nullable
    public Struct getRoamingStruct() {
        return null;
    }

    public boolean isLocalPlayer() {
        return localPlayer;
    }

    /**
     * 现在幂等了
     */
    @Override
    public boolean shouldForceUpdate() {
        return currentFrameRenderTimes > 1 || !RenderUtil.isRenderingLevel();
    }

    @Deprecated
    @Override
    protected void codeAnimation(AnimationEvent<CustomPlayerEntity> animationEvent, EntityModelData data, boolean update) {
        super.codeAnimation(animationEvent,data, update);

        GeoModelState model = getLoadedGeoModel();

        // 更新第一人称相机偏移与头部隐藏
        if (model != null && animationEvent.getAnimatableEntity().isLocalPlayer()) {
            if (FirstPersonCompat.isInstalled()) {
                if (model.firstPersonHead() != null) {
                    model.firstPersonHead().setHidden(FirstPersonCompat.shouldHideHead());
                }
                if (model.firstPersonViewLocator() != null) {
                    FirstPersonCompat.setHeadPos(model.firstPersonViewLocator().getPivotY() * animationEvent.getAnimatableEntity().getHeightScale());
                } else if (update) {
                    if (!model.headBones().isEmpty()) {
                        var head = model.headBones().get(model.headBones().size() - 1);
                        FirstPersonCompat.setHeadPos(head == null ? 24f : (head.getPivotY() * animationEvent.getAnimatableEntity().getHeightScale()));
                    }
                }
            }

            if (BetterCombatCompat.isInstalled() && model.firstPersonHead() != null) {
                model.firstPersonHead().setHidden(BetterCombatCompat.shouldHideHead(this));
            }
        }
    }

    @Override
    protected void onLoadGeoModel(GeoModelState model) {
        super.onLoadGeoModel(model);
        syncHandler = getEventHandler(MolangEventWrapper.SYNC);
    }

    public void playExtraAnimation(String animationName) {
        if (getAnimation(animationName)!= null) {
            this.extraAnimationName = animationName;
            this.isPlayingExtraAnimation = true;
            this.isExtraAnimationDirty = true;
        } else {
            this.isPlayingExtraAnimation = false;
        }
    }

    public void clearExtraAnimationDirty() {
        this.isExtraAnimationDirty = false;
    }

    public boolean isPlayingExtraAnimation() {
        return isPlayingExtraAnimation;
    }

    public boolean shouldResetExtraAnimation() {
        return isExtraAnimationDirty;
    }

    public String getExtraAnimationName() {
        return this.extraAnimationName;
    }

    public void stopExtraAnimation() {
        this.isPlayingExtraAnimation = false;
    }

    @Override
    protected void preAnimationSetup(float seekTime) {
        super.preAnimationSetup(seekTime);
        // 设置 roaming 变量
        getAnimationProcessor().putRemoteStruct(getRoamingStruct());
    }

    public void molangSync(FloatArrayList args) {
        if (syncHandler != null) {
            executeMolangExp(MolangEventWrapper.wrap(syncHandler, args), true, false, null);
        }
    }
}
