package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MolangEventWrapper {
    public static final int PLAYER_INIT = StringPool.computeIfAbsent("player_init");
    public static final int PLAYER_UPDATE = StringPool.computeIfAbsent("player_update");
    public static final int SYNC = StringPool.computeIfAbsent("sync");

    public static IValue wrap(List<IValue> handlers, float @Nullable [] args) {
        return wrap(handlers, args != null ? FloatArrayList.wrap(args) : FloatLists.emptyList());
    }

    public static IValue wrap(List<IValue> handlers, Object @Nullable [] args) {
        return wrap(handlers, args != null ? ObjectArrayList.wrap(args) : ObjectLists.emptyList());
    }

    public static IValue wrap(List<IValue> handlers) {
        return wrap(handlers, ObjectLists.emptyList());
    }

    private static IValue wrap(List<IValue> handlers, List<?> array) {
        return evaluator -> {
            if (evaluator.entity() instanceof IContext<?> ctx) {
                for (var handler : handlers) {
                    return ctx.callUserFunction(evaluator, handler, array);
                }
            }
            return null;
        };
    }
}
