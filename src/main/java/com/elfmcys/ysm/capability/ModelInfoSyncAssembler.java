package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.catalog.ServerCatalogSnapshot;
import com.elfmcys.ysm.network.forge.PlayerStateHandler;
import com.elfmcys.ysm.network.protocol.ModelReferenceCodec;
import com.elfmcys.ysm.model.catalog.CatalogRootKind;
import com.elfmcys.ysm.proto.network.protocol.v0.CommonV0;
import com.elfmcys.ysm.proto.network.protocol.v0.PlayerStateV0;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ModelInfoSyncAssembler {
    private ModelInfoSyncAssembler() {
    }

    public static Optional<PlayerStateV0.PlayerStateUpdate> build(ServerPlayer player,
                                                                  ModelInfoCapability capability,
                                                                  ServerCatalogSnapshot snapshot) {
        return ModelSelectionService.resolve(capability, snapshot).map(model -> {
            var hash = model.descriptor().modelHash();
            var reference = CommonV0.ModelReference.newInstance();
            var builtinDefault = model.location().rootKind() == CatalogRootKind.BUILTIN
                    && model.location().path().value().equals("default") ? hash : null;
            ModelReferenceCodec.write(reference, hash, builtinDefault);
            var update = PlayerStateHandler.newFull(player, capability)
                    .setModel(PlayerStateV0.ModelSelectionState.newInstance()
                            .setModel(reference)
                            .setTextureId(capability.getSelectTexture())
                            .setDisabled(capability.isDisabled()));
            capability.getPropertiesTracker().populateFull(update, player);
            var variables = capability.roamingVariables().variables(hash);
            var roaming = PlayerStateV0.RoamingState.newInstance().setModelKey(hash.roamingHash());
            variables.object2FloatEntrySet().fastForEach(entry -> roaming.addVariables(
                    CommonV0.MolangVariable.newInstance()
                            .setName(entry.getKey())
                            .setValue(entry.getFloatValue())));
            update.setRoaming(roaming);
            return update;
        });
    }
}
