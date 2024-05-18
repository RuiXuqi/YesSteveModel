package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClientModelBuilder {
    private static final int MODEL_MAIN_INDEX = 0;
    private static final int MODEL_ARM_INDEX = 1;
    private static final int MODEL_ARROW_INDEX = 2;

    private static final int ANIMATION_MAIN_INDEX = 0;
    private static final int ANIMATION_ARM_INDEX = 1;
    private static final int ANIMATION_EXTRA_INDEX = 2;
    private static final int ANIMATION_TAC_INDEX = 3;
    private static final int ANIMATION_ARROW_INDEX = 4;
    private static final int ANIMATION_CARRYON_INDEX = 5;

    private static ClientModel DEFAULT_MODEL;

    public static ClientModel build(ClientModelData data, boolean isDefault, boolean isNeedAuth) {
        GeoModel mainModel = data.geoModels().get(MODEL_MAIN_INDEX);
        GeoModel armModel = data.geoModels().get(MODEL_ARM_INDEX);
        GeoModel arrowModel = data.geoModels().get(MODEL_ARROW_INDEX);

        var mainAnimations = buildMainAnimationMap(data, isDefault);
        var arrowAnimations = buildArrowAnimationMap(data, isDefault);

        var textures = buildTextureMap(data, isDefault);
        var arrowTexture = getArrowTextureId(data, isDefault);

        var displayInfo = buildDisplayInfo(data);

        var info = new ClientModelInfo(displayInfo, isNeedAuth);
        var model = new ClientModel(mainModel, armModel, arrowModel, mainAnimations, arrowAnimations, textures, arrowTexture, data.info(), info);
        if (isDefault) {
            DEFAULT_MODEL = model;
        }
        return model;
    }

    private static Map<String, Animation> buildMainAnimationMap(ClientModelData data, boolean isDefaultModel) {
        Object2ReferenceOpenHashMap<String, Animation> map = new Object2ReferenceOpenHashMap<>();
        if (!isDefaultModel) {
            map.putAll(DEFAULT_MODEL.mainAnimations());
        }
        for (var i = 0; i < data.animationFiles().size(); i++) {
            if (i == ANIMATION_ARROW_INDEX) {
                continue;
            }
            var file = data.animationFiles().get(i);
            if (file != null) {
                map.putAll(file.animations());
            }
        }
        return Object2ReferenceMaps.unmodifiable(map);
    }

    private static Map<String, Animation> buildArrowAnimationMap(ClientModelData data, boolean isDefaultModel) {
        Object2ReferenceOpenHashMap<String, Animation> map = new Object2ReferenceOpenHashMap<>();
        if (!isDefaultModel) {
            map.putAll(DEFAULT_MODEL.arrowAnimations());
        }
        var file = data.animationFiles().get(ANIMATION_ARROW_INDEX);
        if (file != null) {
            map.putAll(file.animations());
        }
        return Object2ReferenceMaps.unmodifiable(map);
    }

    public static FifoHashMap<String, ResourceLocation> buildTextureMap(ClientModelData data, boolean isDefault) {
        Object2ObjectArrayMap<String, ResourceLocation> map = new Object2ObjectArrayMap<>();
        int counter = 0;
        for (var entry : data.textures().entrySet()) {
            if (entry.getKey().equals(ModelIdUtil.ARROW_TEXTURE_NAME_PLACEHOLDER)) {
                continue;
            }
            var id = new ResourceLocation(YesSteveModel.MOD_ID, (isDefault ? "default" : data.info().hash()) + "/" + counter++);
            map.put(entry.getKey(), id);
        }
        return new FifoHashMap<>(map);
    }

    @Nullable
    public static ResourceLocation getArrowTextureId(ClientModelData data, boolean isDefault) {
        return data.textures().containsKey(ModelIdUtil.ARROW_TEXTURE_NAME_PLACEHOLDER) ? ModelIdUtil.getTextureId(isDefault ? "default" : data.info().hash()) : null;
    }

    private static List<Component> buildDisplayInfo(ClientModelData data) {
        List<Component> component = Lists.newArrayList();
        var extraInfo = data.info().metadata();
        if (extraInfo != null) {
            if(!StringUtils.isBlank(extraInfo.name())) {
                component.add(Component.literal(extraInfo.name()).withStyle(ChatFormatting.GOLD));
                if (StringUtils.isNoneBlank(extraInfo.tips())) {
                    String[] split = extraInfo.tips().split("\n");
                    Arrays.stream(split).forEach(s -> component.add(Component.literal(s).withStyle(ChatFormatting.GRAY)));
                }
                if (!extraInfo.authors().isEmpty()) {
                    component.add(Component.translatable("gui.yes_steve_model.model.authors", StringUtils.join(
                            extraInfo.authors().stream().map(author -> author.role().isEmpty() ? author.name() : (author.role() + ":" + author.name())).toArray(String[]::new), "丨")));
                }
                if (StringUtils.isNoneBlank(extraInfo.license().type())) {
                    component.add(Component.translatable("gui.yes_steve_model.model.license", extraInfo.license().type()));
                }
            }
        }
        return component;
    }

    @Nullable
    public static ClientModel getDefaultModel() {
        return DEFAULT_MODEL;
    }
}
