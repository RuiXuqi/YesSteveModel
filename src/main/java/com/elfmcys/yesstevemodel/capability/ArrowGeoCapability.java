package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.entity.CustomArrowEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArrowGeoCapability extends CustomArrowEntity {
    public ArrowGeoCapability(AbstractArrow arrow) {
        super(arrow);
    }

    public void init(String ownerModelId) {
        setModelId(ownerModelId);
        setInitialized();
    }
}
