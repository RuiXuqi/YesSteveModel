package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.client.animation.molang.roaming.RemoteRoamingStruct;
import com.elfmcys.ysm.client.entity.CustomProjectileEntity;
import com.elfmcys.ysm.model.domain.ModelHash;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ProjectileAnimatableCapability extends CustomProjectileEntity {
    private @Nullable RemoteRoamingStruct roamingStruct;

    public ProjectileAnimatableCapability(Projectile projectile) {
        super(projectile);
    }

    public void init(ModelHash ownerModelHash) {
        updateModelHash(ownerModelHash);
        setInitialized();
    }

    public void initRoamingVars(Int2FloatOpenHashMap vars) {
        if (vars != null) {
            // 无条件同步服务端数据
            roamingStruct = new RemoteRoamingStruct(vars);
        } else {
            roamingStruct = null;
        }
    }

    @Override
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
        super.preAnimationSetup(seekTime, shouldTick);
        getAnimationProcessor().putRemoteStruct(roamingStruct);
    }
}
