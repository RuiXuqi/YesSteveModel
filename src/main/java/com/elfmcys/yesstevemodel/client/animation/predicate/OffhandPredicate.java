package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.api.IEntityExtraInfo;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalHold;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.LoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class OffhandPredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }
        if (!entity.swinging && !entity.isUsingItem()) {
            ItemStack offhandItem = entity.getItemInHand(InteractionHand.OFF_HAND);
            if (offhandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(offhandItem)) {
                return playAnimation(event, "hold_offhand:charged_crossbow", LoopType.LOOP);
            }
        }
        if (checkSwingAndUse(entity, InteractionHand.OFF_HAND)) {
            ItemStack offhandItem = entity.getItemInHand(InteractionHand.OFF_HAND);
            if (event.getAnimatableEntity().getStateTracker() instanceof IEntityExtraInfo info && !isSameItem(offhandItem, info, InteractionHand.OFF_HAND)) {
                info.setHandItem(offhandItem, InteractionHand.OFF_HAND);
                event.getCodedController().forceReload();
            }

            String id = event.getAnimatableEntity().getModelId();
            ConditionalHold conditionalHold = ClientModelManager.getModel(id).map(model -> model.conditionManager().getHoldOffhand()).orElse(null);
            if (conditionalHold != null) {
                String name = conditionalHold.doTest(entity, InteractionHand.OFF_HAND);
                if (StringUtils.isNoneBlank(name)) {
                    return playAnimation(event, name, LoopType.LOOP);
                }
            }
        }
        return PlayState.STOP;
    }

    private boolean isSameItem(ItemStack playerItem, IEntityExtraInfo info, InteractionHand hand) {
        ItemStack preItem = info.getHandItem(hand);
        if (preItem.isDamaged()) {
            return ItemStack.isSameItem(playerItem, preItem);
        }
        return ItemStack.matches(playerItem, preItem);
    }

    private boolean checkSwingAndUse(LivingEntity entity, InteractionHand hand) {
        if (entity.swinging && entity.swingingArm == hand) {
            return false;
        }
        return !entity.isUsingItem() || entity.getUsedItemHand() != hand;
    }
}
