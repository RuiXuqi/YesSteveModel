package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.yesstevemodel.client.entity.CustomVehicleEntity;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VehicleAnimatableCapability extends CustomVehicleEntity {
    private @Nullable RemoteRoamingStruct roamingStruct;

    public VehicleAnimatableCapability(Entity entity) {
        super(entity);
    }

    public void init(String ownerModelId) {
        updateModelId(ownerModelId);
        setInitialized();
    }

    public void initRoamingVars(@NotNull Int2FloatOpenHashMap vars) {
        if (roamingStruct == null) {
            // 无条件同步服务端数据
            roamingStruct = new RemoteRoamingStruct(vars);
        } else {
            roamingStruct.update(vars);
        }
    }

    public void updateRoamingVars(@NotNull Int2FloatArrayMap vars) {
        if (roamingStruct == null) {
            roamingStruct = new RemoteRoamingStruct(new Int2FloatOpenHashMap());
        }
        roamingStruct.update(vars);
    }

    @Override
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
        super.preAnimationSetup(seekTime, shouldTick);
        getAnimationProcessor().putRemoteStruct(roamingStruct);
    }
}
