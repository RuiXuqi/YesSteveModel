package com.elfmcys.yesstevemodel.client.model.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationControllerFile;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

// Native Access
public class PlayerModelData {
    private final List<GeoModel> geoModels;
    private final List<AnimationFile> animationFiles;
    private final List<AnimationControllerFile> animationControllerFiles;
    private final FifoHashMap<String, NativeTexture> textures;

    // Native Access
    public PlayerModelData(GeoModel[] geoModels, AnimationFile[] animationFiles, AnimationControllerFile[] animationControllerFiles, FifoHashMap<String, NativeTexture> textures) {
        this.geoModels = ObjectArrayList.wrap(geoModels);
        this.animationFiles = ObjectArrayList.wrap(animationFiles);
        this.animationControllerFiles = ObjectArrayList.wrap(animationControllerFiles);
        this.textures = textures;
    }

    public List<AnimationFile> animationFiles() {
        return animationFiles;
    }

    public List<GeoModel> geoModels() {
        return geoModels;
    }

    public List<AnimationControllerFile> animationControllerFiles() {
        return animationControllerFiles;
    }

    public FifoHashMap<String, NativeTexture> textures() {
        return textures;
    }
}
