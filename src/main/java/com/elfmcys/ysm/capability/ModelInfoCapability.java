package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.model.ServerModelManager;
import com.elfmcys.ysm.network.message.SyncModelInfo;
import com.elfmcys.ysm.network.message.data.RoamingVarsChanges;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

public class ModelInfoCapability {
    private String modelId;
    private String selectTexture;
    /**
     * 用于处理假人等伪造的玩家实体
     */
    private boolean mandatory;
    private Int2ReferenceOpenHashMap<Object2FloatOpenHashMap<String>> molangStorage;
    private ServerDrivenPlayerPropertiesTracker propertiesTracker;
    /**
     * 用于禁用 YSM 模型，因为有玩家想强制显示原版玩家模型
     */
    private boolean disabled;

    /* 以下字段不参与持久化 */
    private boolean dirty;
    private final Queue<Consumer<Object2FloatOpenHashMap<String>>> molangVarsConsumers;

    public ModelInfoCapability() {
        var defaultModel = ServerModelManager.getDefaultModelAndTexture();
        this.modelId = defaultModel.getLeft();
        this.selectTexture = defaultModel.getRight();
        this.molangStorage = new Int2ReferenceOpenHashMap<>();
        this.propertiesTracker = new ServerDrivenPlayerPropertiesTracker();
        this.molangVarsConsumers = Queues.newArrayDeque();
        this.disabled = false;
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
        this.mandatory = source.mandatory;
        this.propertiesTracker = source.propertiesTracker;
        this.molangVarsConsumers.addAll(source.molangVarsConsumers);
        this.disabled = source.disabled;
        source.molangVarsConsumers.clear();
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

    // 必须在主线程上调用
    public Optional<SyncModelInfo> buildPacketForDispatch(ServerPlayer entity, boolean broadcast) {
        return ServerModelManager.getModel(modelId).map(model -> {
            var molangVars = molangStorage.computeIfAbsent(model.info().hashShort(), hash -> new Object2FloatOpenHashMap<>(0));
            while (true) {
                var task = molangVarsConsumers.poll();
                if (task == null) {
                    break;
                }
                task.accept(molangVars);
            }
            return new SyncModelInfo(entity.getId(), modelId, selectTexture, disabled,
                    propertiesTracker.full(entity, broadcast).molangVars(model.info().hashShort(), molangVars));
        });
    }

    public void executeWithMolangVars(Consumer<Object2FloatOpenHashMap<String>> consumer) {
        ServerModelManager.getModel(modelId).ifPresentOrElse(model -> {
            int index = model.info().hashShort();
            var molangVars = molangStorage.computeIfAbsent(index, hash -> new Object2FloatOpenHashMap<>(0));
            consumer.accept(molangVars);
        }, () -> {
            molangVarsConsumers.add(consumer);
        });
    }

    public Optional<Object2FloatOpenHashMap<String>> getMolangVars() {
        return ServerModelManager.getModel(modelId)
                .map(m -> molangStorage.computeIfAbsent(m.info().hashShort(), hash -> new Object2FloatOpenHashMap<>(0)));
    }

    public void updateRoamingVars(ServerPlayer player, RoamingVarsChanges changes) {
        molangStorage.compute(changes.modelHashShort, (hash, map) -> {
            if (map != null) {
                map.putAll(changes.variablesServerBound);
                return map;
            } else {
                return new Object2FloatOpenHashMap<>(changes.variablesServerBound);
            }
        });
        propertiesTracker.updateMolangVars(player, !dirty, changes.modelHashShort, changes.variablesServerBound);
        // 无需 markDirty
    }

    public void trimRoamingStorage(IntSet hashSet) {
        var iter = molangStorage.int2ReferenceEntrySet().fastIterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (!hashSet.contains(entry.getIntKey())) {
                iter.remove();
            }
        }
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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putString("model_id", this.modelId);
        tag.putString("select_texture", this.selectTexture);
        tag.putBoolean("mandatory", mandatory);
        tag.putBoolean("disabled", disabled);

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
        this.modelId = nbt.getString("model_id");
        this.selectTexture = nbt.getString("select_texture");
        if (selectTexture.length() > 4 && selectTexture.toLowerCase().endsWith(".png")) {
            this.selectTexture = this.selectTexture.substring(0, this.selectTexture.length() - 4);
        }
        this.mandatory = nbt.getBoolean("mandatory");
        this.disabled = nbt.getBoolean("disabled");

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
