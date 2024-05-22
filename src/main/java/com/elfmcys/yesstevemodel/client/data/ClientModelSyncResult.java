package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.message.StringFormattedMessage;

import org.jetbrains.annotations.Nullable;
import java.util.List;

// Native Access: 所有方法都在 c++ 线程池上调用
public class ClientModelSyncResult {
    public boolean success;
    @Nullable public Component message;

    public Object2ReferenceMap<String, ClientModel> models = new Object2ReferenceOpenHashMap<>();
    public ClientModel defaultModel;
    public ConditionManager conditionManager = new ConditionManager();

    private final List<ResourceLocation> newTextureIds = Lists.newArrayList();
    private final List<ResourceLocation> removedTextures = Lists.newArrayList();

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
    public void removeModel(final ClientModelData data) {
        var textureMap = ClientModelBuilder.buildTextureMap(data, false);
        removedTextures.addAll(textureMap.values());
    }

    // Native Access
    // Default 模型在首次同步时最先传入
    @SuppressWarnings("unused")
    public void registerModel(String modelId, ClientModelData data, boolean isDefault, boolean isNeedAuth, boolean isNew) {
        ClientModel model;
        try {
            model = ClientModelBuilder.build(data, isDefault, isNeedAuth);
        } catch (Exception e) {
            YesSteveModel.LOGGER.error(new StringFormattedMessage("Failed to process {}", modelId), e);
            return;
        }

        model.mainAnimations().keySet().forEach(name -> conditionManager.addTest(modelId, name));
        if (isNew) {
            registerTextures(data, model);
        }

        models.put(modelId, model);
        if (isDefault) {
            defaultModel = model;
        }
    }

    // 集中注册可能导致严重掉帧，所以提前到这里分散注册
    private void registerTextures(ClientModelData data, ClientModel model) {
        Minecraft.getInstance().execute(() -> {
            for (final var entry : model.textures().entrySet()) {
                if (Minecraft.getInstance().getTextureManager().getTexture(entry.getValue(), MissingTextureAtlasSprite.getTexture()) == MissingTextureAtlasSprite.getTexture()) {
                    var texture = data.textures().get(entry.getKey());
                    Minecraft.getInstance().getTextureManager().register(entry.getValue(), texture);
                    newTextureIds.add(entry.getValue());
                } else {
                    removedTextures.remove(entry.getValue());
                }
            }
        });
    }

    // Native Access: 同步结束后，commit 之前调用
    @SuppressWarnings("unused")
    public void freeze() {
        if (!models.containsKey("default") && ClientModelBuilder.getDefaultModel() != null) {
            models.put("default", ClientModelBuilder.getDefaultModel());
        }
        models = Object2ReferenceMaps.unmodifiable(models);
    }

    public void releaseRemovedTextures() {
        for (ResourceLocation textureId : removedTextures) {
            Minecraft.getInstance().getTextureManager().release(textureId);
        }
        removedTextures.clear();
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
