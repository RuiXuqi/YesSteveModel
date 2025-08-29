package com.elfmcys.yesstevemodel.client.lang;

import com.elfmcys.yesstevemodel.client.data.ClientModel;
import com.elfmcys.yesstevemodel.client.data.ClientModelData;
import com.elfmcys.yesstevemodel.info.ModelAuthor;
import com.elfmcys.yesstevemodel.info.ModelStats;
import com.elfmcys.yesstevemodel.info.stats.GeoModelStats;
import com.elfmcys.yesstevemodel.info.stats.ModelTextureStats;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 不使用原版的语言文件管理机制，而是直接写一个自己的实现
 */
public class LanguageManager {
    public static final String DEFAULT_LANGUAGE_CODE = "en_us";

    public static String getI18n(ClientModel data, String key, String defaultValue) {
        String local = Minecraft.getInstance().getLanguageManager().getSelected();
        Map<String, Map<String, String>> languages = data.languages();
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

    public static String getI18n(ClientModelData data, String local, String key, String defaultValue) {
        Map<String, Map<String, String>> languages = data.languages();
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

    public static Map<String, List<Component>> buildAllDisplayInfos(ClientModelData data) {
        Map<String, List<Component>> displayInfos = Maps.newHashMap();
        data.languages().keySet().forEach(language -> displayInfos.put(language, buildDisplayInfo(data, language)));
        if (!displayInfos.containsKey(DEFAULT_LANGUAGE_CODE)) {
            displayInfos.put(DEFAULT_LANGUAGE_CODE, buildDisplayInfo(data, DEFAULT_LANGUAGE_CODE));
        }
        return displayInfos;
    }

    private static List<Component> buildDisplayInfo(ClientModelData data, String local) {
        List<Component> component = Lists.newArrayList();
        var metadata = data.info().metadata();

        if (metadata != null) {
            // 名称
            String name = LanguageManager.getI18n(data, local, "metadata.name", metadata.name());
            if (StringUtils.isBlank(name)) {
                return component;
            }
            component.add(Component.literal(name).withStyle(ChatFormatting.GOLD));

            // 描述
            String tips = LanguageManager.getI18n(data, local, "metadata.tips", metadata.tips());
            if (StringUtils.isNoneBlank(tips)) {
                String[] split = tips.replace("\r", "").split("\n");
                Arrays.stream(split).forEach(s -> component.add(Component.literal(s).withStyle(ChatFormatting.GRAY)));
            }

            // 作者和许可证
            if (!metadata.authors().isEmpty() || StringUtils.isNoneBlank(metadata.license().type())) {
                component.add(CommonComponents.space());
            }

            // 作者
            if (!metadata.authors().isEmpty()) {
                int[] index = new int[]{-1};
                String[] authorNames = metadata.authors().stream()
                        .map(getModelAuthorStringFunction(data, local, index))
                        .toArray(String[]::new);
                String joinedAuthors = StringUtils.join(authorNames, "丨");
                component.add(Component.translatable("gui.yes_steve_model.model.authors",
                        Component.literal(joinedAuthors).withStyle(ChatFormatting.DARK_GRAY)));
            }

            // 许可证，不需要语言文件
            if (StringUtils.isNoneBlank(metadata.license().type())) {
                component.add(Component.translatable("gui.yes_steve_model.model.license",
                        Component.literal(metadata.license().type()).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        ModelStats stats = data.info().stats();
        if (stats != null) {
            GeoModelStats modelStats = stats.playerModel();
            Map<String, ModelTextureStats> textures = stats.textures();

            component.add(CommonComponents.space());
            component.add(Component.translatable("gui.yes_steve_model.model.main_model_info", modelStats.bones(), modelStats.cubes(), modelStats.faces())
                    .withStyle(ChatFormatting.GRAY));
            component.add(Component.translatable("gui.yes_steve_model.model.texture_info", textures.size())
                    .withStyle(ChatFormatting.GRAY));
        }

        return component;
    }

    @NotNull
    private static Function<ModelAuthor, String> getModelAuthorStringFunction(ClientModelData data, String local, int[] index) {
        return author -> {
            index[0]++;
            String authorName = getI18n(data, local, "metadata.authors.%d.name".formatted(index[0]), author.name());
            if (author.role().isEmpty()) {
                return authorName;
            }
            String authorRole = getI18n(data, local, "metadata.authors.%d.role".formatted(index[0]), author.role());
            return authorRole + ": " + authorName;
        };
    }
}
