package com.elfmcys.yesstevemodel.util;

import java.lang.reflect.Field;
import java.util.Optional;

public class ReflectionUtil {
    public static Optional<Field> getField(Class<?> clazz, String name, Class<?> type) {
        do {
            try {
                var field = clazz.getDeclaredField(name);
                if (field.getType() != type) {
                    break;
                }
                return Optional.of(field);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        } while (clazz != null);
        return Optional.empty();
    }
}
