package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.domain.ModelHash;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Persistent player model selection and synchronization state. */
public final class ModelInfoCapability {
    private ModelHash modelHash;
    private String selectTexture = "";
    private boolean mandatory;
    private boolean disabled;
    private boolean dirty;
    private long stateRevision;
    private final RoamingVariableStore roamingVariables = new RoamingVariableStore();
    private ServerDrivenPlayerPropertiesTracker propertiesTracker =
            new ServerDrivenPlayerPropertiesTracker();

    public void setModelAndTexture(ModelHash modelHash, String selectTexture) {
        if (Objects.equals(this.modelHash, modelHash)
                && this.selectTexture.equals(selectTexture)) {
            return;
        }
        this.modelHash = modelHash;
        this.selectTexture = selectTexture;
        markDirty();
    }

    public void moveFrom(ModelInfoCapability source) {
        modelHash = source.modelHash;
        selectTexture = source.selectTexture;
        mandatory = source.mandatory;
        disabled = source.disabled;
        stateRevision = source.stateRevision;
        propertiesTracker = source.propertiesTracker;
        roamingVariables.moveFrom(source.roamingVariables);
        markDirty();
    }

    public ModelHash getModelHash() {
        return modelHash;
    }

    public String getSelectTexture() {
        return selectTexture;
    }

    public void setSelectTexture(String selectTexture) {
        this.selectTexture = selectTexture;
        markDirty();
    }

    public void setDisabled(boolean disabled) {
        if (this.disabled != disabled) {
            this.disabled = disabled;
            markDirty();
        }
    }

    public void playAnimation(ServerPlayer player, String animation) {
        propertiesTracker.setExtraAnimation(player, !dirty, animation);
    }

    public void stopAnimation(ServerPlayer player) {
        propertiesTracker.setExtraAnimation(player, !dirty, "");
    }

    public void executeWithMolangVars(
            Consumer<Object2FloatOpenHashMap<String>> consumer) {
        roamingVariables.execute(modelHash, consumer);
    }

    public Optional<Object2FloatOpenHashMap<String>> getMolangVars() {
        return roamingVariables.get(modelHash);
    }

    public void updateRoamingVars(ServerPlayer player, int modelKey,
                                  it.unimi.dsi.fastutil.objects.Object2FloatMap<String> variables) {
        roamingVariables.update(modelKey, variables);
        propertiesTracker.updateMolangVars(player, !dirty,
                modelKey, variables);
    }

    public void applyClientAnimation(String animation) {
        propertiesTracker.acceptClientAnimation(animation);
    }

    public void applyClientRoaming(int modelKey,
                                   it.unimi.dsi.fastutil.objects.Object2FloatMap<String> variables,
                                   boolean full) {
        if (full) {
            roamingVariables.replace(modelKey, variables);
        } else {
            roamingVariables.update(modelKey, variables);
        }
        propertiesTracker.acceptClientRoaming();
    }

    public long nextStateRevision() {
        return ++stateRevision;
    }

    public void trimRoamingStorage(IntSet retainedHashes) {
        roamingVariables.trim(retainedHashes);
    }

    RoamingVariableStore roamingVariables() {
        return roamingVariables;
    }

    public ServerDrivenPlayerPropertiesTracker getPropertiesTracker() {
        return propertiesTracker;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public void setMandatory(boolean mandatory) {
        if (this.mandatory != mandatory) {
            this.mandatory = mandatory;
            markDirty();
        }
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putString("model_hash", modelHash == null ? "" : modelHash.toString());
        tag.putString("select_texture", selectTexture);
        tag.putBoolean("mandatory", mandatory);
        tag.putBoolean("disabled", disabled);
        tag.put("molang_storage", roamingVariables.serialize());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        var storedHash = tag.getString("model_hash");
        try {
            modelHash = storedHash.isEmpty() ? null : ModelHash.parse(storedHash);
        } catch (IllegalArgumentException ignored) {
            modelHash = null;
        }
        selectTexture = tag.getString("select_texture");
        if (selectTexture.length() > 4 && selectTexture.toLowerCase().endsWith(".png")) {
            selectTexture = selectTexture.substring(0, selectTexture.length() - 4);
        }
        mandatory = tag.getBoolean("mandatory");
        disabled = tag.getBoolean("disabled");
        roamingVariables.deserialize(tag.getCompound("molang_storage"));
    }
}
