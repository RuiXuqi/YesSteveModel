package com.elfmcys.yesstevemodel.client.data;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.texture.NativeTexture;
import com.elfmcys.yesstevemodel.info.type.ProjectileType;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
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
        textureMap.forEach((key, value) -> removeTextureSet(value, data.textures().get(key)));
        if (data.textures().containsKey(ModelIdUtil.ARROW_TEXTURE_NAME_PLACEHOLDER)) {
            removeTextureSet(ModelIdUtil.getArrowTextureId(data.info().hash()), data.textures().get(ModelIdUtil.ARROW_TEXTURE_NAME_PLACEHOLDER));
        }
        removedTextures.addAll(ClientModelBuilder.buildAuthorAvatarMap(data).values());
    }

    private void removeTextureSet(ResourceLocation id, NativeTexture uv) {
        removedTextures.add(id);
        for (var type : uv.getPBRTextures().keySet()) {
            removedTextures.add(type.getId(id));
        }
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

        model.animations().keySet().forEach(name -> conditionManager.addTest(modelId, name));
        if (isNew) {
            registerModelTextures(data, model);
        }

        models.put(modelId, model);
        if (isDefault) {
            defaultModel = model;
        }
    }

    // 集中注册可能导致严重掉帧，所以提前到这里分散注册
    private void registerModelTextures(ClientModelData data, ClientModel model) {
        Minecraft.getInstance().execute(() -> {
            for (final var entry : model.textures().entrySet()) {
                var textures = data.textures().get(entry.getKey());
                registerTextureSet(entry.getValue(), textures);
            }
            for (final var entry : model.clientModelInfo().authorAvatars().entrySet()) {
                var texture = data.authorAvatars().get(entry.getKey());
                tryRegisterTexture(entry.getValue(), texture);
            }
            for (final var entry : model.projectileModels().entrySet()) {
                if (entry.getKey() == ProjectileType.ARROW) {
                    var texture = data.textures().get(ModelIdUtil.ARROW_TEXTURE_NAME_PLACEHOLDER);
                    registerTextureSet(entry.getValue().texture(), texture);
                }
            }
        });
    }

    private void registerTextureSet(ResourceLocation uvId, NativeTexture uvTexture) {
        tryRegisterTexture(uvId, uvTexture);
        for (var entry : uvTexture.getPBRTextures().entrySet()) {
            tryRegisterTexture(entry.getKey().getId(uvId), entry.getValue());
        }
    }

    private void tryRegisterTexture(ResourceLocation id, AbstractTexture texture) {
        if (Minecraft.getInstance().getTextureManager().getTexture(id, MissingTextureAtlasSprite.getTexture()) == MissingTextureAtlasSprite.getTexture()) {
            Minecraft.getInstance().getTextureManager().register(id, texture);
            newTextureIds.add(id);
        } else {
            removedTextures.remove(id);
        }
    }

    // Native Access: 同步结束后，commit 之前调用
    @SuppressWarnings("unused")
    public void freeze() {
        if (!models.containsKey("default") && ClientModelBuilder.getDefaultModel() != null) {
            ClientModelBuilder.getDefaultModel().animations().keySet().forEach(name -> conditionManager.addTest("default", name));
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
