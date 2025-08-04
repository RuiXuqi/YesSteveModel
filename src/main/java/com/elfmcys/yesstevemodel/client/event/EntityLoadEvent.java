package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class EntityLoadEvent {
    private static final Cache<Integer, List<Consumer<Entity>>> CACHE = CacheBuilder.newBuilder().expireAfterAccess(30,TimeUnit.SECONDS).build();

    @SubscribeEvent
    public static void onEntityLoadToWorld(final EntityJoinLevelEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        var list = CACHE.getIfPresent(event.getEntity().getId());
        if (list != null) {
            for (var consumer : list) {
                consumer.accept(event.getEntity());
            }
        }
        CACHE.invalidate(event.getEntity().getId());
    }

    public static void executeOnEntity(int entityId, Consumer<Entity> consumer) {
        Minecraft.getInstance().execute(() -> {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                var entity = level.getEntity(entityId);
                if (entity != null) {
                    consumer.accept(entity);
                } else {
                    addRecoveryHandler(entityId, consumer);
                }
            }
        });
    }

    // 非线程安全
    private static void addRecoveryHandler(int entityId, Consumer<Entity> consumer) {
        var list = CACHE.getIfPresent(entityId);
        if (list == null) {
            list = new ArrayList<>(3);
            CACHE.put(entityId, list);
        }
        list.add(consumer);
    }
}
