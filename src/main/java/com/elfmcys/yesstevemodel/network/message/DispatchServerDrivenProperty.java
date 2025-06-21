package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DispatchServerDrivenProperty {
    private final int entityId;
    public final byte flying;
    public final Object2ByteMap<MobEffect> effects;

    private DispatchServerDrivenProperty(int entityId, byte flying, Object2ByteMap<MobEffect> effects) {
        this.entityId = entityId;
        this.flying = flying;
        this.effects = effects;
    }

    public static DispatchServerDrivenProperty flying(int entityId, boolean flying) {
        return new DispatchServerDrivenProperty(entityId, flying ? (byte) 1 : (byte) 0, Object2ByteMaps.emptyMap());
    }

    public static DispatchServerDrivenProperty addEffect(int entityId, MobEffect effect, int level) {
        return new DispatchServerDrivenProperty(entityId, (byte) -1, Object2ByteMaps.singleton(effect, (byte) level));
    }

    public static DispatchServerDrivenProperty removeEffect(int entityId, MobEffect effect) {
        return new DispatchServerDrivenProperty(entityId, (byte) -1, Object2ByteMaps.singleton(effect, (byte) -1));
    }

    /**
     * 全量同步
     * 为避免 CME 必须在主线程上调用
     */
    public static DispatchServerDrivenProperty full(Entity entity) {
        byte flying;
        if (entity instanceof Player player) {
            flying = player.getAbilities().flying ? (byte) 1 : (byte) 0;
        } else {
            flying = -1;
        }

        Object2ByteMap<MobEffect> effects = Object2ByteMaps.emptyMap();
        if (entity instanceof LivingEntity living) {
            var effectInstances = living.getActiveEffects();
            effects = new Object2ByteArrayMap<>(effectInstances.size());
            for (var effectInstance : effectInstances) {
                effects.put(effectInstance.getEffect(), (byte) effectInstance.getAmplifier());
            }
        }

        return new DispatchServerDrivenProperty(entity.getId(), flying, effects);
    }

    public static void encode(DispatchServerDrivenProperty message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeByte(message.flying);
        buf.writeMap(message.effects, (b, e) -> b.writeId(BuiltInRegistries.MOB_EFFECT, e), (b, level) -> b.writeByte(level));
    }

    public static DispatchServerDrivenProperty decode(FriendlyByteBuf buf) {
        var entityId = buf.readVarInt();
        var flying = buf.readByte();
        var effectSize = buf.readVarInt();
        Object2ByteMap<MobEffect> effects;
        if (effectSize == 0) {
            effects = Object2ByteMaps.emptyMap();
        } else if (effectSize == 1) {
            var effect = buf.readById(BuiltInRegistries.MOB_EFFECT);
            var level = buf.readByte();
            effects = Object2ByteMaps.singleton(effect, level);
        } else {
            effects = new Object2ByteOpenHashMap<>(effectSize);
            for (int i = 0; i < effectSize; i++) {
                var effect = buf.readById(BuiltInRegistries.MOB_EFFECT);
                var level = buf.readByte();
                effects.put(effect, level);
            }
        }

        return new DispatchServerDrivenProperty(entityId, flying, effects);
    }

    public static void handle(final DispatchServerDrivenProperty msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                var entity = Minecraft.getInstance().level.getEntity(msg.entityId);
                if (entity != null) {
                    handle(entity, msg);
                } else {
                    EntityLoadEvent.addRecoveryHandler(msg.entityId, e -> handle(e, msg));
                }
            });
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(Entity entity, DispatchServerDrivenProperty msg) {
        if (entity instanceof Player player) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                cap.updateServerDrivenProperty(msg);
            });
        }
    }
}
