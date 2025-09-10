package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.entity.CustomProjectileEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProjectileAnimatableCapability extends CustomProjectileEntity {
    public ProjectileAnimatableCapability(Projectile projectile) {
        super(projectile);
    }

    public void init(String ownerModelId) {
        updateModelId(ownerModelId);
        setInitialized();
    }
}
