package com.elfmcys.yesstevemodel.client.animation;

import com.elfmcys.yesstevemodel.api.IPlayerExtraInfo;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.*;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.client.compat.swem.SwemCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.util.EquipmentUtil;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class AnimationManager {
    private static AnimationManager MANAGER;
    @SuppressWarnings("unchecked")
    private final ReferenceArrayList<AnimationState>[] data = new ReferenceArrayList[Priority.LOWEST + 1];

    public AnimationManager() {
        for (int i = 0; i < data.length; i++) {
            data[i] = new ReferenceArrayList<>(6);
        }
    }

    public static AnimationManager getInstance() {
        if (MANAGER == null) {
            MANAGER = new AnimationManager();
        }
        return MANAGER;
    }

    @NotNull
    public static <P extends IAnimatable<?>> PlayState playLoopAnimation(AnimationEvent<P> event, String animationName) {
        return playAnimation(event, animationName, ILoopType.EDefaultLoopTypes.LOOP);
    }

    @NotNull
    private static <P extends IAnimatable<?>> PlayState playAnimation(AnimationEvent<P> event, String animationName, ILoopType loopType) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName, loopType));
        return PlayState.CONTINUE;
    }

    @NotNull
    private static <P extends IAnimatable<?>> PlayState playAnimation(AnimationEvent<P> event, String animationName) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName));
        return PlayState.CONTINUE;
    }

    public void register(AnimationState state) {
        data[state.getPriority()].add(state);
    }

    public PlayState predicateParallel(AnimationEvent<CustomPlayerEntity> event, String animationName) {
        if (Minecraft.getInstance().isPaused()) {
            return PlayState.STOP;
        }
        return playLoopAnimation(event, animationName);
    }

    public PlayState predicateCap(AnimationEvent<CustomPlayerEntity> event) {
        CustomPlayerEntity animatable = event.getAnimatable();
        if (animatable.hasPreviewAnimation()) {
            return playLoopAnimation(event, animatable.getPreviewAnimation());
        }

        return animatable.getEntity().getCapability(PlayerGeoCapabilityProvider.CAP).map(cap -> {
            if (cap.isPlayingAnimation()) {
                if (cap.isAnimationDirty()) {
                    cap.clearAnimationDirty();
                    event.getController().markNeedsReload();
                }
                return playAnimation(event, cap.getAnimationName());
            }
            return PlayState.STOP;
        }).orElse(PlayState.STOP);
    }

    public PlayState predicateHover(AnimationEvent<CustomPlayerEntity> event) {
        String hoverAnimation = event.getAnimatable().getHoverAnimation();
        if (StringUtils.isNoneBlank(hoverAnimation)) {
            return playLoopAnimation(event, hoverAnimation);
        }
        return PlayState.STOP;
    }

    public PlayState predicateFocus(AnimationEvent<CustomPlayerEntity> event) {
        String focusAnimation = event.getAnimatable().getFocusAnimation();
        if (StringUtils.isNoneBlank(focusAnimation)) {
            return playLoopAnimation(event, focusAnimation);
        }
        return PlayState.STOP;
    }

    @NotNull
    public PlayState predicateMain(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null) {
            return PlayState.STOP;
        }
        if (event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }

        // 跑酷模组兼容
        String parCoolAnimation = ParCoolCompat.getAnimation(player);
        if (parCoolAnimation != null) {
            String modelId = event.getAnimatable().getModelId();
            Optional<Animation> optional = ClientModelManager.getPlayerAnimation(modelId, parCoolAnimation);
            if (optional.isPresent()) {
                return playAnimation(event, parCoolAnimation);
            }
        }

        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            // 载具动画单独检查
            if (i == Priority.HIGH) {
                PlayState vehicleAnimation = getVehicleAnimation(event);
                if (vehicleAnimation != null) {
                    return vehicleAnimation;
                }
            }
            for (AnimationState state : data[i]) {
                if (state.getPredicate().test(player, event)) {
                    String animationName = state.getAnimationName();
                    ILoopType loopType = state.getLoopType();
                    PlayState gunMainAnimation = TACZCompat.playGunMainAnimation(player, event, animationName, loopType);
                    return Objects.requireNonNullElseGet(gunMainAnimation, () -> playAnimation(event, animationName, loopType));
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateOffhandHold(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (!player.swinging && !player.isUsingItem()) {
            ItemStack offhandItem = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offhandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(offhandItem)) {
                return playAnimation(event, "hold_offhand:charged_crossbow", ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        if (checkSwingAndUse(player, InteractionHand.OFF_HAND)) {
            ItemStack offhandItem = player.getItemInHand(InteractionHand.OFF_HAND);
            if (player instanceof IPlayerExtraInfo info && !isSameItem(offhandItem, info, InteractionHand.OFF_HAND)) {
                info.setHandItem(offhandItem, InteractionHand.OFF_HAND);
                playAnimation(event, "empty", ILoopType.EDefaultLoopTypes.LOOP);
            }

            String id = event.getAnimatable().getModelId();
            ConditionalHold conditionalHold = ClientModelManager.getModel(id).map(model -> model.conditionManager().getHoldOffhand()).orElse(null);
            if (conditionalHold != null) {
                String name = conditionalHold.doTest(player, InteractionHand.OFF_HAND);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateMainhandHold(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (!player.swinging && !player.isUsingItem()) {
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            PlayState gunHoldAnimation = TACZCompat.playGunHoldAnimation(mainHandItem, event);
            if (gunHoldAnimation != null) {
                return gunHoldAnimation;
            }
            if (mainHandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(mainHandItem)) {
                return playAnimation(event, "hold_mainhand:charged_crossbow", ILoopType.EDefaultLoopTypes.LOOP);
            }
            if (player.fishing != null) {
                return playAnimation(event, "hold_mainhand:fishing", ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        if (checkSwingAndUse(player, InteractionHand.MAIN_HAND)) {
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (player instanceof IPlayerExtraInfo info && !isSameItem(mainHandItem, info, InteractionHand.MAIN_HAND)) {
                info.setHandItem(mainHandItem, InteractionHand.MAIN_HAND);
                playAnimation(event, "empty", ILoopType.EDefaultLoopTypes.LOOP);
            }

            String id = event.getAnimatable().getModelId();
            ConditionalHold conditionalHold = ClientModelManager.getModel(id).map(model -> model.conditionManager().getHoldMainhand()).orElse(null);
            if (conditionalHold != null) {
                String name = conditionalHold.doTest(player, InteractionHand.MAIN_HAND);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateMainhandFire(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (!player.swinging && !player.isUsingItem()) {
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            return TACZCompat.playGunFireAnimation(mainHandItem, event);
        }
        return PlayState.STOP;
    }

    private boolean isSameItem(ItemStack playerItem, IPlayerExtraInfo info, InteractionHand hand) {
        ItemStack preItem = info.getHandItem(hand);
        if (preItem.isDamaged()) {
            return ItemStack.isSameItem(playerItem, preItem);
        }
        return ItemStack.matches(playerItem, preItem);
    }

    public PlayState predicateSwing(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (player.swinging && !player.isSleeping()) {
            if (player.swingTime == 0) {
                // 空动画用于重置 PLAY_ONCE 动画
                playAnimation(event, "empty", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
            String id = event.getAnimatable().getModelId();
            ConditionalSwing conditionalSwing = ClientModelManager.getModel(id).map(model -> (player.swingingArm == InteractionHand.MAIN_HAND) ? model.conditionManager().getSwingMainhand() : model.conditionManager().getSwingOffhand()).orElse(null);
            if (conditionalSwing != null) {
                String name = conditionalSwing.doTest(player, player.swingingArm);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
                }
            }
            String defaultSwing = (player.swingingArm == InteractionHand.MAIN_HAND) ? "swing_hand" : "swing_offhand";
            return playAnimation(event, defaultSwing, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        return PlayState.CONTINUE;
    }

    public PlayState predicateUse(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        if (player.isUsingItem() && !player.isSleeping()) {
            if (player.getTicksUsingItem() == 1) {
                playAnimation(event, "empty", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
            if (player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                String id = event.getAnimatable().getModelId();
                ConditionalUse conditionalUse = ClientModelManager.getModel(id).map(model -> model.conditionManager().getUseMainhand()).orElse(null);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(player, InteractionHand.MAIN_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_mainhand", ILoopType.EDefaultLoopTypes.LOOP);
            } else {
                String id = event.getAnimatable().getModelId();
                ConditionalUse conditionalUse = ClientModelManager.getModel(id).map(model -> model.conditionManager().getUseOffhand()).orElse(null);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(player, InteractionHand.OFF_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_offhand", ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateArmor(AnimationEvent<CustomPlayerEntity> event, EquipmentSlot slot) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        ItemStack itemBySlot = EquipmentUtil.getEquippedItem(player, slot);
        if (itemBySlot.isEmpty()) {
            return PlayState.STOP;
        }

        String id = event.getAnimatable().getModelId();
        ConditionArmor conditionArmor = ClientModelManager.getModel(id).map(model -> model.conditionManager().getArmor()).orElse(null);
        if (conditionArmor != null) {
            String name = conditionArmor.doTest(player, slot);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        String modelId = event.getAnimatable().getModelId();
        String defaultName = slot.getName() + ":default";
        if (ClientModelManager.getPlayerAnimation(modelId, defaultName).isPresent()) {
            return playAnimation(event, defaultName, ILoopType.EDefaultLoopTypes.LOOP);
        }
        return PlayState.STOP;
    }

    @Nullable
    public PlayState getVehicleAnimation(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return null;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !vehicle.isAlive()) {
            return null;
        }

        String swemAnimation = SwemCompat.getAnimation(player);
        if (StringUtils.isNoneBlank(swemAnimation)) {
            return playAnimation(event, swemAnimation, ILoopType.EDefaultLoopTypes.LOOP);
        }

        String id = event.getAnimatable().getModelId();
        ConditionalVehicle vehicleCondition = ClientModelManager.getModel(id).map(model -> model.conditionManager().getVehicle()).orElse(null);
        if (vehicleCondition != null) {
            String name = vehicleCondition.doTest(player);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        // 兼容旧版本的几个动画
        if (vehicle instanceof Pig) {
            return playAnimation(event, "ride_pig", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (vehicle instanceof Saddleable) {
            return playAnimation(event, "ride", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (vehicle instanceof Boat) {
            return playAnimation(event, "boat", ILoopType.EDefaultLoopTypes.LOOP);
        }
        if (CarryOnCompat.isCarryOnLoaded() && CarryOnCompat.isCarryOnPrincess(player, event)) {
            return playAnimation(event, "carryon:princess", ILoopType.EDefaultLoopTypes.LOOP);
        }
        return playAnimation(event, "sit", ILoopType.EDefaultLoopTypes.LOOP);
    }

    public PlayState predicatePassengerAnimation(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null || event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        Entity passenger = player.getFirstPassenger();
        if (passenger == null || !passenger.isAlive()) {
            return PlayState.STOP;
        }

        String id = event.getAnimatable().getModelId();
        ConditionalPassenger conditionalPassenger = ClientModelManager.getModel(id).map(model -> model.conditionManager().getPassenger()).orElse(null);
        if (conditionalPassenger != null) {
            String name = conditionalPassenger.doTest(player);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }
        return PlayState.STOP;
    }

    private boolean checkSwingAndUse(Player player, InteractionHand hand) {
        if (player.swinging && player.swingingArm == hand) {
            return false;
        }
        return !player.isUsingItem() || player.getUsedItemHand() != hand;
    }
}