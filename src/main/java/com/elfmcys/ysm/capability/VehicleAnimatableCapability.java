package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.ysm.client.entity.CustomVehicleEntity;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
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
        // 无条件同步服务端数据
        roamingStruct = new RemoteRoamingStruct(vars);
    }

    public void updateRoamingVars(@NotNull Int2FloatMap vars) {
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
