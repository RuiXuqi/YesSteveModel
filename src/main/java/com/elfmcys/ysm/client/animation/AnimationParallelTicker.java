package com.elfmcys.ysm.client.animation;

import com.elfmcys.ysm.client.entity.CustomEntity;
import com.elfmcys.ysm.config.ClientConfig;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.lang.ref.WeakReference;

public class AnimationParallelTicker {
    private static final ReferenceArrayList<WeakReference<CustomEntity<?>>> ANIMATABLE_LIST = new ReferenceArrayList<>(64);
    private static final ReferenceArrayList<CustomEntity<?>> TASK_LIST = new ReferenceArrayList<>(16);

    public static void add(CustomEntity<?> instance) {
        ANIMATABLE_LIST.add(new WeakReference<>(instance));
    }

    public static void scheduleAll(final float partialTick) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) {
            return;
        }

        final var iterator = ANIMATABLE_LIST.iterator();
        while (iterator.hasNext()) {
            final CustomEntity<?> animatable = iterator.next().get();
            if (animatable == null) {
                iterator.remove();
                continue;
            }
            if (!animatable.isActive()) {
                // 原版 mc 不会 revive 客户端实体，此处假设其它模组也不会；
                // 如果出现玩家动画不更新的 bug，优先排查这里。
                iterator.remove();
                continue;
            }

            animatable.checkModelUpdate();
            if (!animatable.canUpdateAsync() || !animatable.isInitialized() || !animatable.isModelPresent()) {
                continue;
            }

            final Entity entity = animatable.getEntity();
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
            } else {
                if (ClientConfig.DISABLE_VEHICLE_MODEL.get()) {
                    continue;
                }
            }

            animatable.beginAsyncUpdate(partialTick);
            TASK_LIST.add(animatable);
        }
    }

    public static void waitAll() {
        for (var animatable : TASK_LIST) {
            try {
                animatable.waitForAsyncUpdate();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        TASK_LIST.clear();
    }
}
