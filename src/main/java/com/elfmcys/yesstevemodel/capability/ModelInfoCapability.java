package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.message.SubmitVariableChanges;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ModelInfoCapability {
    private String modelId;
    private String selectTexture;
    private String animation = "idle";
    private boolean playAnimation = false;
    private Object2FloatOpenHashMap<String> variables = new Object2FloatOpenHashMap<>();
    private int instanceId;
    private boolean dirty;
    private boolean mandatory;

    public ModelInfoCapability() {
        var defaultModel = ServerModelManager.getDefaultModelAndTexture();
        this.modelId = defaultModel.getLeft();
        this.selectTexture = defaultModel.getRight();
    }

    public void setModelAndTexture(String modelId, String selectTexture) {
        if (this.modelId.equals(modelId) && this.selectTexture.equals(selectTexture)) {
            return;
        }
        this.modelId = modelId;
        this.selectTexture = selectTexture;
        markDirty();
    }

    public void setDefault() {
        var defaultModel = ServerModelManager.getDefaultModelAndTexture();
        if (!this.modelId.equals(defaultModel.getLeft())) {
            resetVariables(instanceId + 1);
        }
        setModelAndTexture(defaultModel.getLeft(), defaultModel.getRight());
    }

    public void copyFrom(ModelInfoCapability source) {
        this.modelId = source.modelId;
        this.selectTexture = source.selectTexture;
        this.animation = source.animation;
        this.playAnimation = source.playAnimation;
        this.variables = source.variables;
        this.instanceId = source.instanceId;
        markDirty();
    }

    public String getModelId() {
        return modelId;
    }

    public String getSelectTexture() {
        return selectTexture;
    }

    public void setSelectTexture(String selectTexture) {
        this.selectTexture = selectTexture;
        markDirty();
    }

    public void playAnimation(String animation) {
        this.animation = animation;
        this.playAnimation = true;
        markDirty();
    }

    public void stopAnimation() {
        this.playAnimation = false;
        markDirty();
    }

    public String getAnimation() {
        return animation;
    }

    public void updateVariables(SubmitVariableChanges packet) {
        if (packet.instanceId < instanceId) {
            return;
        }
        if (packet.instanceId > instanceId) {
            variables.clear();
            instanceId = packet.instanceId;
        }
        for (var entry : packet.variables) {
            variables.put(entry.key(), entry.valueFloat());
        }
        // 无需 mark dirty
    }

    public void resetVariables(int instanceId) {
        if (this.instanceId == instanceId) {
            return;
        }
        variables.clear();
        this.instanceId = instanceId;
    }

    @OnlyIn(Dist.CLIENT)
    public Object2FloatOpenHashMap<String> getVariables() {
        return variables;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public boolean isPlayAnimation() {
        return playAnimation;
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void setMandatory(boolean value) {
        this.mandatory = value;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("model_id", this.modelId);
        tag.putString("select_texture", this.selectTexture);
        tag.putString("animation", this.animation);
        tag.putBoolean("play_animation", this.playAnimation);
        tag.putInt("instance_id", instanceId);
        tag.putBoolean("mandatory", mandatory);

        CompoundTag variablesTag = new CompoundTag();
        tag.put("molang_vars", variablesTag);
        for (var entry : variables.object2FloatEntrySet()) {
            variablesTag.putFloat(entry.getKey(), entry.getFloatValue());
        }

        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.modelId = ModelIdUtil.stripLegacyPrefix(nbt.getString("model_id"));
        this.selectTexture = nbt.getString("select_texture");
        if (selectTexture.length() > 4 && selectTexture.toLowerCase().endsWith(".png")) {
            this.selectTexture = this.selectTexture.substring(0, this.selectTexture.length() - 4);
        }
        this.animation = nbt.getString("animation");
        this.playAnimation = nbt.getBoolean("play_animation");
        this.instanceId = nbt.getInt("instance_id");
        this.mandatory = nbt.getBoolean("mandatory");

        CompoundTag variablesTag = nbt.getCompound("molang_vars");
        for (var name : variablesTag.getAllKeys()) {
            this.variables.put(name, variablesTag.getFloat(name));
        }
    }
}
