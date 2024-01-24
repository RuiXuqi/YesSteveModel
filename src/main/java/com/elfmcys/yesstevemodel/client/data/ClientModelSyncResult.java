package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.message.StringFormattedMessage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Native Access: 所有方法都在 c++ 线程池上调用
public class ClientModelSyncResult {
    public boolean success;
    @Nullable public Component message;

    public Map<ResourceLocation, ClientModelInfo> modelInfo = Maps.newHashMap();
    public Map<ResourceLocation, GeoModel> geoModels = Maps.newHashMap();
    public Map<ResourceLocation, AnimationFile> animations = Maps.newHashMap();
    public Set<String> authModels = Sets.newHashSet();
    public ConditionManager conditionManager = new ConditionManager();
    public AnimationFile defaultMainAnimationFile;
    public AnimationFile defaultArrowAnimationFile;
    public GeoModel defaultMainModel;

    private final List<ResourceLocation> newTextureIds = Lists.newArrayList();
    private final List<ResourceLocation> removedTextures = Lists.newArrayList();
    private final Map<ResourceLocation, NativeTexture> duplicatedTextures = Maps.newHashMap();

    // Native Access
    public ClientModelSyncResult() {

    }

    // Native Access: 同步结束后，commit 之前调用
    @SuppressWarnings("unused")
    public void setResult(boolean success, @Nullable Object message) {
        this.success = true;
        this.message = (Component) message;
    }

    // Native Access
    @SuppressWarnings("unused")
    public void removeModel(final ClientModel model, boolean delayed) {
        final ResourceLocation modelId = ModelIdUtil.getModelId(model.name());
        if (delayed) {
            removedTextures.addAll(model.textures().keySet().stream().map(name -> ModelIdUtil.getTextureId(modelId, name)).toList());
        } else {
            Minecraft.getInstance().execute(() -> {
               for(String textureName : model.textures().keySet()) {
                   ResourceLocation textureId = ModelIdUtil.getTextureId(modelId, textureName);
                   Minecraft.getInstance().getTextureManager().release(textureId);
               }
            });
        }
    }

    // Native Access
    // Default 模型必须第一个传入
    @SuppressWarnings("unused")
    public void registerModel(ClientModel model, boolean isNew) {
        ClientModelProcessor proc = new ClientModelProcessor(model);
        try {
            proc.process();
        } catch (Exception e) {
            YesSteveModel.LOGGER.error(new StringFormattedMessage("Failed to process {}", model.name()), e);
        }

        if (model.isNeedAuth()) {
            authModels.add(model.name());
        }
        if (model.isDefault()) {
            defaultMainAnimationFile = proc.animationFiles().get(ModelIdUtil.DEFAULT_MAIN_MODEL_ID);
            defaultArrowAnimationFile = proc.animationFiles().get(ModelIdUtil.DEFAULT_ARROW_MODEL_ID);
            defaultMainModel = proc.geoModels().get(proc.mainId());
        } else {
            AnimationFile mainAnimation = proc.animationFiles().get(proc.mainId());
            defaultMainAnimationFile.getAnimations().forEach((key, value) -> {
                mainAnimation.getAnimations().putIfAbsent(key, value);
            });
            if (proc.geoModels().containsKey(proc.arrowId())) {
                AnimationFile arrowAnimation = proc.animationFiles().get(proc.arrowId());
                defaultArrowAnimationFile.getAnimations().forEach((key, value) -> {
                    arrowAnimation.getAnimations().putIfAbsent(key, value);
                });
            }
        }

        registerGeoModel(proc.geoModels());
        registerAnimations(proc.mainId(), proc.animationFiles());
        registerTexture(proc.textures(), isNew);
        registerModelInfo(proc.id(), proc.mainModelInfo());
    }

    private void registerGeoModel(Map<ResourceLocation, GeoModel> modelMap) {
        geoModels.putAll(modelMap);
    }

    private void registerAnimations(ResourceLocation mainId, Map<ResourceLocation, AnimationFile> animationFileMap) {
        animationFileMap.get(mainId).getAnimations().forEach((name, animation) -> conditionManager.addTest(mainId, name));
        animations.putAll(animationFileMap);
    }

    // 统一注册可能导致严重掉帧，所以提前在这里注册
    @SuppressWarnings("ConstantValue")
    private void registerTexture(Map<ResourceLocation, NativeTexture> textures, boolean isNew) {
        if (!isNew) {
            return;
        }
        Minecraft.getInstance().execute(() -> {
            for (final Map.Entry<ResourceLocation, NativeTexture> entry : textures.entrySet()) {
                if(Minecraft.getInstance().getTextureManager().getTexture(entry.getKey(), null) == null) {
                    Minecraft.getInstance().getTextureManager().register(entry.getKey(), entry.getValue());
                } else {
                    duplicatedTextures.put(entry.getKey(), entry.getValue());
                }
            }
        });
        newTextureIds.addAll(textures.keySet());
    }

    private void registerModelInfo(ResourceLocation modelId, ClientModelInfo modelInfo) {
        this.modelInfo.put(modelId, modelInfo);
    }

    // Native Access: 同步结束后，commit 之前调用
    @SuppressWarnings("unused")
    public void freeze() {
        modelInfo = ImmutableMap.copyOf(modelInfo);
        geoModels = ImmutableMap.copyOf(geoModels);
        animations = ImmutableMap.copyOf(animations);
        authModels = ImmutableSet.copyOf(authModels);
    }

    public void releaseRemovedTextures() {
        for (ResourceLocation textureId : removedTextures) {
            Minecraft.getInstance().getTextureManager().release(textureId);
        }
        removedTextures.clear();
    }

    public void replaceDuplicatedTextures() {
        for (Map.Entry<ResourceLocation, NativeTexture> entry : duplicatedTextures.entrySet()) {
            Minecraft.getInstance().getTextureManager().release(entry.getKey());
            Minecraft.getInstance().getTextureManager().register(entry.getKey(), entry.getValue());
        }

        duplicatedTextures.clear();
    }

    // Native Access: 同步取消后调用
    @SuppressWarnings("unused")
    public void rollback() {
        Minecraft.getInstance().execute(() -> {
            for (ResourceLocation textureId : newTextureIds) {
                Minecraft.getInstance().getTextureManager().release(textureId);
            }
            newTextureIds.clear();
        });
    }
}
