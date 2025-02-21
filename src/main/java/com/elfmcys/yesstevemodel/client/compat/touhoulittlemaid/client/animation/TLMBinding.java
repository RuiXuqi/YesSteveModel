package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public class TLMBinding extends ContextBinding {
    public static final TLMBinding INSTANCE = new TLMBinding();

    private TLMBinding() {
        maidEntityVar("is_begging", ctx -> ctx.entity().isBegging());
        maidEntityVar("is_sitting", ctx -> ctx.entity().isMaidInSittingPose());
        maidEntityVar("has_backpack", ctx -> ctx.entity().hasBackpack());
    }

    public void maidEntityVar(String name, IValueEvaluator<?, IContext<EntityMaid>> evaluator) {
        bindings.put(name, new MaidEntityVariable(evaluator));
    }
}