package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionArmor;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.util.EquipmentUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import static com.elfmcys.yesstevemodel.client.animation.predicate.IAnimationPredicate.playAnimation;

public class ArmorPredicate implements IAnimationPredicate<CustomPlayerEntity> {
    private final EquipmentSlot slot;

    public ArmorPredicate(EquipmentSlot slot) {
        this.slot = slot;
    }

    @Override
    public PlayState test(AnimationEvent<CustomPlayerEntity> event, ExpressionEvaluator<?> evaluator) {
        Player player = event.getAnimatableEntity().getEntity();
        if (player == null || event.getAnimatableEntity().hasPreviewAnimation()) {
            return PlayState.STOP;
        }
        ItemStack itemBySlot = EquipmentUtil.getEquippedItem(player, slot);
        if (itemBySlot.isEmpty()) {
            return PlayState.STOP;
        }

        String id = event.getAnimatableEntity().getModelId();
        ConditionArmor conditionArmor = ClientModelManager.getModel(id).map(model -> model.conditionManager().getArmor()).orElse(null);
        if (conditionArmor != null) {
            String name = conditionArmor.doTest(player, slot);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, ILoopType.EDefaultLoopTypes.LOOP);
            }
        }

        String modelId = event.getAnimatableEntity().getModelId();
        String defaultName = slot.getName() + ":default";
        if (ClientModelManager.getPlayerAnimation(modelId, defaultName).isPresent()) {
            return playAnimation(event, defaultName, ILoopType.EDefaultLoopTypes.LOOP);
        }
        return PlayState.STOP;
    }
}
