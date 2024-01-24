package com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.PooledStringHashSet;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import javax.annotation.Nonnull;
import java.util.List;

public class ModelScript {
    @Nonnull
    private final PooledStringHashSet publicVariableNames;
    @Nonnull
    private final List<IValue> initializationValues;
    @Nonnull
    private final List<IValue> preAnimationValues;

    public ModelScript(String[] publicVariableNames, IValue[] initializationValues, IValue[] preAnimationValues) {
        this.publicVariableNames = new PooledStringHashSet(ReferenceArrayList.wrap(publicVariableNames));
        this.initializationValues = ReferenceArrayList.wrap(initializationValues);
        this.preAnimationValues = ReferenceArrayList.wrap(preAnimationValues);
    }

    @Nonnull
    public PooledStringHashSet publicVariableNames() {
        return publicVariableNames;
    }

    @Nonnull
    public List<IValue> initializationValues() {
        return initializationValues;
    }

    @Nonnull
    public List<IValue> preAnimationValues() {
        return preAnimationValues;
    }
}
