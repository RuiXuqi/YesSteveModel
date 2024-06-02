package com.elfmcys.yesstevemodel.client.animation;

import com.elfmcys.yesstevemodel.api.IPlayerExtraInfo;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.*;
import com.elfmcys.yesstevemodel.client.compat.TacGunRenderer;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.IAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public final class AnimationManager {
    public final static String TACZ_ID = "tacz";
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

    @NotNull
    public PlayState predicateMain(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatable().getEntity();
        if (player == null) {
            return PlayState.STOP;
        }
        if (event.getAnimatable().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        for (int i = Priority.HIGHEST; i <= Priority.LOWEST; i++) {
            for (AnimationState state : data[i]) {
                if (state.getPredicate().test(player, event)) {
                    String animationName = state.getAnimationName();
                    ILoopType loopType = state.getLoopType();
                    if (ModList.get().isLoaded(TACZ_ID) && TacGunRenderer.isGun(player.getMainHandItem())) {
                        return TacGunRenderer.playGunMainAnimation(event, animationName, loopType);
                    }
                    return playAnimation(event, animationName, loopType);
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
            ConditionalHold conditionalHold = ConditionManager.getHoldOffhand(id);
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
            if (ModList.get().isLoaded(TACZ_ID) && TacGunRenderer.isGun(mainHandItem)) {
                return TacGunRenderer.playGunHoldAnimation(event, mainHandItem);
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
            ConditionalHold conditionalHold = ConditionManager.getHoldMainhand(id);
            if (conditionalHold != null) {
                String name = conditionalHold.doTest(player, InteractionHand.MAIN_HAND);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                }
            }
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
            ConditionalSwing conditionalSwing = (player.swingingArm == InteractionHand.MAIN_HAND) ? ConditionManager.getSwingMainhand(id) : ConditionManager.getSwingOffhand(id);
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
                ConditionalUse conditionalUse = ConditionManager.getUseMainhand(id);
                if (conditionalUse != null) {
                    String name = conditionalUse.doTest(player, InteractionHand.MAIN_HAND);
                    if (StringUtils.isNoneBlank(name)) {
                        return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
                    }
                }
                return playAnimation(event, "use_mainhand", ILoopType.EDefaultLoopTypes.LOOP);
            } else {
                String id = event.getAnimatable().getModelId();
                ConditionalUse conditionalUse = ConditionManager.getUseOffhand(id);
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
        ItemStack itemBySlot = player.getItemBySlot(slot);
        if (itemBySlot.isEmpty()) {
            return PlayState.STOP;
        }

        String id = event.getAnimatable().getModelId();
        ConditionArmor conditionArmor = ConditionManager.getArmor(id);
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

    private boolean checkSwingAndUse(Player player, InteractionHand hand) {
        if (player.swinging && player.swingingArm == hand) {
            return false;
        }
        return !player.isUsingItem() || player.getUsedItemHand() != hand;
    }
}