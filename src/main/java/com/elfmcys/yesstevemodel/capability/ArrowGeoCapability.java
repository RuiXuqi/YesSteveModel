package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.instance.CustomArrowInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArrowGeoCapability extends CustomArrowInstance {
    private boolean initialized = false;

    public ArrowGeoCapability(AbstractArrow arrow) {
        super(arrow);
    }

    public void init(String ownerModelId) {
        animatable.setModelId(ownerModelId);
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
