package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import com.elfmcys.yesstevemodel.util.InputCheckUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class InputCheck {
    public static class Keyboard implements Function {
        @Override
        public @Nullable Object evaluate(@NotNull ExecutionContext<?> context, @NotNull ArgumentCollection arguments) {
            if (!InputCheckUtil.isInGame()) {
                return false;
            }
            int keyCode = arguments.getAsInt(context, 0);
            return GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), keyCode) == GLFW.GLFW_PRESS;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size == 1;
        }
    }

    public static class Mouse implements Function {
        @Override
        public @Nullable Object evaluate(@NotNull ExecutionContext<?> context, @NotNull ArgumentCollection arguments) {
            if (!InputCheckUtil.isInGame()) {
                return false;
            }
            int button = arguments.getAsInt(context, 0);
            return GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), button) == GLFW.GLFW_PRESS;
        }

        @Override
        public boolean validateArgumentSize(int size) {
            return size == 1;
        }
    }
}
