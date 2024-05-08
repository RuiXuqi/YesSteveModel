package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationControllerFile;
import com.elfmcys.yesstevemodel.info.ModelInfo;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

// Native Access
public final class ClientModelData {
    private final List<GeoModel> geoModels;
    private final List<AnimationFile> animationFiles;
    private final List<AnimationControllerFile> animationControllerFiles;
    private final FifoHashMap<String, NativeTexture> textures;
    private final Map<String, NativeTexture> authorAvatars;
    @NotNull
    private final ModelInfo info;

    // Native Access
    public ClientModelData(GeoModel[] geoModels, AnimationFile[] animationFiles, AnimationControllerFile[] animationControllerFiles, FifoHashMap<String, NativeTexture> textures, Map<String, NativeTexture> authorAvatars, @NotNull ModelInfo info) {
        this.geoModels = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(geoModels));
        this.animationFiles = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(animationFiles));
        this.animationControllerFiles = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(animationControllerFiles));
        this.textures = textures;
        this.authorAvatars = Object2ObjectMaps.unmodifiable(new Object2ObjectOpenHashMap<>(authorAvatars));
        this.info = info;
    }

    public List<GeoModel> geoModels() {
        return geoModels;
    }

    public List<AnimationFile> animationFiles() {
        return animationFiles;
    }

    public List<AnimationControllerFile> animationControllerFiles() {
        return animationControllerFiles;
    }

    public FifoHashMap<String, NativeTexture> textures() {
        return textures;
    }

    public Map<String, NativeTexture> authorAvatars() {
        return authorAvatars;
    }

    @NotNull
    public ModelInfo info() {
        return info;
    }
}
