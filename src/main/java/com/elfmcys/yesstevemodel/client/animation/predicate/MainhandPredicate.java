package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.api.IEntityExtraInfo;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionalHold;
import com.elfmcys.yesstevemodel.client.compat.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.TlmClientCompat;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class MainhandPredicate implements IAnimationPredicate<AnimatableEntity<? extends LivingEntity>> {
    @Override
    public PlayState test(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity entity = event.getAnimatableEntity().getEntity();
        if (entity == null || event.getAnimatableEntity() instanceof IPreviewEntity) {
            return PlayState.STOP;
        }

        if (!checkSwingAndUse(entity, InteractionHand.MAIN_HAND)) {
            return PlayState.PAUSE;
        }

        ItemStack mainHandItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
        PlayState gunHoldAnimation = TACZCompat.playGunHoldAnimation(mainHandItem, event);
        if (gunHoldAnimation != null) {
            return gunHoldAnimation;
        }
        gunHoldAnimation = SWarfareCompat.playGunHoldAnimation(mainHandItem, event);
        if (gunHoldAnimation != null) {
            return gunHoldAnimation;
        }
        if (mainHandItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(mainHandItem)) {
            return playAnimation(event, "hold_mainhand:charged_crossbow");
        }
        boolean playerIsFishing = entity instanceof Player player && player.fishing != null;
        boolean maidIsFishing = TlmClientCompat.isMaidFishing(entity);
        if (playerIsFishing || maidIsFishing) {
            return playAnimation(event, "hold_mainhand:fishing");
        }

        if (event.getAnimatableEntity().getStateTracker() instanceof IEntityExtraInfo info && !isSameItem(mainHandItem, info, InteractionHand.MAIN_HAND)) {
            info.setHandItem(mainHandItem, InteractionHand.MAIN_HAND);
            event.getCodedController().indicateReload();
        }

        String id = event.getAnimatableEntity().getModelId();
        ConditionalHold conditionalHold = ClientModelManager.getModel(id).map(model -> model.conditionManager().getHoldMainhand()).orElse(null);
        if (conditionalHold != null) {
            String name = conditionalHold.doTest(entity, InteractionHand.MAIN_HAND);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name);
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
