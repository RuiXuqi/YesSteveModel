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
    public final boolean full;
    public final byte flying;
    public final int expLevel;
    public final Object2ByteMap<MobEffect> effects;

    private DispatchServerDrivenProperty(boolean full, int entityId, byte flying, int expLevel, Object2ByteMap<MobEffect> effects) {
        this.full = full;
        this.entityId = entityId;
        this.flying = flying;
        this.expLevel = expLevel;
        this.effects = effects;
    }

    public static DispatchServerDrivenProperty flying(int entityId, boolean flying) {
        return new DispatchServerDrivenProperty(false, entityId, flying ? (byte) 1 : (byte) 0, -1, Object2ByteMaps.emptyMap());
    }

    public static DispatchServerDrivenProperty addEffect(int entityId, MobEffect effect, int level) {
        return new DispatchServerDrivenProperty(false, entityId, (byte) -1, -1, Object2ByteMaps.singleton(effect, (byte) level));
    }

    public static DispatchServerDrivenProperty removeEffect(int entityId, MobEffect effect) {
        return new DispatchServerDrivenProperty(false, entityId, (byte) -1, -1, Object2ByteMaps.singleton(effect, (byte) 0));
    }

    public static Object expLevel(int entityId, int expLevel) {
        return new DispatchServerDrivenProperty(false, entityId, (byte) -1, expLevel, Object2ByteMaps.emptyMap());
    }

    /**
     * 全量同步
     * 为避免 CME 必须在主线程上调用
     */
    public static DispatchServerDrivenProperty full(Entity entity) {
        byte flying;
        int expLevel;
        if (entity instanceof Player player) {
            flying = player.getAbilities().flying ? (byte) 1 : (byte) 0;
            expLevel = player.experienceLevel;
        } else {
            flying = -1;
            expLevel = -1;
        }

        Object2ByteMap<MobEffect> effects;
        if (entity instanceof LivingEntity living) {
            var effectInstances = living.getActiveEffects();
            if (effectInstances.isEmpty()) {
                effects = Object2ByteMaps.emptyMap();
            } else if (effectInstances.size() == 1) {
                var effectInstance = effectInstances.iterator().next();
                effects = Object2ByteMaps.singleton(effectInstance.getEffect(), (byte) (effectInstance.getAmplifier() + 1));
            } else {
                var effectArray = new MobEffect[effectInstances.size()];
                var levelArray = new byte[effectInstances.size()];
                var i = 0;
                for (var effectInstance : effectInstances) {
                    effectArray[i] = effectInstance.getEffect();
                    levelArray[i] = (byte) (effectInstance.getAmplifier() + 1);
                    ++i;
                }
                effects = new Object2ByteArrayMap<>(effectArray, levelArray);
            }
        } else {
            effects = Object2ByteMaps.emptyMap();
        }

        return new DispatchServerDrivenProperty(true, entity.getId(), flying, expLevel, effects);
    }

    public static void encode(DispatchServerDrivenProperty message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.full);
        buf.writeVarInt(message.entityId);
        buf.writeByte(message.flying);
        buf.writeVarInt(message.expLevel);
        buf.writeVarInt(message.effects.size());
        Object2ByteMaps.fastForEach(message.effects, entry -> {
            buf.writeId(BuiltInRegistries.MOB_EFFECT, entry.getKey());
            buf.writeByte(entry.getByteValue());
        });
    }

    public static DispatchServerDrivenProperty decode(FriendlyByteBuf buf) {
        var full = buf.readBoolean();
        var entityId = buf.readVarInt();
        var flying = buf.readByte();
        var expLevel = buf.readVarInt();
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

        return new DispatchServerDrivenProperty(full, entityId, flying, expLevel, effects);
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

