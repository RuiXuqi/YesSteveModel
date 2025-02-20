package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event.YsmMaidScreenEvent;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event.SyncCapability;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.render.CustomYsmMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

public class TlmCompatInner {
    static void registerEvent() {
        MinecraftForge.EVENT_BUS.register(new YsmMaidScreenEvent());
        MinecraftForge.EVENT_BUS.register(new SyncCapability());
    }

    @SuppressWarnings("all")
    static void registerYsmEntityMaidRenderer() {
        EntityMaidRenderer.YSM_ENTITY_MAID_RENDERER = (manager) -> (IGeoEntityRenderer) new CustomYsmMaidRenderer(manager);
    }

    static boolean isMaid(LivingEntity entity) {
        return entity instanceof EntityMaid;
    }

    static boolean maidIsFishing(LivingEntity entity) {
        return entity instanceof EntityMaid maid && maid.fishing != null;
    }

    static void markTacGunAnimationNeedReload(LivingEntity entity) {
        entity.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setTacGunAnimationNeedReload(true);
        });
    }
}
