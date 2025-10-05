package com.elfmcys.yesstevemodel.client.model;

import com.elfmcys.yesstevemodel.client.animation.condition.ConditionManager;
import com.elfmcys.yesstevemodel.client.animation.condition.FPArmConditionManager;
import com.elfmcys.yesstevemodel.client.model.data.ClientModelData;
import com.elfmcys.yesstevemodel.client.sound.SoundData;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationControllerFile;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.info.ModelMetadata;
import com.elfmcys.yesstevemodel.lib.concentus.OpusException;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.elfmcys.yesstevemodel.util.SoundDecoderUtil;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class ClientModelBuilder {
    private static final String FP_ARM_ANIMATION = "fp_arm";
    private static final ResourceLocation ARROW = new ResourceLocation("minecraft:arrow");
    private static final ResourceLocation BOAT = new ResourceLocation("minecraft:boat");
    private static ClientModel DEFAULT_MODEL;

    public static ClientModel build(ClientModelData data, boolean isDefault, boolean isNeedAuth) {
        List<AbstractTexture> allTextures = new ArrayList<>();

        var playerModel = buildPlayerModel(data, isDefault, allTextures);
        var projectileModels = buildProjectileModels(data, isDefault, allTextures);
        var vehicleModels = buildVehicleModels(data, isDefault, allTextures);
        var assets = buildCommonAssets(data);

        var clientModelInfo = buildClientModelInfo(data, isNeedAuth, allTextures);

        var model = new ClientModel(playerModel, projectileModels, vehicleModels, assets, data.info(), clientModelInfo, allTextures);
        if (isDefault) {
            DEFAULT_MODEL = model;
        }
        return model;
    }

    public static PlayerModel buildPlayerModel(ClientModelData data, boolean isDefault, List<AbstractTexture> allTextures) {
        var player = data.playerModel();

        var mainModel = player.geoModels().get(0);
        var armModel = player.geoModels().get(1);

        var animations = new Object2ReferenceOpenHashMap<String, Animation>();
        var fpArmAnimations = new Object2ReferenceOpenHashMap<String, Animation>();
        for (var name : player.animationFiles().keySet()) {
            var file = player.animationFiles().get(name);
            if (FP_ARM_ANIMATION.equals(name)) {
                fpArmAnimations.putAll(file.animations());
            } else {
                animations.putAll(file.animations());
            }
        }
        if (!isDefault) {
            for (var entry : DEFAULT_MODEL.playerModel().animations().entrySet()) {
                animations.computeIfAbsent(entry.getKey(), key -> entry.getValue());
            }
            for (var entry : DEFAULT_MODEL.playerModel().fpArmAnimations().entrySet()) {
                fpArmAnimations.computeIfAbsent(entry.getKey(), key -> entry.getValue());
            }
        }

        var conditionManager = new ConditionManager();
        animations.keySet().forEach(conditionManager::addTest);

        var fpArmConditionManager = new FPArmConditionManager();
        fpArmAnimations.keySet().forEach(fpArmConditionManager::addTest);

        var animationControllers = new Object2ReferenceOpenHashMap<String, AnimationControllerData>();
        for (var file : player.animationControllerFiles()) {
            animationControllers.putAll(file.animationControllers());
        }

        for (var texture : player.textures().values()) {
            allTextures.add(texture);
            allTextures.addAll(texture.getPBRTextures().values());
        }
        var defaultTexture = !StringUtils.isEmpty(data.info().properties().defaultTexture()) && player.textures().containsKey(data.info().properties().defaultTexture())
                ? data.info().properties().defaultTexture() : player.textures().getKeyAt(0);

        return new PlayerModel(mainModel, armModel, animations, fpArmAnimations, conditionManager, fpArmConditionManager,
                animationControllers, player.textures(), defaultTexture, player.textures().get(defaultTexture));
    }


    private static Map<ResourceLocation, ProjectileModel> buildProjectileModels(ClientModelData data, boolean isDefault, List<AbstractTexture> allTextures) {
        Object2ReferenceOpenHashMap<ResourceLocation, ProjectileModel> map = new Object2ReferenceOpenHashMap<>();

        for (var projectile : data.projectileModel()) {
            var geoModel = projectile.geoModel();
            AnimationFile animationFile = projectile.animationFile();
            AnimationControllerFile controllerFile = projectile.controllerFile();

            var animations = new Object2ReferenceOpenHashMap<>(animationFile != null ? animationFile.animations() : Object2ReferenceMaps.emptyMap());
            if (!isDefault) {
                for (var animEntry : DEFAULT_MODEL.projectileModels().get(ARROW).animations().entrySet()) {
                    animations.computeIfAbsent(animEntry.getKey(), key -> animEntry.getValue());
                }
            }

            Map<String, AnimationControllerData> controllers = Object2ReferenceMaps.emptyMap();
            if (controllerFile != null) {
                controllers = new Object2ReferenceOpenHashMap<>(controllerFile.animationControllers());
            }

            allTextures.add(projectile.texture());
            allTextures.addAll(projectile.texture().getPBRTextures().values());

            var model = new ProjectileModel(geoModel, animations, controllers, projectile.texture());
            for (var id : ModelIdUtil.getEntityIdMatch(projectile.match())) {
                map.put(id, model);
            }
        }

        return map;
    }

    private static Map<ResourceLocation, VehicleModel> buildVehicleModels(ClientModelData data, boolean isDefault, List<AbstractTexture> allTextures) {
        Object2ReferenceOpenHashMap<ResourceLocation, VehicleModel> map = new Object2ReferenceOpenHashMap<>();

        for (var vehicle : data.vehicleModel()) {
            var geoModel = vehicle.geoModel();
            var animationFile = vehicle.animationFile();
            var controllerFile = vehicle.controllerFile();

            var animations = new Object2ReferenceOpenHashMap<>(animationFile != null ? animationFile.animations() : Object2ReferenceMaps.emptyMap());
            if (!isDefault) {
                for (var animEntry : DEFAULT_MODEL.vehicleModels().get(BOAT).animations().entrySet()) {
                    animations.computeIfAbsent(animEntry.getKey(), key -> animEntry.getValue());
                }
            }

            Map<String, AnimationControllerData> controllers = Object2ReferenceMaps.emptyMap();
            if (controllerFile != null) {
                controllers = new Object2ReferenceOpenHashMap<>(controllerFile.animationControllers());
            }

            allTextures.add(vehicle.texture());
            allTextures.addAll(vehicle.texture().getPBRTextures().values());

            var model = new VehicleModel(geoModel, animations, controllers, vehicle.texture());
            for (var id : ModelIdUtil.getEntityIdMatch(vehicle.match())) {
                map.put(id, model);
            }
        }

        return map;
    }

    private static CommonAsset buildCommonAssets(ClientModelData data) {
        var sounds = buildSoundMap(data);
        var userFunctions = buildUserFunctionMap(data);
        var eventHandlers = buildEventHandlers(data);

        return new CommonAsset(sounds, userFunctions, eventHandlers, data.assets().languageFiles());
    }

    private static ClientModelInfo buildClientModelInfo(ClientModelData data, boolean isNeedAuth, List<AbstractTexture> textures) {
        var guiImages = buildGuiImages(data, textures);

        ModelMetadata metadata = data.info().metadata();
        String name = metadata != null ? metadata.name() : StringUtils.EMPTY;
        return new ClientModelInfo(name, isNeedAuth, data.authorAvatars(), guiImages);
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

    public static Map<String, AbstractTexture> buildGuiImages(ClientModelData data, List<AbstractTexture> allTextures) {
        Object2ObjectOpenHashMap<String, AbstractTexture> map = new Object2ObjectOpenHashMap<>();
        if (data.info().properties() != null) {
            for (var entry : data.guiImages().entrySet()) {
                var texture = entry.getValue();
                if (texture != null) {
                    allTextures.add(texture);
                    map.put(entry.getKey(), texture);
                }
            }
        }
        return Object2ObjectMaps.unmodifiable(map);
    }
}
