package com.elfmcys.yesstevemodel.geckolib3.core.processor;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.function.BiConsumer;

public class DebugInfo {
    private final ReferenceArrayList<DebugItem> items = new ReferenceArrayList<>();
    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void add(Phase phase, String name, IValue exp) {
        items.add(new DebugItem(name, exp, phase));
    }

    public void remove(String name) {
        items.removeIf(item -> item.name.equals(name));
    }

    public void clear() {
        items.clear();
    }

    public void evaluatePre(ExpressionEvaluator<?> evaluator) {
        if(!enabled) {
            return;
        }
        for(DebugItem item : items) {
            if(item.phase == Phase.PRE_ANIMATION) {
                item.eval(evaluator);
            }
        }
    }

    public void evaluatePost(ExpressionEvaluator<?> evaluator) {
        if(!enabled) {
            return;
        }
        for(DebugItem item : items) {
            if(item.phase == Phase.POST_ANIMATION) {
                item.eval(evaluator);
            }
        }
    }

    public void enumerate(BiConsumer<String, Object> enumerator) {
        for(DebugItem item : items) {
            enumerator.accept(item.name, item.result);
        }
    }

    private static class DebugItem {
        private final String name;
        private final IValue value;
        private final Phase phase;
        private Object result;

        public DebugItem(String name, IValue value, Phase phase) {
            this.name = name;
            this.value = value;
            this.phase = phase;
        }

        public void eval(ExpressionEvaluator<?> evaluator) {
            try {
                result = value.evalUnsafe(evaluator);
            } catch (Exception e) {
                result = "Error: " + e.getMessage();
            }
        }

        public Object result() {
            return result;
        }

        public String name() {
            return name;
        }

        public Phase phase() {
            return phase;
        }
    }

    public enum Phase {
        PRE_ANIMATION,
        POST_ANIMATION
    }
}
