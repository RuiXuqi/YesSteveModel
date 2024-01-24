package com.elfmcys.yesstevemodel.client.animation.condition;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.Map;

public class ConditionManager {
    public static ConditionManager INSTANCE = new ConditionManager();

    private final Map<ResourceLocation, ConditionalSwing> swing = Maps.newHashMap();
    private final Map<ResourceLocation, ConditionalUse> useMainHand = Maps.newHashMap();
    private final Map<ResourceLocation, ConditionalUse> useOffHand = Maps.newHashMap();
    private final Map<ResourceLocation, ConditionalHold> holdMainHand = Maps.newHashMap();
    private final Map<ResourceLocation, ConditionalHold> holdOffHand = Maps.newHashMap();
    private final Map<ResourceLocation, ConditionArmor> amor = Maps.newHashMap();

    public void addTest(ResourceLocation id, String name) {
        swing.putIfAbsent(id, new ConditionalSwing());
        useMainHand.putIfAbsent(id, new ConditionalUse(InteractionHand.MAIN_HAND));
        useOffHand.putIfAbsent(id, new ConditionalUse(InteractionHand.OFF_HAND));
        holdMainHand.putIfAbsent(id, new ConditionalHold(InteractionHand.MAIN_HAND));
        holdOffHand.putIfAbsent(id, new ConditionalHold(InteractionHand.OFF_HAND));
        amor.putIfAbsent(id, new ConditionArmor());

        ConditionalSwing conditionalSwing = swing.get(id);
        ConditionalUse conditionalUseMainhand = useMainHand.get(id);
        ConditionalUse conditionalUseOffhand = useOffHand.get(id);
        ConditionalHold conditionalHoldMainhand = holdMainHand.get(id);
        ConditionalHold conditionalHoldOffhand = holdOffHand.get(id);
        ConditionArmor conditionArmor = amor.get(id);

        conditionalSwing.addTest(name);
        conditionalUseMainhand.addTest(name);
        conditionalUseOffhand.addTest(name);
        conditionalHoldMainhand.addTest(name);
        conditionalHoldOffhand.addTest(name);
        conditionArmor.addTest(name);
    }

    public static void setInstance(ConditionManager instance) {
        INSTANCE = instance;
    }

    public static ConditionalSwing getSwing(ResourceLocation id) {
        return INSTANCE.swing.get(id);
    }

    public static ConditionalUse getUseMainhand(ResourceLocation id) {
        return INSTANCE.useMainHand.get(id);
    }

    public static ConditionalUse getUseOffhand(ResourceLocation id) {
        return INSTANCE.useOffHand.get(id);
    }

    public static ConditionalHold getHoldMainhand(ResourceLocation id) {
        return INSTANCE.holdMainHand.get(id);
    }

    public static ConditionalHold getHoldOffhand(ResourceLocation id) {
        return INSTANCE.holdOffHand.get(id);
    }

    public static ConditionArmor getArmor(ResourceLocation id) {
        return INSTANCE.amor.get(id);
    }
}
