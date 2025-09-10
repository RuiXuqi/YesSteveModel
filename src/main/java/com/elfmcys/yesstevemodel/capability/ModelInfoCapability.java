package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.message.SyncModelInfo;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class ModelInfoCapability {
    private String modelId;
    private String selectTexture;
    private String animation = "idle";
    private boolean playAnimation = false;
    /**
     * 用于处理假人等伪造的玩家实体
     */
    private boolean mandatory;
    private Int2ReferenceOpenHashMap<Object2FloatOpenHashMap<String>> molangStorage;
    private ServerDrivenPlayerPropertiesTracker propertiesTracker;

    /* 以下字段不参与持久化 */
    private boolean dirty;

    public ModelInfoCapability() {
        var defaultModel = ServerModelManager.getDefaultModelAndTexture();
        this.modelId = defaultModel.getLeft();
        this.selectTexture = defaultModel.getRight();
        this.molangStorage = new Int2ReferenceOpenHashMap<>();
        this.propertiesTracker = new ServerDrivenPlayerPropertiesTracker();
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
        setModelAndTexture(defaultModel.getLeft(), defaultModel.getRight());
    }

    public void copyFrom(ModelInfoCapability source) {
        this.molangStorage = source.molangStorage;
        this.modelId = source.modelId;
        this.selectTexture = source.selectTexture;
        this.animation = source.animation;
        this.playAnimation = source.playAnimation;
        this.mandatory = source.mandatory;
        this.propertiesTracker = source.propertiesTracker;
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
        if (this.playAnimation) {
            this.playAnimation = false;
            markDirty();
        }
    }

    public String getAnimation() {
        return animation;
    }

    // 必须在主线程上调用
    public Optional<SyncModelInfo> buildPacketForDispatch(ServerPlayer entity) {
        return ServerModelManager.getModel(modelId).map(model ->
                new SyncModelInfo(
                        entity.getId(),
                        modelId,
                        model.info().hashShort(),
                        selectTexture,
                        animation,
                        playAnimation,
                        molangStorage.computeIfAbsent(model.info().hashShort(), hash -> new Object2FloatOpenHashMap<>()),
                        null,
                        ServerDrivenPlayerPropertiesTracker.full(entity))
        );
    }

    public void updateRoamingVars(RoamingVarsChanges changes) {
        molangStorage.compute(changes.modelHashShort, (hash, map) -> {
            if (map != null) {
                map.putAll(changes.variablesServerBound);
                return map;
            } else {
                return new Object2FloatOpenHashMap<>(changes.variablesServerBound);
            }
        });
        // 无需 markDirty
    }

    public ServerDrivenPlayerPropertiesTracker getPropertiesTracker() {
        return propertiesTracker;
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

    public void clearDirty() {
        this.dirty = false;
    }

    public void setMandatory(boolean value) {
        if (this.mandatory != value) {
            this.mandatory = value;
            markDirty();
        }
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public Object2FloatOpenHashMap<String> getMolangVarsServerBound() {
        return ServerModelManager.getModel(modelId).map(model -> {
            int index = model.info().hashShort();
            return molangStorage.computeIfAbsent(index, hash -> new Object2FloatOpenHashMap<>());
        }).orElse(new Object2FloatOpenHashMap<>());
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putString("model_id", this.modelId);
        tag.putString("select_texture", this.selectTexture);
        tag.putString("animation", this.animation);
        tag.putBoolean("play_animation", this.playAnimation);
        tag.putBoolean("mandatory", mandatory);

        CompoundTag storageTag = new CompoundTag();
        molangStorage.int2ReferenceEntrySet().fastForEach(storageEntry -> {
            CompoundTag varsTag = new CompoundTag();
            storageEntry.getValue().object2FloatEntrySet().fastForEach(varsEntry -> {
                varsTag.putFloat(varsEntry.getKey(), varsEntry.getFloatValue());
            });
            storageTag.put(String.valueOf(storageEntry.getIntKey()), varsTag);
        });
        tag.put("molang_storage", storageTag);

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
        this.mandatory = nbt.getBoolean("mandatory");

        this.molangStorage.clear();
        var storageTag = nbt.getCompound("molang_storage");
        for (var modelHashShortStr : storageTag.getAllKeys()) {
            var varsTag = storageTag.getCompound(modelHashShortStr);
            var modelHashShort = Integer.parseInt(modelHashShortStr);
            var varsTagKeys = varsTag.getAllKeys();
            var vars = this.molangStorage.computeIfAbsent(modelHashShort, hash -> new Object2FloatOpenHashMap<>(varsTagKeys.size()));
            for (var name : varsTagKeys) {
                var value = varsTag.getFloat(name);
                vars.put(name, value);
            }
        }
    }
}
