package com.elfmcys.yesstevemodel.client.animation;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoInstance;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Iterator;

public class AnimationParallelTicker {
    private static final ReferenceArrayList<WeakReference<GeoInstance<?, ?>>> INSTANCE_LIST = new ReferenceArrayList<>(64);

    public static void tickAll(final float partialTick, final Frustum clippinghelper, final Camera pActiveRenderInfo) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer localPlayer = mc.player;
        if(localPlayer == null) {
            return;
        }

        final EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        final Vec3 vector3d = pActiveRenderInfo.getPosition();
        final Iterator<WeakReference<GeoInstance<?, ?>>> iterator = INSTANCE_LIST.iterator();
        while(iterator.hasNext()) {
            final GeoInstance<?, ?> instance = iterator.next().get();
            if (instance == null) {
                iterator.remove();
                continue;
            }
            if(!instance.isActive()) {
                // 原版 mc 不会 revive 客户端实体，此处假设其它模组也不会；
                // 如果出现玩家动画不更新的 bug，优先排查这里。
                iterator.remove();
                continue;
            }
            if(!instance.isInitialized() || !instance.canUpdateAsync()) {
                continue;
            }

            final Entity entity = instance.getAnimatable().getEntity();
            if(entity instanceof AbstractClientPlayer) {
                if (entity instanceof LocalPlayer) {
                    if (GeneralConfig.DISABLE_SELF_MODEL.get()) {
                        continue;
                    }
                } else {
                    if (GeneralConfig.DISABLE_OTHER_MODEL.get()) {
                        continue;
                    }
                }
            } else if(entity instanceof AbstractArrow) {
                if(GeneralConfig.DISABLE_ARROWS_MODEL.get()) {
                    continue;
                }
            }

            if (dispatcher.shouldRender(entity, clippinghelper, vector3d.x, vector3d.y, vector3d.z)) {
                instance.beginAsyncUpdate(partialTick);
            }
        }
    }

    public static void register(GeoInstance<?, ?> instance) {
        INSTANCE_LIST.add(new WeakReference<>(instance));
    }
}
