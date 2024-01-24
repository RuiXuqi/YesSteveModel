package com.elfmcys.yesstevemodel.geckolib3.core.event;

import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.event.EventKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class InstructionKeyFrameVisitor {
    private final List<EventKeyFrame<IValue[]>> list;
    private int nextIndex = 0;

    public InstructionKeyFrameVisitor(List<EventKeyFrame<IValue[]>> list) {
        this.list = list;
    }

    private void evalValues(ExpressionEvaluator<?> evaluator, IValue[] values) {
        for (IValue value : values) {
            value.evalAsDouble(evaluator);
        }
    }

    public void visit(ExpressionEvaluator<?> evaluator, double currentTick) {
        while (!reachEnd()) {
            EventKeyFrame<IValue[]> keyFrame = list.get(nextIndex);
            if (keyFrame.getStartTick() > currentTick) {
                return;
            }
            evalValues(evaluator, keyFrame.getEventData());
            nextIndex++;
        }
    }

    public void visitRemaining(ExpressionEvaluator<?> evaluator) {
        for (int i = nextIndex; i < list.size(); i++) {
            evalValues(evaluator, list.get(i).getEventData());
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
