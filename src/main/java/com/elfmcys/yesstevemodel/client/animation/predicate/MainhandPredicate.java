package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.api.IPlayerExtraInfo;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalHold;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class MainhandPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity().hasPreviewAnimation()) {
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

            String id = event.getAnimatableEntity().getModelId();
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

    private boolean isSameItem(ItemStack playerItem, IPlayerExtraInfo info, InteractionHand hand) {
        ItemStack preItem = info.getHandItem(hand);
        if (preItem.isDamaged()) {
            return ItemStack.isSameItem(playerItem, preItem);
        }
        return ItemStack.matches(playerItem, preItem);
    }

    private boolean checkSwingAndUse(Player player, InteractionHand hand) {
        if (player.swinging && player.swingingArm == hand) {
            return false;
        }
        return !player.isUsingItem() || player.getUsedItemHand() != hand;
    }
}
