package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event.SyncCapability;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event.UpdateRemoteStruct;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event.YsmMaidScreenEvent;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.event.YsmMaidTickEvent;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.render.CustomYsmMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class TlmCompatInner {
    static void registerEvent() {
        MinecraftForge.EVENT_BUS.register(new YsmMaidScreenEvent());
        MinecraftForge.EVENT_BUS.register(new SyncCapability());
        MinecraftForge.EVENT_BUS.register(new YsmMaidTickEvent());
        MinecraftForge.EVENT_BUS.register(new UpdateRemoteStruct());
    }

    @SuppressWarnings("all")
    static void registerYsmEntityMaidRenderer() {
        EntityMaidRenderer.YSM_ENTITY_MAID_RENDERER = (manager) -> (IGeoEntityRenderer) new CustomYsmMaidRenderer(manager);
    }

    static boolean isMaid(Entity entity) {
        return entity instanceof EntityMaid;
    }

    static boolean isChair(Entity entity) {
        return entity instanceof EntityChair;
    }

    static boolean isSit(Entity entity) {
        return entity instanceof EntitySit;
    }

    static String getChairId(Entity entity) {
        if (entity instanceof EntityChair chair) {
            return chair.getModelId();
        }
        return StringUtils.EMPTY;
    }

    static boolean maidIsFishing(LivingEntity entity) {
        return entity instanceof EntityMaid maid && maid.fishing != null;
    }

    static void markTacGunAnimationNeedReload(LivingEntity entity) {
        entity.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setTacGunAnimationNeedReload(true);
        });
    }

    static boolean isGohei(Item item) {
        return item instanceof ItemHakureiGohei;
    }

    static SoundBuffer getSoundBuffer(Entity entity, String soundPath) {
        if (entity instanceof EntityMaid maid) {
            return maid.getCapability(YsmMaidCapabilityProvider.CAP)
                    .map(cap -> ClientModelManager.getModel(cap.getModelId())
                            .map(model -> model.sounds().get(soundPath)))
                    .orElse(Optional.empty())
                    .map(data -> new SoundBuffer(data.byteBuffer(), data.audioFormat()))
                    .orElse(null);
        }
        return null;
    }
}
