package com.elfmcys.yesstevemodel.geckolib3.core.event;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.MolangContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class InstructionKeyFrameExecutor {
    private final List<EventKeyFrame<IValue[]>> list;
    private int nextIndex = 0;

    public InstructionKeyFrameExecutor(List<EventKeyFrame<IValue[]>> list) {
        this.list = list;
    }

    private void evalValues(ExpressionEvaluator<?> evaluator, IValue[] values) {
        for (IValue value : values) {
            value.eval(evaluator);
        }
    }

    public void executeTo(ExpressionEvaluator<MolangContext<?>> evaluator, float currentTick, boolean dryRun) {
        evaluator.entity().setAllowEmitting(!dryRun);
        while (!reachEnd()) {
            EventKeyFrame<IValue[]> keyFrame = list.get(nextIndex);
            if (keyFrame.getStartTick() > currentTick) {
                break;
            }
            evalValues(evaluator, keyFrame.getEventData());
            nextIndex++;
        }
        evaluator.entity().setAllowEmitting(false);
    }

    public void executeRemaining(ExpressionEvaluator<MolangContext<?>> evaluator, boolean dryRun) {
        evaluator.entity().setAllowEmitting(!dryRun);
        for (int i = nextIndex; i < list.size(); i++) {
            evalValues(evaluator, list.get(i).getEventData());
        }
        evaluator.entity().setAllowEmitting(false);
        nextIndex = list.size();
    }

    public void tryExecuteLastFrame(ExpressionEvaluator<?> evaluator, float animationLength) {
        if (nextIndex < list.size()) {
            var last = list.get(list.size() - 1);
            if (last.getStartTick() == animationLength) {
                evalValues(evaluator, last.getEventData());
            }
        }
        nextIndex = list.size();
    }

    public boolean reachEnd() {
        return nextIndex >= list.size();
    }

    public void reset() {
        nextIndex = 0;
    }
}
