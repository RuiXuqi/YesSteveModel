package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.sound.SoundData;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.GeoAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.info.ModelInfo;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class ClientModel {
    private final GeoModel mainModel;

    private final GeoModel armModel;

    private final Map<String, Animation> animations;

    private final Map<String, GeoAnimationController> animationControllers;

    private final FifoHashMap<String, ResourceLocation> textures;

    private final Map<ProjectileType, ProjectileModel> projectileModels;

    private final Map<String, SoundData> sounds;

    private final Int2ReferenceOpenHashMap<IValue> functions;

    private final Int2ReferenceOpenHashMap<List<IValue>> eventHandlers;

    private final ModelInfo modelInfo;

    private final ClientModelInfo clientModelInfo;

    private final ConditionManager conditionManager;

    public ClientModel(GeoModel mainModel, GeoModel armModel, Map<String, Animation> animations, Map<String, GeoAnimationController> animationControllers, FifoHashMap<String, ResourceLocation> textures, Map<String, SoundData> sounds, Map<ProjectileType, ProjectileModel> projectileModels, Int2ReferenceOpenHashMap<IValue> functions, Int2ReferenceOpenHashMap<List<IValue>> eventHandlers, ModelInfo modelInfo, ClientModelInfo clientModelInfo, ConditionManager conditionManager) {
        this.mainModel = mainModel;
        this.armModel = armModel;
        this.animations = animations;
        this.animationControllers = animationControllers;
        this.textures = textures;
        this.sounds = sounds;
        this.projectileModels = projectileModels;
        this.functions = functions;
        this.eventHandlers = eventHandlers;
        this.modelInfo = modelInfo;
        this.clientModelInfo = clientModelInfo;
        this.conditionManager = conditionManager;
    }

    public GeoModel mainModel() {
        return mainModel;
    }

    public GeoModel armModel() {
        return armModel;
    }

    public Map<String, Animation> animations() {
        return animations;
    }

    public Map<String, GeoAnimationController> animationControllers() {
        return animationControllers;
    }

    public FifoHashMap<String, ResourceLocation> textures() {
        return textures;
    }

    public Map<String, SoundData> sounds() {
        return sounds;
    }

    public Int2ReferenceOpenHashMap<IValue> functions() {
        return functions;
    }

    public Int2ReferenceOpenHashMap<List<IValue>> eventHandlers() {
        return eventHandlers;
    }

    public Map<ProjectileType, ProjectileModel> projectileModels() {
        return projectileModels;
    }

    public ModelInfo modelInfo() {
        return modelInfo;
    }

    public ClientModelInfo clientModelInfo() {
        return clientModelInfo;
    }

    public ConditionManager conditionManager() {
        return conditionManager;
    }

    public String defaultTextureName() {
        if (textures.containsKey(modelInfo.properties().defaultTexture())) {
            return modelInfo.properties().defaultTexture();
        } else {
            return textures.getKeyAt(0);
        }
    }
}
