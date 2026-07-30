package com.elfmcys.ysm.client.lang;

import com.elfmcys.ysm.client.model.ModelPackInfo;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 不使用原版的语言文件管理机制，而是直接写一个自己的实现
 */
public class LanguageManager {
    public static final String DEFAULT_LANGUAGE_CODE = "en_us";

    public static String getI18n(ModelPackInfo info, String key, @Nullable String defaultValue) {
        if (defaultValue == null) {
            defaultValue = "";
        }
        String local = Minecraft.getInstance().getLanguageManager().getSelected();
        Map<String, Map<String, String>> languages = info.lang();
        if (languages == null || languages.isEmpty()) {
            return defaultValue;
        }
        if (languages.containsKey(local)) {
            return languages.get(local).getOrDefault(key, defaultValue);
        } else if (languages.containsKey(DEFAULT_LANGUAGE_CODE)) {
            return languages.get(DEFAULT_LANGUAGE_CODE).getOrDefault(key, defaultValue);
        } else {
            return defaultValue;
        }
    }

    public static String getI18n(ModelRenderTarget data, String key, String defaultValue) {
        String local = Minecraft.getInstance().getLanguageManager().getSelected();
        return getI18n(data, local, key, defaultValue);
    }

    public static String getI18n(ModelRenderTarget data, String local, String key, String defaultValue) {
        Map<String, Map<String, String>> languages = data.assets().languageFiles();
        if (languages.isEmpty()) {
            return defaultValue;
        }
        if (languages.containsKey(local)) {
            return languages.get(local).getOrDefault(key, defaultValue);
        } else if (languages.containsKey(DEFAULT_LANGUAGE_CODE)) {
            return languages.get(DEFAULT_LANGUAGE_CODE).getOrDefault(key, defaultValue);
        } else {
            return defaultValue;
        }
    }

}
