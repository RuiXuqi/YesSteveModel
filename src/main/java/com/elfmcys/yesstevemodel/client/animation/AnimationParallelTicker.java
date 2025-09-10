package com.elfmcys.yesstevemodel.client.animation;

import com.elfmcys.yesstevemodel.config.ClientConfig;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.lang.ref.WeakReference;
import java.util.Iterator;

public class AnimationParallelTicker {
    private static final ReferenceArrayList<WeakReference<AnimatableEntity<?>>> INSTANCE_LIST = new ReferenceArrayList<>(64);

    public static void tickAll(final float partialTick) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) {
            return;
        }

        final Iterator<WeakReference<AnimatableEntity<?>>> iterator = INSTANCE_LIST.iterator();
        while (iterator.hasNext()) {
            final AnimatableEntity<?> instance = iterator.next().get();
            if (instance == null) {
                iterator.remove();
                continue;
            }
            if (!instance.isActive()) {
                // 原版 mc 不会 revive 客户端实体，此处假设其它模组也不会；
                // 如果出现玩家动画不更新的 bug，优先排查这里。
                iterator.remove();
                continue;
            }
            if (!instance.isInitialized() || !instance.canUpdateAsync()) {
                continue;
            }

            final Entity entity = instance.getEntity();
            if (entity instanceof AbstractClientPlayer) {
                if (entity instanceof LocalPlayer) {
                    if (ClientConfig.DISABLE_SELF_MODEL.get()) {
                        continue;
                    }
                } else {
                    if (ClientConfig.DISABLE_OTHER_MODEL.get()) {
                        continue;
                    }
                }
            } else if (entity instanceof Projectile) {
                if (ClientConfig.DISABLE_PROJECTILE_MODEL.get()) {
                    continue;
                }
            }

            instance.beginAsyncUpdate(partialTick);
        }
    }

    public static void register(AnimatableEntity<?> instance) {
        INSTANCE_LIST.add(new WeakReference<>(instance));
    }
}
