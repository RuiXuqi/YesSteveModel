package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProjectileAnimatableCapability extends CustomArrowEntity {
    public ProjectileAnimatableCapability(AbstractArrow arrow) {
        super(arrow);
    }

    public void init(String ownerModelId) {
        setModelId(ownerModelId);
        setInitialized();
    }
}
