package com.elfmcys.yesstevemodel.util;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

// Native Access
@SuppressWarnings("unused")
public class NativeUtil {
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

    // Native Access
    public static int[] sortTextureNamesAsLegacy(String[] textureNames) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < textureNames.length; i++) {
            hashMap.put(textureNames[i] + ".png", i);
        }
        return hashMap.keySet().stream().mapToInt(hashMap::get).toArray();
    }
}
