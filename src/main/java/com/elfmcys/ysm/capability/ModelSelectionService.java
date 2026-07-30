package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.config.ServerConfig;
import com.elfmcys.ysm.model.catalog.ServerCatalogSnapshot;
import com.elfmcys.ysm.model.storage.ModelFileHandle;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;

import java.util.Optional;

/** Resolves mutable player selection against an explicit immutable server catalog. */
public final class ModelSelectionService {
    private ModelSelectionService() {
    }

    public static boolean selectDefault(ModelInfoCapability capability,
                                        ServerCatalogSnapshot snapshot) {
        var model = snapshot.findPath(ServerConfig.DEFAULT_MODEL_PATH.get())
                .or(snapshot::defaultModel).orElse(null);
        if (model == null) {
            return false;
        }
        var view = model.view();
        var configured = ServerConfig.DEFAULT_MODEL_TEXTURE.get();
        var requested = view.getPlayer().getTextureNames().contains(configured)
                ? configured : view.getManifest().getInfo().getSettings().getDefaultTexture();
        var texture = view.getPlayer().getTextureNames().contains(requested)
                ? requested : view.getPlayer().getTextureNames().stream()
                .sorted().findFirst().orElse("");
        capability.setModelAndTexture(model.descriptor().modelHash(), texture);
        return true;
    }

    public static boolean selectBuiltinDefault(ModelInfoCapability capability,
                                               ServerCatalogSnapshot snapshot,
                                               String requestedTexture) {
        var model = snapshot.models().values().stream()
                .filter(handle -> handle.location().rootKind() == CatalogRootKind.BUILTIN
                        && handle.location().path().value().equals("default"))
                .findFirst().orElse(null);
        if (model == null) {
            return false;
        }
        var textures = model.view().getPlayer().getTextureNames();
        var configured = model.view().getManifest().getInfo().getSettings().getDefaultTexture();
        var texture = textures.contains(requestedTexture) ? requestedTexture
                : textures.contains(configured) ? configured
                : textures.stream().sorted().findFirst().orElse("");
        capability.setModelAndTexture(model.descriptor().modelHash(), texture);
        return true;
    }

    public static Optional<ModelFileHandle> resolve(ModelInfoCapability capability,
                                                     ServerCatalogSnapshot snapshot) {
        if (capability.getModelHash() == null && !selectDefault(capability, snapshot)) {
            return Optional.empty();
        }
        return snapshot.find(capability.getModelHash());
    }

    public static String displayId(ModelInfoCapability capability,
                                   ServerCatalogSnapshot snapshot) {
        var hash = capability.getModelHash();
        return hash == null ? "default" : snapshot.find(hash)
                .map(model -> model.location().path().value())
                .orElse(hash.toString());
    }
}
