package com.elfmcys.yesstevemodel.util;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

// Native Access
@SuppressWarnings("unused")
public class NativeUtil {
    // Native Access
    public static Object getDirection(int i) {
        switch(i) {
            case 1 : return Direction.DOWN;
            case 2 : return Direction.UP;
            case 3 : return Direction.NORTH;
            case 4 : return Direction.SOUTH;
            case 5 : return Direction.WEST;
            case 6 : return Direction.EAST;
            default: return null;
        }
    }

    // Native Access
    public static Object translatableText(String message, @Nullable Object[] args) {
        if (args == null || args.length == 0) {
            return Component.translatable(message);
        } else {
            return Component.translatable(message, args);
        }
    }

    // Native Access
    public static Object stringText(@Nullable String message) {
        return Component.literal(message == null ? "" : message);
    }

    // Native Access
    public static Object appendText(Object self, Object pSibling) {
        return ((MutableComponent) self).append((Component) pSibling);
    }
}
