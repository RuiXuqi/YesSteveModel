package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client;

import com.elfmcys.yesstevemodel.client.entity.HumanoidStateTracker;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public class MaidStateTracker extends HumanoidStateTracker<EntityMaid> {
    public MaidStateTracker(EntityMaid entity) {
        super(entity);
    }

    @Override
    public void reset() {
        super.reset();
    }
}
