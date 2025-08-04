package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DispatchServerDrivenProperty {
    public final int entityId;
    public final int variant;
    public boolean flying;
    public Object2ByteMap<MobEffect> effects;
    public int expLevel;
    public int foodLevel;
    public int health;
    public int maxHealth;

    public DispatchServerDrivenProperty(int entityId, int variant) {
        this.entityId = entityId;
        this.variant = variant;
    }

    public static DispatchServerDrivenProperty flying(int entityId, boolean flying) {
        var msg = new DispatchServerDrivenProperty(entityId, 1);
        msg.flying = flying;
        return msg;
    }

    public static DispatchServerDrivenProperty addEffect(int entityId, MobEffect effect, int level) {
        var msg = new DispatchServerDrivenProperty(entityId, 2);
        msg.effects = Object2ByteMaps.singleton(effect, (byte) level);
        return msg;
    }

    public static DispatchServerDrivenProperty removeEffect(int entityId, MobEffect effect) {
        var msg = new DispatchServerDrivenProperty(entityId, 2);
        msg.effects = Object2ByteMaps.singleton(effect, (byte) 0);
        return msg;
    }

    public static DispatchServerDrivenProperty expLevel(int entityId, int expLevel) {
        var msg = new DispatchServerDrivenProperty(entityId, 3);
        msg.expLevel = expLevel;
        return msg;
    }

    public static DispatchServerDrivenProperty foodLevel(int entityId, int foodLevel) {
        var msg = new DispatchServerDrivenProperty(entityId, 4);
        msg.foodLevel = foodLevel;
        return msg;
    }

    public static DispatchServerDrivenProperty health(int entityId, int health) {
        var msg = new DispatchServerDrivenProperty(entityId, 5);
        msg.health = health;
        return msg;
    }

    public static DispatchServerDrivenProperty maxHealth(int entityId, int maxHealth) {
        var msg = new DispatchServerDrivenProperty(entityId, 6);
        msg.maxHealth = maxHealth;
        return msg;
    }

    public static void encode(DispatchServerDrivenProperty msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.variant);

        var variant = msg.variant;
        if (variant == 0 || variant == 1) {
            buf.writeBoolean(msg.flying);
        }
        if (variant == 0 || variant == 2) {
            buf.writeVarInt(msg.effects.size());
            Object2ByteMaps.fastForEach(msg.effects, entry -> {
                buf.writeId(BuiltInRegistries.MOB_EFFECT, entry.getKey());
                buf.writeByte(entry.getByteValue());
            });
        }
        if (variant == 0 || variant == 3) {
            buf.writeVarInt(msg.expLevel);
        }
        if (variant == 0 || variant == 4) {
            buf.writeVarInt(msg.foodLevel);
        }
        if (variant == 0 || variant == 5) {
            buf.writeVarInt(msg.health);
        }
        if (variant == 0 || variant == 6) {
            buf.writeVarInt(msg.maxHealth);
        }
    }

    public static DispatchServerDrivenProperty decode(FriendlyByteBuf buf) {
        var entityId = buf.readVarInt();
        var variant = buf.readVarInt();
        var msg = new DispatchServerDrivenProperty(entityId, variant);

        if (variant == 0 || variant == 1) {
            msg.flying = buf.readBoolean();
        }
        if (variant == 0 || variant == 2) {
            var effectSize = buf.readVarInt();
            if (effectSize == 0) {
                msg.effects = Object2ByteMaps.emptyMap();
            } else if (effectSize == 1) {
                var effect = buf.readById(BuiltInRegistries.MOB_EFFECT);
                var level = buf.readByte();
                msg.effects = Object2ByteMaps.singleton(effect, level);
            } else {
                var effectArray = new MobEffect[effectSize];
                var levelArray = new byte[effectSize];
                for (var i = 0; i < effectSize; ++i) {
                    effectArray[i] = buf.readById(BuiltInRegistries.MOB_EFFECT);
                    levelArray[i] = buf.readByte();
                }
                msg.effects = new Object2ByteArrayMap<>(effectArray, levelArray);
            }
        }
        if (variant == 0 || variant == 3) {
            msg.expLevel = buf.readVarInt();
        }
        if (variant == 0 || variant == 4) {
            msg.foodLevel = buf.readVarInt();
        }
        if (variant == 0 || variant == 5) {
            msg.health = buf.readVarInt();
        }
        if (variant == 0 || variant == 6) {
            msg.maxHealth = buf.readVarInt();
        }

        return msg;
    }

    public static void handle(final DispatchServerDrivenProperty msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(msg.entityId, entity -> handle(entity, msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(Entity entity, DispatchServerDrivenProperty msg) {
        if (entity instanceof Player player) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                cap.getStateTracker().updateServerDrivenProperty(msg);
            });
        }
    }
}

