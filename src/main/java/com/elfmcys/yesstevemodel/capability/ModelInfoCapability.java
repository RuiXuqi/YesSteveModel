package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.network.message.SubmitVariableChanges;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ModelInfoCapability {
    private ResourceLocation modelId = new ResourceLocation(YesSteveModel.MOD_ID, GeneralConfig.DEFAULT_MODEL_ID.get());
    private ResourceLocation selectTexture = new ResourceLocation(YesSteveModel.MOD_ID, GeneralConfig.DEFAULT_MODEL_ID.get() + "/" + GeneralConfig.DEFAULT_MODEL_TEXTURE.get());
    private String animation = "idle";
    private boolean playAnimation = false;
    private Object2FloatOpenHashMap<String> variables = new Object2FloatOpenHashMap<>();
    private int instanceId;
    private boolean dirty;

    public void setModelAndTexture(ResourceLocation modelId, ResourceLocation selectTexture) {
        this.modelId = modelId;
        this.selectTexture = selectTexture;
        markDirty();
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

    public ResourceLocation getModelId() {
        return modelId;
    }

    public ResourceLocation getSelectTexture() {
        return selectTexture;
    }

    public void setSelectTexture(ResourceLocation selectTexture) {
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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("model_id", this.modelId.toString());
        tag.putString("select_texture", this.selectTexture.toString());
        tag.putString("animation", this.animation);
        tag.putBoolean("play_animation", this.playAnimation);
        tag.putInt("instance_id", instanceId);

        CompoundTag variablesTag = new CompoundTag();
        tag.put("molang_vars", variablesTag);
        for (var entry : variables.object2FloatEntrySet()) {
            variablesTag.putFloat(entry.getKey(), entry.getFloatValue());
        }

        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.modelId = new ResourceLocation(nbt.getString("model_id"));
        this.selectTexture = new ResourceLocation(nbt.getString("select_texture"));
        this.animation = nbt.getString("animation");
        this.playAnimation = nbt.getBoolean("play_animation");
        this.instanceId = nbt.getInt("instance_id");

        CompoundTag variablesTag = nbt.getCompound("molang_vars");
        for (var name : variablesTag.getAllKeys()) {
            this.variables.put(name, variablesTag.getFloat(name));
        }
    }
}
