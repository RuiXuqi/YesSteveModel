package com.elfmcys.ysm.client.model.internal.render;

import com.elfmcys.ysm.client.animation.condition.ConditionManager;
import com.elfmcys.ysm.client.animation.condition.FPArmConditionManager;
import com.elfmcys.ysm.client.model.CommonAsset;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.PlayerModelResources;
import com.elfmcys.ysm.client.model.ProjectileModelResources;
import com.elfmcys.ysm.client.model.RenderTargetResources;
import com.elfmcys.ysm.client.model.VehicleModelResources;
import com.elfmcys.ysm.client.model.data.ModelRenderTargetBuildInput;
import com.elfmcys.ysm.client.model.data.PlayerModelData;
import com.elfmcys.ysm.client.model.data.ProjectileModelData;
import com.elfmcys.ysm.client.model.data.VehicleModelData;
import com.elfmcys.ysm.client.texture.PBRTextureSet;
import com.elfmcys.ysm.geckolib3.core.builder.controller.AnimationControllerData;
import com.elfmcys.ysm.geckolib3.core.molang.value.IValue;
import com.elfmcys.ysm.geckolib3.file.AnimationControllerFile;
import com.elfmcys.ysm.info.ModelInfo;
import com.elfmcys.ysm.model.domain.ModelHash;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ModelRenderTargetAssembler {
    private ModelRenderTargetAssembler() {
    }

    public static ModelRenderTarget build(ModelHash modelHash, ModelRenderTargetBuildInput data) {
        List<AbstractTexture> allTextures = new ArrayList<>();

        var assets = buildCommonAssets(data);
        final RenderTargetResources targetResources;
        if (data.target() instanceof PlayerModelData player) {
            targetResources = buildPlayerModel(player, data.info(), assets, allTextures);
        } else if (data.target() instanceof ProjectileModelData projectile) {
            targetResources = buildProjectileModel(projectile, assets, allTextures);
        } else if (data.target() instanceof VehicleModelData vehicle) {
            targetResources = buildVehicleModel(vehicle, assets, allTextures);
        } else {
            throw new IllegalArgumentException("Unsupported model render target data: " + data.target().getClass());
        }
        return new ModelRenderTarget(modelHash, data.renderTargetId(), targetResources, assets, data.info(), allTextures);
    }

    private static PlayerModelResources buildPlayerModel(PlayerModelData player, ModelInfo info, CommonAsset assets,
                                                         List<AbstractTexture> allTextures) {
        var variants = player.variants();
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Player model has no variants");
        }

        var animations = player.animations();
        var fpArmAnimations = player.firstPersonAnimations();

        var conditionManager = new ConditionManager();
        animations.keySet().forEach(conditionManager::addTest);

        var fpArmConditionManager = new FPArmConditionManager();
        fpArmAnimations.keySet().forEach(fpArmConditionManager::addTest);

        var animationControllers = new Object2ReferenceOpenHashMap<String, AnimationControllerData>();
        for (var file : player.animationControllerFiles()) {
            animationControllers.putAll(file.animationControllers());
        }

        for (var variant : variants.values()) {
            var texture = variant.texture();
            allTextures.add(texture);
            if (texture instanceof PBRTextureSet pbrTextureSet) {
                allTextures.addAll(pbrTextureSet.getPBRTextures().values());
            }
        }
        var defaultTexture = !StringUtils.isEmpty(info.properties().defaultTexture())
                && variants.containsKey(info.properties().defaultTexture())
                ? info.properties().defaultTexture() : variants.getKeyAt(0);

        return new PlayerModelResources(variants, animations, fpArmAnimations, conditionManager, fpArmConditionManager,
                animationControllers, defaultTexture, assets);
    }


    private static ProjectileModelResources buildProjectileModel(ProjectileModelData projectile, CommonAsset assets,
                                                         List<AbstractTexture> allTextures) {
        AnimationControllerFile controllerFile = projectile.controllerFile();
        Object2ReferenceMap<String, AnimationControllerData> controllers = Object2ReferenceMaps.emptyMap();
        if (controllerFile != null) {
            controllers = new Object2ReferenceOpenHashMap<>(controllerFile.animationControllers());
        }
        allTextures.add(projectile.texture());
        allTextures.addAll(projectile.texture().getPBRTextures().values());
        return new ProjectileModelResources(projectile.geoModel(), projectile.animations(), controllers,
                projectile.texture(), assets);
    }

    private static VehicleModelResources buildVehicleModel(VehicleModelData vehicle, CommonAsset assets,
                                                   List<AbstractTexture> allTextures) {
        var controllerFile = vehicle.controllerFile();
        Object2ReferenceMap<String, AnimationControllerData> controllers = Object2ReferenceMaps.emptyMap();
        if (controllerFile != null) {
            controllers = new Object2ReferenceOpenHashMap<>(controllerFile.animationControllers());
        }
        allTextures.add(vehicle.texture());
        allTextures.addAll(vehicle.texture().getPBRTextures().values());
        return new VehicleModelResources(vehicle.geoModel(), vehicle.animations(), controllers, vehicle.texture(), assets);
    }

    private static CommonAsset buildCommonAssets(ModelRenderTargetBuildInput data) {
        var sounds = data.assets().sounds();
        var userFunctions = buildUserFunctionMap(data);
        var eventHandlers = buildEventHandlers(data);

        return new CommonAsset(sounds, userFunctions, eventHandlers, data.assets().languageFiles());
    }

    private static Object2ReferenceOpenHashMap<String, IValue> buildUserFunctionMap(ModelRenderTargetBuildInput data) {
        var map = new Object2ReferenceOpenHashMap<String, IValue>(data.assets().userFunctions().size());
        for (var entry : data.assets().userFunctions().entrySet()) {
            var name = entry.getKey();
            var splitterIndex = name.indexOf('@');
            if (splitterIndex == 0) {
                continue;
            } else if (splitterIndex != -1) {
                name = name.substring(0, splitterIndex);
            }
            map.put(name, entry.getValue());
        }
        return map;
    }

    private static Object2ReferenceOpenHashMap<String, List<IValue>> buildEventHandlers(ModelRenderTargetBuildInput data) {
        var map = new Object2ReferenceOpenHashMap<String, List<IValue>>();
        for (var entry : data.assets().userFunctions().entrySet()) {
            var splitterIndex = entry.getKey().indexOf('@');
            if (splitterIndex != -1 && splitterIndex + 1 < entry.getKey().length()) {
                var eventTypeName = entry.getKey().substring(splitterIndex + 1);
                var eventType = eventTypeName.toLowerCase();
                map.computeIfAbsent(eventType, k -> new ReferenceArrayList<>()).add(entry.getValue());
            }
        }
        return map;
    }

}
