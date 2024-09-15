package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.info.ModelStats;
import com.elfmcys.yesstevemodel.info.stats.GeoModelStats;
import com.elfmcys.yesstevemodel.info.stats.ModelTextureStats;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
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
        var animations = buildAnimationMap(data, isDefault);
        var animationControllers = buildAnimationControllerMap(data);
        var textures = buildTextureMap(data, isDefault);
        var authorAvatars = buildAuthorAvatarMap(data);

        var projectileModels = buildProjectileModels(data, isDefault);

        var displayInfo = buildDisplayInfo(data);
        var info = new ClientModelInfo(displayInfo, isNeedAuth, authorAvatars);

        var conditionManager = buildConditionManager(animations);

        var model = new ClientModel(mainModel, armModel, animations, animationControllers, textures, projectileModels, data.info(), info, conditionManager);
        if (isDefault) {
            DEFAULT_MODEL = model;
        }
        return model;
    }

    private static Map<String, Animation> buildAnimationMap(ClientModelData data, boolean isDefaultModel) {
        Object2ReferenceOpenHashMap<String, Animation> map = new Object2ReferenceOpenHashMap<>();
        if (!isDefaultModel) {
            map.putAll(DEFAULT_MODEL.animations());
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

    private static Map<String, GeoAnimationController> buildAnimationControllerMap(ClientModelData data) {
        Object2ReferenceOpenHashMap<String, GeoAnimationController> map = new Object2ReferenceOpenHashMap<>();
        for (var i = 0; i < data.animationControllerFiles().size(); i++) {
            var file = data.animationControllerFiles().get(i);
            if (file != null) {
                map.putAll(file.animationControllers());
            }
        }
        return Object2ReferenceMaps.unmodifiable(map);
    }

    private static ConditionManager buildConditionManager(Map<String, Animation> animations) {
        ConditionManager conditionManager = new ConditionManager();
        animations.keySet().forEach(conditionManager::addTest);
        return conditionManager;
    }

    private static Map<ProjectileType, ProjectileModel> buildProjectileModels(ClientModelData data, boolean isDefaultModel) {
        Object2ReferenceOpenHashMap<ProjectileType, ProjectileModel> map = new Object2ReferenceOpenHashMap<>();

        var arrowModel = buildArrowModel(data, isDefaultModel);
        if (arrowModel != null) {
            map.put(ProjectileType.ARROW, arrowModel);
        }

        return Object2ReferenceMaps.unmodifiable(map);
    }

    @Nullable
    private static ProjectileModel buildArrowModel(ClientModelData data, boolean isDefaultModel) {
        var model = data.geoModels().get(MODEL_ARROW_INDEX);
        if (model == null) {
            return null;
        }

        Object2ReferenceOpenHashMap<String, Animation> animationMap = new Object2ReferenceOpenHashMap<>();
        if (!isDefaultModel) {
            animationMap.putAll(DEFAULT_MODEL.projectileModels().get(ProjectileType.ARROW).animations());
        }
        var animationFile = data.animationFiles().get(ANIMATION_ARROW_INDEX);
        if (animationFile != null) {
            animationMap.putAll(animationFile.animations());
        }

        var texture = ModelIdUtil.getArrowTextureId(data.info().hash());

        return new ProjectileModel(model, Object2ReferenceMaps.unmodifiable(animationMap), texture);
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

    public static Map<String, ResourceLocation> buildAuthorAvatarMap(ClientModelData data) {
        Object2ObjectOpenHashMap<String, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        if (data.info().metadata() != null) {
            int counter = 0;
            for (var author : data.info().metadata().authors()) {
                if (data.authorAvatars().containsKey(author.name())) {
                    map.put(author.name(), new ResourceLocation(YesSteveModel.MOD_ID, data.info().hash() + "/author/" + counter++));
                }
            }
        }
        return Object2ObjectMaps.unmodifiable(map);
    }

    private static List<Component> buildDisplayInfo(ClientModelData data) {
        List<Component> component = Lists.newArrayList();
        var extraInfo = data.info().metadata();
        if (extraInfo != null) {
            if (!StringUtils.isBlank(extraInfo.name())) {
                component.add(Component.literal(extraInfo.name()).withStyle(ChatFormatting.GOLD));
                if (StringUtils.isNoneBlank(extraInfo.tips())) {
                    String[] split = extraInfo.tips().replace("\r", "").split("\n");
                    Arrays.stream(split).forEach(s -> component.add(Component.literal(s).withStyle(ChatFormatting.GRAY)));
                }
                if (!extraInfo.authors().isEmpty() || StringUtils.isNoneBlank(extraInfo.license().type())) {
                    component.add(CommonComponents.space());
                }
                if (!extraInfo.authors().isEmpty()) {
                    component.add(Component.translatable("gui.yes_steve_model.model.authors", StringUtils.join(
                            extraInfo.authors().stream().map(author -> author.role().isEmpty() ? author.name() : (author.role() + ": " + author.name())).toArray(String[]::new), "丨")));
                }
                if (StringUtils.isNoneBlank(extraInfo.license().type())) {
                    component.add(Component.translatable("gui.yes_steve_model.model.license", extraInfo.license().type()));
                }
            }
        }

        ModelStats stats = data.info().stats();
        if (stats != null) {
            GeoModelStats modelStats = stats.playerModel();
            Map<String, ModelTextureStats> textures = stats.textures();

            component.add(CommonComponents.space());
            component.add(Component.translatable("gui.yes_steve_model.model.main_model_info", modelStats.bones(), modelStats.cubes(), modelStats.faces()));
            component.add(Component.translatable("gui.yes_steve_model.model.texture_info", textures.size()));
        }

        return component;
    }

    @Nullable
    public static ClientModel getDefaultModel() {
        return DEFAULT_MODEL;
    }
}
