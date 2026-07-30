package com.elfmcys.ysm.client.model.data;

import com.elfmcys.ysm.client.texture.NativeTexture;
import com.elfmcys.ysm.geckolib3.file.AnimationControllerFile;
import com.elfmcys.ysm.geckolib3.file.AnimationFile;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.util.FifoHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;

// Native Access
public class PlayerModelData {
    private final List<GeoModel> geoModels;
    private final Map<String, AnimationFile> animationFiles;
    private final List<AnimationControllerFile> animationControllerFiles;
    private final FifoHashMap<String, NativeTexture> textures;

    // Native Access
    public PlayerModelData(GeoModel[] geoModels, Map<String, AnimationFile> animationFiles, AnimationControllerFile[] animationControllerFiles, FifoHashMap<String, NativeTexture> textures) {
        this.geoModels = ObjectArrayList.wrap(geoModels);
        this.animationFiles = animationFiles;
        this.animationControllerFiles = ObjectArrayList.wrap(animationControllerFiles);
        this.textures = textures;
    }

    public Map<String, AnimationFile> animationFiles() {
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
