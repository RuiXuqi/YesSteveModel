package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.info.ModelInfo;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClientModel {
    private final GeoModel mainModel;
    private final GeoModel armModel;
    @Nullable
    private final GeoModel arrowModel;

    private final Map<String, Animation> mainAnimations;
    @NotNull
    private final Map<String, Animation> arrowAnimations;

    private final FifoHashMap<String, ResourceLocation> textures;
    @Nullable
    private final ResourceLocation arrowTexture;

    private final ModelInfo modelInfo;

    private final ClientModelInfo clientModelInfo;

    public ClientModel(GeoModel mainModel, GeoModel armModel, @Nullable GeoModel arrowModel, Map<String, Animation> mainAnimations, @NotNull Map<String, Animation> arrowAnimations, FifoHashMap<String, ResourceLocation> textures, @Nullable ResourceLocation arrowTexture, ModelInfo modelInfo, ClientModelInfo clientModelInfo) {
        this.mainModel = mainModel;
        this.armModel = armModel;
        this.arrowModel = arrowModel;
        this.mainAnimations = mainAnimations;
        this.arrowAnimations = arrowAnimations;
        this.textures = textures;
        this.arrowTexture = arrowTexture;
        this.modelInfo = modelInfo;
        this.clientModelInfo = clientModelInfo;
    }

    public GeoModel mainModel() {
        return mainModel;
    }

    public GeoModel armModel() {
        return armModel;
    }

    @Nullable
    public GeoModel arrowModel() {
        return arrowModel;
    }

    public Map<String, Animation> mainAnimations() {
        return mainAnimations;
    }

    @NotNull
    public Map<String, Animation> arrowAnimations() {
        return arrowAnimations;
    }

    public FifoHashMap<String, ResourceLocation> textures() {
        return textures;
    }

    @Nullable
    public ResourceLocation arrowTexture() {
        return arrowTexture;
    }

    public ModelInfo modelInfo() {
        return modelInfo;
    }

    public ClientModelInfo clientModelInfo() {
        return clientModelInfo;
    }

    public String defaultTextureName() {
        if (textures.containsKey(modelInfo.properties().defaultTexture())) {
            return modelInfo.properties().defaultTexture();
        } else {
            return textures.getKeyAt(0);
        }
    }
}
