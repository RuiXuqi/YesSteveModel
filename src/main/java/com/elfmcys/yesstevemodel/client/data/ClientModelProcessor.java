package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.raw.pojo.ExtraInfo;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ClientModelProcessor {
    private final ResourceLocation id;
    private final ResourceLocation mainModelId;
    private final ResourceLocation arrowModelId;
    private final ClientModel model;

    private ClientModelInfo mainModelInfo;
    private Map<ResourceLocation, GeoModel> geoModels;
    private Map<ResourceLocation, NativeTexture> textures;
    private Map<ResourceLocation, AnimationFile> animationFiles;

    public ClientModelProcessor(ClientModel model) {
        this.id = ModelIdUtil.getModelId(model.name());
        this.mainModelId = ModelIdUtil.getMainId(id);
        this.arrowModelId = ModelIdUtil.getArrowId(id);
        this.model = model;
    }

    public ResourceLocation id() {
        return id;
    }

    public ResourceLocation mainId() {
        return mainModelId;
    }

    public ResourceLocation arrowId() {
        return arrowModelId;
    }

    public ClientModel model() {
        return model;
    }

    public ClientModelInfo mainModelInfo() {
        return mainModelInfo;
    }

    public Map<ResourceLocation, GeoModel> geoModels() {
        return geoModels;
    }

    public Map<ResourceLocation, NativeTexture> textures() {
        return textures;
    }

    public Map<ResourceLocation, AnimationFile> animationFiles() {
        return animationFiles;
    }

    public void process() {
        processTextures();
        processGeoModels();
        processAnimations();
    }

    public void processGeoModels() {
        geoModels = new HashMap<>();
        for (Map.Entry<String, GeoModel> entry : model.geoModels().entrySet()) {
            ResourceLocation geoModelId = ModelIdUtil.getSubModelId(id, entry.getKey());
            if(entry.getKey().equals("main")) {
                Pair<List<Component>, List<String>> extraInfo = handleExtraInfo(entry.getValue().properties.getExtraInfo());
                mainModelInfo = new ClientModelInfo(Lists.newArrayList(
                        model.textures().keySet().stream()
                                .filter(name -> !name.equals(ModelIdUtil.ARROW_TEXTURE_NAME))
                                .map(name -> ModelIdUtil.getTextureId(id, name))
                                .collect(Collectors.toList())),
                        model.hash(),
                        model.features(),
                        entry.getValue().properties.getWidthScale(),
                        entry.getValue().properties.getHeightScale(),
                        extraInfo.getLeft(),
                        extraInfo.getRight(),
                        entry.getValue().properties.getExtraInfo() != null && entry.getValue().properties.getExtraInfo().isFree());
            }
            geoModels.put(geoModelId, entry.getValue());
        }
    }

    public void processTextures() {
        textures = new HashMap<>();
        for (Map.Entry<String, NativeTexture> entry : model.textures().entrySet()) {
            textures.put(ModelIdUtil.getTextureId(id, entry.getKey()), entry.getValue());
        }
    }

    private void processAnimations() {
        animationFiles = new HashMap<>();
        AnimationFile main = new AnimationFile();
        model.animationFiles().forEach((name, file) -> {
            if(name.equals(ModelIdUtil.ARROW_MODEL_NAME)) {
                animationFiles.put(arrowModelId, file);
                return;
            }
            mergeAnimationFile(main, file);
        });
        animationFiles.put(mainModelId, main);
        if(geoModels.containsKey(arrowModelId)) {
            animationFiles.computeIfAbsent(arrowModelId, k -> new AnimationFile());
        }
    }

    private static void mergeAnimationFile(AnimationFile main, AnimationFile other) {
        other.getAnimations().forEach(main::putAnimation);
    }

    @Nonnull
    private static Pair<List<Component>, List<String>> handleExtraInfo(@Nullable ExtraInfo extraInfo) {
        if (extraInfo == null) {
            return Pair.of(Collections.emptyList(), Collections.emptyList());
        }

        List<Component> component = Lists.newArrayList();
        List<String> extraAnimationNames;

        if(!StringUtils.isBlank(extraInfo.getName())) {
            component.add(Component.literal(extraInfo.getName()).withStyle(ChatFormatting.GOLD));
            if (StringUtils.isNoneBlank(extraInfo.getTips())) {
                String[] split = extraInfo.getTips().split("\n");
                Arrays.stream(split).forEach(s -> component.add(Component.literal(s).withStyle(ChatFormatting.GRAY)));
            }
            if (extraInfo.getAuthors() != null && extraInfo.getAuthors().length != 0) {
                component.add(Component.translatable("gui.yes_steve_model.model.authors", StringUtils.join(extraInfo.getAuthors(), "丨")));
            }
            if (StringUtils.isNoneBlank(extraInfo.getLicense())) {
                component.add(Component.translatable("gui.yes_steve_model.model.license", extraInfo.getLicense()));
            }
        }

        if(extraInfo.getExtraAnimationNames() != null) {
            extraAnimationNames = Arrays.asList(extraInfo.getExtraAnimationNames());
        } else {
            extraAnimationNames = Collections.emptyList();
        }

        return Pair.of(component, extraAnimationNames);
    }
}
