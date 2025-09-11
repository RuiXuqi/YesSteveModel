package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.lang.LanguageManager;
import com.elfmcys.yesstevemodel.client.model.data.ClientModelData;
import com.elfmcys.yesstevemodel.client.sound.SoundData;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.info.ModelMetadata;
import com.elfmcys.yesstevemodel.lib.concentus.OpusException;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import com.elfmcys.yesstevemodel.util.SoundDecoderUtil;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class ClientModelBuilder {
    private static ClientModel DEFAULT_MODEL;

    public static ClientModel build(ClientModelData data, boolean isDefault, boolean isNeedAuth, List<Pair<ResourceLocation, AbstractTexture>> textureQueue) {
        var playerModel = buildPlayerModel(data, isDefault, textureQueue);
        var projectileModels = buildProjectileModels(data, isDefault, textureQueue);
        var assets = buildCommonAssets(data);

        var clientModelInfo = buildClientModelInfo(data, isNeedAuth, textureQueue);
        var registeredTextureIds = textureQueue.stream().map(Pair::getKey).toList();

        var model = new ClientModel(playerModel, projectileModels, assets, data.info(), clientModelInfo, registeredTextureIds);
        if (isDefault) {
            DEFAULT_MODEL = model;
        }
        return model;
    }

    public static PlayerModel buildPlayerModel(ClientModelData data, boolean isDefault, List<Pair<ResourceLocation, AbstractTexture>> textureQueue) {
        var player = data.playerModel();

        var mainModel = player.geoModels().get(0);
        var armModel = player.geoModels().get(1);

        var animations = new Object2ReferenceOpenHashMap<String, Animation>();
        for (var file : player.animationFiles()) {
            animations.putAll(file.animations());
        }
        if (!isDefault) {
            for (var entry : DEFAULT_MODEL.playerModel().animations().entrySet()) {
                animations.computeIfAbsent(entry.getKey(), key -> entry.getValue());
            }
        }

        var conditionManager = new ConditionManager();
        animations.keySet().forEach(conditionManager::addTest);

        var animationControllers = new Object2ReferenceOpenHashMap<String, AnimationControllerData>();
        for (var file : player.animationControllerFiles()) {
            animationControllers.putAll(file.animationControllers());
        }

        var texturesMap = new Object2ObjectArrayMap<String, ResourceLocation>();
        int counter = 0;
        for (var entry : player.textures().entrySet()) {
            var id = new ResourceLocation(YesSteveModel.MOD_ID, (isDefault ? "default" : data.info().hash()) + "/" + counter++);
            texturesMap.put(entry.getKey(), id);
            textureQueue.add(Pair.of(id, entry.getValue()));
            for (var pbrEntry : entry.getValue().getPBRTextures().entrySet()) {
                textureQueue.add(Pair.of(pbrEntry.getKey().getId(id), pbrEntry.getValue()));
            }
        }
        var textures = new FifoHashMap<>(texturesMap);
        var defaultTexture = !StringUtils.isEmpty(data.info().properties().defaultTexture()) && textures.containsKey(data.info().properties().defaultTexture())
                ? data.info().properties().defaultTexture() : textures.getKeyAt(0);

        return new PlayerModel(mainModel, armModel, animations, conditionManager, animationControllers, textures, defaultTexture, textures.get(defaultTexture));
    }

    private static Map<ResourceLocation, ProjectileModel> buildProjectileModels(ClientModelData data, boolean isDefault, List<Pair<ResourceLocation, AbstractTexture>> textureQueue) {
        Object2ReferenceOpenHashMap<ResourceLocation, ProjectileModel> map = new Object2ReferenceOpenHashMap<>();

        int counter = 0;
        for (var entry : data.projectileModel().entrySet()) {
            var model = entry.getValue().geoModel();

            var animations = new Object2ReferenceOpenHashMap<>(entry.getValue().animationFile() != null ? entry.getValue().animationFile().animations() : Object2ReferenceMaps.emptyMap());
            if (!isDefault) {
                for (var animEntry : DEFAULT_MODEL.projectileModels().get(EntityType.ARROW.builtInRegistryHolder().key().location()).animations().entrySet()) {
                    animations.computeIfAbsent(animEntry.getKey(), key -> animEntry.getValue());
                }
            }

            var textureId = new ResourceLocation(YesSteveModel.MOD_ID, (isDefault ? "default" : data.info().hash()) + "/p/" + counter++);
            textureQueue.add(Pair.of(textureId, entry.getValue().texture()));
            for (var pbrEntry : entry.getValue().texture().getPBRTextures().entrySet()) {
                textureQueue.add(Pair.of(pbrEntry.getKey().getId(textureId), pbrEntry.getValue()));
            }

            map.put(new ResourceLocation(entry.getKey()), new ProjectileModel(model, animations, textureId));
        }

        return map;
    }

    private static CommonAsset buildCommonAssets(ClientModelData data) {
        var sounds = buildSoundMap(data);
        var userFunctions = buildUserFunctionMap(data);
        var eventHandlers = buildEventHandlers(data);

        return new CommonAsset(sounds, userFunctions, eventHandlers, data.assets().languageFiles());
    }

    private static ClientModelInfo buildClientModelInfo(ClientModelData data, boolean isNeedAuth, List<Pair<ResourceLocation, AbstractTexture>> textureQueue) {
        var guiImages = buildGuiImages(data, textureQueue);
        var authorAvatars = buildAuthorAvatarMap(data, textureQueue);
        var displayInfo = LanguageManager.buildAllDisplayInfos(data);

        ModelMetadata metadata = data.info().metadata();
        String name = metadata != null ? metadata.name() : StringUtils.EMPTY;
        return new ClientModelInfo(name, displayInfo, isNeedAuth, authorAvatars, guiImages);
    }

    private static Int2ReferenceOpenHashMap<IValue> buildUserFunctionMap(ClientModelData data) {
        var map = new Int2ReferenceOpenHashMap<IValue>(data.assets().userFunctions().size());
        for (var entry : data.assets().userFunctions().entrySet()) {
            var name = entry.getKey();
            var splitterIndex = name.indexOf('@');
            if (splitterIndex == 0) {
                continue;
            } else if (splitterIndex != -1) {
                name = name.substring(0, splitterIndex);
            }
            map.put(StringPool.computeIfAbsent(name), entry.getValue());
        }
        return map;
    }

    private static Int2ReferenceOpenHashMap<List<IValue>> buildEventHandlers(ClientModelData data) {
        var map = new Int2ReferenceOpenHashMap<List<IValue>>();
        for (var entry : data.assets().userFunctions().entrySet()) {
            var splitterIndex = entry.getKey().indexOf('@');
            if (splitterIndex != -1 && splitterIndex + 1 < entry.getKey().length()) {
                var eventTypeName = entry.getKey().substring(splitterIndex + 1);
                var eventType = StringPool.computeIfAbsent(eventTypeName.toLowerCase());
                map.computeIfAbsent(eventType, k -> new ReferenceArrayList<>()).add(entry.getValue());
            }
        }
        return map;
    }

    public static Map<String, SoundData> buildSoundMap(ClientModelData data) {
        Object2ObjectOpenHashMap<String, SoundData> map = new Object2ObjectOpenHashMap<>();
        var sounds = data.assets().sounds();
        if (sounds != null && !sounds.isEmpty()) {
            for (String soundPath : sounds.keySet()) {
                SoundData soundData = bufferToSoundData(sounds.get(soundPath));
                if (soundData != null) {
                    map.put(soundPath, soundData);
                }
            }
        }
        return Object2ObjectMaps.unmodifiable(map);
    }

    private static SoundData bufferToSoundData(byte[] byteArray) {
        try {
            return SoundDecoderUtil.bufferToSoundData(byteArray);
        } catch (IOException | OpusException e) {
            e.fillInStackTrace();
        }
        return null;
    }

    @SuppressWarnings("DataFlowIssue")
    public static Map<String, ResourceLocation> buildAuthorAvatarMap(ClientModelData data, List<Pair<ResourceLocation, AbstractTexture>> textureQueue) {
        Object2ObjectOpenHashMap<String, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        if (data.info().metadata() != null) {
            int counter = 0;
            for (var author : data.info().metadata().authors()) {
                var texture = data.authorAvatars().get(author.name());
                if (texture != null) {
                    var id = new ResourceLocation(YesSteveModel.MOD_ID, data.info().hash() + "/author/" + counter++);
                    textureQueue.add(Pair.of(id, texture));
                    map.put(author.name(), id);
                }
            }
        }
        return Object2ObjectMaps.unmodifiable(map);
    }

    public static Map<String, ResourceLocation> buildGuiImages(ClientModelData data, List<Pair<ResourceLocation, AbstractTexture>> textureQueue) {
        Object2ObjectOpenHashMap<String, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        if (data.info().properties() != null) {
            int counter = 0;
            for (var entry : data.guiImages().entrySet()) {
                var texture = entry.getValue();
                if (texture != null) {
                    var id = new ResourceLocation(YesSteveModel.MOD_ID, data.info().hash() + "/gui_image/" + counter++);
                    textureQueue.add(Pair.of(id, texture));
                    map.put(entry.getKey(), id);
                }
            }
        }
        return Object2ObjectMaps.unmodifiable(map);
    }
}
