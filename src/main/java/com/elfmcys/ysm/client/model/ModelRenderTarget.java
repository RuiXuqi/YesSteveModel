package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.client.lang.LanguageManager;
import com.elfmcys.ysm.client.texture.CustomTextureManager;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.ysm.info.ModelInfo;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

public final class ModelRenderTarget implements AutoCloseable {
    private final ModelHash modelHash;
    private final String renderTargetId;
    private final RenderTargetResources targetResources;
    private final CommonAsset assets;

    private final ModelInfo modelInfo;
    private final List<AbstractTexture> registeredTextureIds;
    private boolean closed;

    public ModelRenderTarget(ModelHash modelHash, String renderTargetId, RenderTargetResources targetResources,
                        CommonAsset assets, ModelInfo modelInfo, List<AbstractTexture> registeredTextureIds) {
        this.modelHash = Objects.requireNonNull(modelHash, "modelHash");
        this.renderTargetId = Objects.requireNonNull(renderTargetId, "renderTargetId");
        this.targetResources = Objects.requireNonNull(targetResources, "targetResources");
        this.assets = Objects.requireNonNull(assets, "assets");
        this.modelInfo = Objects.requireNonNull(modelInfo, "modelInfo");
        this.registeredTextureIds = List.copyOf(registeredTextureIds);
    }

    public ModelHash modelHash() {
        return modelHash;
    }

    public String renderTargetId() {
        return renderTargetId;
    }

    public PlayerModelResources playerResources() {
        return targetResources instanceof PlayerModelResources player ? player : null;
    }

    public CommonAsset assets() {
        return assets;
    }

    public ProjectileModelResources projectileResources() {
        return targetResources instanceof ProjectileModelResources projectile ? projectile : null;
    }

    public VehicleModelResources vehicleResources() {
        return targetResources instanceof VehicleModelResources vehicle ? vehicle : null;
    }

    public ModelInfo info() {
        return modelInfo;
    }

    public String getDisplayName(String defaultName) {
        var metadata = info().metadata();
        if (metadata != null) {
            return LanguageManager.getI18n(this, "metadata.name", metadata.name());
        }
        return defaultName;
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) {
            return;
        }
        closed = true;
        registeredTextureIds.forEach(CustomTextureManager::release);
        if (targetResources instanceof PlayerModelResources player) {
            player.animations().close();
            player.fpArmAnimations().close();
        } else if (targetResources instanceof ProjectileModelResources projectile) {
            projectile.animations().close();
        } else if (targetResources instanceof VehicleModelResources vehicle) {
            vehicle.animations().close();
        }
        var models = Collections.newSetFromMap(new IdentityHashMap<GeoModel, Boolean>());
        if (targetResources instanceof PlayerModelResources player) {
            for (var variant : player.variants().values()) {
                models.add(variant.mainModel());
                models.add(variant.armModel());
            }
        } else if (targetResources instanceof ProjectileModelResources projectile) {
            models.add(projectile.model());
        } else if (targetResources instanceof VehicleModelResources vehicle) {
            models.add(vehicle.model());
        }
        models.forEach(GeoModel::close);
    }
}
