package com.elfmcys.yesstevemodel.client.animation.condition;

import com.google.common.collect.Maps;
import net.minecraft.world.InteractionHand;

import java.util.Map;

public class ConditionManager {
    public static ConditionManager INSTANCE = new ConditionManager();

    private final Map<String, ConditionalSwing> swing = Maps.newHashMap();
    private final Map<String, ConditionalSwing> swingOffhand = Maps.newHashMap();
    private final Map<String, ConditionalUse> useMainHand = Maps.newHashMap();
    private final Map<String, ConditionalUse> useOffHand = Maps.newHashMap();
    private final Map<String, ConditionalHold> holdMainHand = Maps.newHashMap();
    private final Map<String, ConditionalHold> holdOffHand = Maps.newHashMap();
    private final Map<String, ConditionArmor> amor = Maps.newHashMap();
    private final Map<String, ConditionTAC> tac = Maps.newHashMap();

    public void addTest(String modelId, String name) {
        ConditionalSwing conditionalSwing = swing.computeIfAbsent(modelId, id -> new ConditionalSwing(InteractionHand.MAIN_HAND));
        ConditionalSwing conditionalSwingOffhand = swingOffhand.computeIfAbsent(modelId, id -> new ConditionalSwing(InteractionHand.OFF_HAND));
        ConditionalUse conditionalUseMainhand = useMainHand.computeIfAbsent(modelId, id -> new ConditionalUse(InteractionHand.MAIN_HAND));
        ConditionalUse conditionalUseOffhand = useOffHand.computeIfAbsent(modelId, id -> new ConditionalUse(InteractionHand.OFF_HAND));
        ConditionalHold conditionalHoldMainhand = holdMainHand.computeIfAbsent(modelId, id -> new ConditionalHold(InteractionHand.MAIN_HAND));
        ConditionalHold conditionalHoldOffhand = holdOffHand.computeIfAbsent(modelId, id -> new ConditionalHold(InteractionHand.OFF_HAND));
        ConditionArmor conditionArmor = amor.computeIfAbsent(modelId, id -> new ConditionArmor());
        ConditionTAC conditionTAC = tac.computeIfAbsent(modelId, id -> new ConditionTAC());

        conditionalSwing.addTest(name);
        conditionalSwingOffhand.addTest(name);
        conditionalUseMainhand.addTest(name);
        conditionalUseOffhand.addTest(name);
        conditionalHoldMainhand.addTest(name);
        conditionalHoldOffhand.addTest(name);
        conditionArmor.addTest(name);
        conditionTAC.addTest(name);
    }

    public static void setInstance(ConditionManager instance) {
        INSTANCE = instance;
    }

    public static ConditionalSwing getSwingMainhand(String modelId) {
        return INSTANCE.swing.get(modelId);
    }

    public static ConditionalSwing getSwingOffhand(String modelId) {
        return INSTANCE.swingOffhand.get(modelId);
    }

    public static ConditionalUse getUseMainhand(String modelId) {
        return INSTANCE.useMainHand.get(modelId);
    }

    public static ConditionalUse getUseOffhand(String modelId) {
        return INSTANCE.useOffHand.get(modelId);
    }

    public static ConditionalHold getHoldMainhand(String modelId) {
        return INSTANCE.holdMainHand.get(modelId);
    }

    public static ConditionalHold getHoldOffhand(String modelId) {
        return INSTANCE.holdOffHand.get(modelId);
    }

    public static ConditionArmor getArmor(String modelId) {
        return INSTANCE.amor.get(modelId);
    }

    public static ConditionTAC getTAC(String modelId) {
        return INSTANCE.tac.get(modelId);
    }
}
