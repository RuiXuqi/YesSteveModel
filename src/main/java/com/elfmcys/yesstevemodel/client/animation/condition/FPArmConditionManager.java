package com.elfmcys.yesstevemodel.client.animation.condition;

public class FPArmConditionManager {
    private final ConditionArmor armor = new ConditionArmor();

    public void addTest(String name) {
        armor.addTest(name);
    }

    public ConditionArmor getArmor() {
        return armor;
    }
}
