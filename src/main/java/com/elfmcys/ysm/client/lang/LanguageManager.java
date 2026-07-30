package com.elfmcys.ysm.client.lang;

import com.elfmcys.ysm.client.model.ClientModel;
import com.elfmcys.ysm.client.model.ModelPackInfo;
import com.elfmcys.ysm.info.ModelAuthor;
import com.elfmcys.ysm.info.stats.PlayerMainModelStats;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    public static String getI18n(ClientModel data, String key, String defaultValue) {
        String local = Minecraft.getInstance().getLanguageManager().getSelected();
        return getI18n(data, local, key, defaultValue);
    }

    public static String getI18n(ClientModel data, String local, String key, String defaultValue) {
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

    public static List<Component> buildDisplayInfo(ClientModel model, String locale, String modelPath, boolean detailed) {
        List<Component> component = Lists.newArrayList();
        var metadata = model.info().metadata();

        if (metadata != null) {
            // 名称
            String name = LanguageManager.getI18n(model, locale, "metadata.name", metadata.name());
            if (StringUtils.isNoneBlank(name)) {
                component.add(Component.literal(name).withStyle(ChatFormatting.GOLD));
            }

            // 描述
            String tips = LanguageManager.getI18n(model, locale, "metadata.tips", metadata.tips());
            if (StringUtils.isNoneBlank(tips)) {
                String[] split = tips.replace("\r", "").split("\n");
                Arrays.stream(split).forEach(s -> component.add(Component.literal(s).withStyle(ChatFormatting.GRAY)));
            }

            if (!metadata.authors().isEmpty() || StringUtils.isNoneBlank(metadata.license().type())) {
                component.add(CommonComponents.space());
            }

            // 作者
            if (!metadata.authors().isEmpty()) {
                int[] index = new int[]{-1};
                String[] authorNames = metadata.authors().stream()
                        .map(getModelAuthorStringFunction(model, locale, index))
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

        // 额外信息
        if (detailed) {
            component.add(Component.translatable("gui.yes_steve_model.model.file",
                    Component.literal(modelPath).withStyle(ChatFormatting.DARK_GRAY)));
            component.add(Component.translatable("gui.yes_steve_model.model.hash",
                    Component.literal(model.info().hash()).withStyle(ChatFormatting.DARK_GRAY)));
            if (StringUtils.isNoneBlank(model.info().extra())) {
                component.add(Component.translatable("gui.yes_steve_model.model.extra",
                        Component.literal(model.info().extra()).withStyle(ChatFormatting.DARK_GRAY)));
            }
            if (model.info().timestamp() != 0) {
                Instant instant = Instant.ofEpochMilli(model.info().timestamp() * 1000);
                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                component.add(Component.translatable("gui.yes_steve_model.model.timestamp",
                        Component.literal(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).withStyle(ChatFormatting.DARK_GRAY)));
            }
            if (StringUtils.isNoneBlank(model.info().rnd())) {
                component.add(Component.translatable("gui.yes_steve_model.model.rand",
                        Component.literal(model.info().rnd()).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        PlayerMainModelStats stats = model.info().stats();
        if (stats != null) {
            component.add(CommonComponents.space());
            component.add(Component.translatable("gui.yes_steve_model.model.main_model_info", stats.bones(), stats.cubes(), stats.faces())
                    .withStyle(ChatFormatting.GRAY));
            component.add(Component.translatable("gui.yes_steve_model.model.texture_info", model.playerModel().textures().size())
                    .withStyle(ChatFormatting.GRAY));
        }

        return component;
    }

    @NotNull
    private static Function<ModelAuthor, String> getModelAuthorStringFunction(ClientModel data, String local, int[] index) {
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
