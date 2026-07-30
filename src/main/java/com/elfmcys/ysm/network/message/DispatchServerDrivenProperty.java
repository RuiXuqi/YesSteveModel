package com.elfmcys.ysm.network.message;

import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.client.event.EntityLoadEvent;
import com.elfmcys.ysm.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;
import org.joml.Math;

import java.util.function.Supplier;

public class DispatchServerDrivenProperty {
    public int entityId;
    public short variant;
    public boolean flying;
    public Object2ByteMap<MobEffect> effects;
    public int expLevel;
    public int foodLevel;
    public int health;
    public int maxHealth;
    public byte xxa;
    public byte yya;
    public byte zza;
    public boolean inShieldBlockCooldown;
    public String extraAnimation;
    private int modelHashShort;
    public Object2FloatMap<String> molangVarsServerBound;
    public Int2FloatMap molangVarsClientBound;

    public DispatchServerDrivenProperty(int entityId) {
        this.entityId = entityId;
    }

    public boolean isEmpty() {
        return this.variant == 0;
    }

    public boolean isFull() {
        return (variant & ((short) 1)) != 0;
    }

    public void setFull() {
        variant |= ((short) 1);
    }

    public void clear(int entityId) {
        this.entityId = entityId;
        this.variant = 0;
        this.effects = null;
        this.molangVarsServerBound = null;
    }

    public DispatchServerDrivenProperty flying(boolean flying) {
        this.variant |= ((short) 1 << 1);
        this.flying = flying;
        return this;
    }

    public DispatchServerDrivenProperty addEffect(MobEffect effect, int level) {
        this.variant |= ((short) 1 << 2);
        if (this.effects == null) {
            this.effects = Object2ByteMaps.singleton(effect, (byte) level);
        } else if (this.effects.size() == 1) {
            this.effects = new Object2ByteOpenHashMap<>(this.effects);
            this.effects.put(effect, (byte) level);
        } else {
            this.effects.put(effect, (byte) level);
        }
        return this;
    }

    public DispatchServerDrivenProperty allEffect(Object2ByteMap<MobEffect> effects) {
        this.variant |= ((short) 1 << 2);
        this.effects = effects;
        return this;
    }

    public DispatchServerDrivenProperty removeEffect(MobEffect effect) {
        addEffect(effect, 0);
        return this;
    }

    public DispatchServerDrivenProperty expLevel(int expLevel) {
        this.variant |= ((short) 1 << 3);
        this.expLevel = expLevel;
        return this;
    }

    public DispatchServerDrivenProperty foodLevel(int foodLevel) {
        this.variant |= ((short) 1 << 4);
        this.foodLevel = foodLevel;
        return this;
    }

    public DispatchServerDrivenProperty health(int health) {
        this.variant |= ((short) 1 << 5);
        this.health = health;
        return this;
    }

    public DispatchServerDrivenProperty maxHealth(int maxHealth) {
        this.variant |= ((short) 1 << 6);
        this.maxHealth = maxHealth;
        return this;
    }

    public DispatchServerDrivenProperty xxa(float xxa) {
        this.variant |= ((short) 1 << 7);
        this.xxa = (byte) Math.round(Math.clamp(xxa, -1f, 1f) * 127);
        return this;
    }

    public DispatchServerDrivenProperty yya(float yya) {
        this.variant |= ((short) 1 << 8);
        this.yya = (byte) Math.round(Math.clamp(yya, -1f, 1f) * 127);
        return this;
    }

    public DispatchServerDrivenProperty zza(float zza) {
        this.variant |= ((short) 1 << 9);
        this.zza = (byte) Math.round(Math.clamp(zza, -1f, 1f) * 127);
        return this;
    }

    public DispatchServerDrivenProperty inShieldBlockCooldown(boolean inShieldBlockCooldown) {
        this.variant |= ((short) 1 << 10);
        this.inShieldBlockCooldown = inShieldBlockCooldown;
        return this;
    }

    public DispatchServerDrivenProperty extraAnimation(String animation) {
        this.variant |= ((short) 1 << 11);
        this.extraAnimation = animation;
        return this;
    }

    public DispatchServerDrivenProperty molangVars(int modelHashShort, Object2FloatMap<String> molangVars) {
        if (this.molangVarsServerBound == null || this.modelHashShort != modelHashShort) {
            this.variant |= ((short) 1 << 12);
            this.modelHashShort = modelHashShort;
            this.molangVarsServerBound = new Object2FloatOpenHashMap<>(molangVars);
        } else {
            this.molangVarsServerBound.putAll(molangVars);
        }
        return this;
    }

    public static void encode(DispatchServerDrivenProperty msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeShort(msg.variant);

        var variant = msg.variant;
        if ((variant & ((short) 1 << 1)) != 0) {
            buf.writeBoolean(msg.flying);
        }
        if ((variant & ((short) 1 << 2)) != 0) {
            buf.writeVarInt(msg.effects.size());
            Object2ByteMaps.fastForEach(msg.effects, entry -> {
                buf.writeId(BuiltInRegistries.MOB_EFFECT, entry.getKey());
                buf.writeByte(entry.getByteValue());
            });
        }
        if ((variant & ((short) 1 << 3)) != 0) {
            buf.writeVarInt(msg.expLevel);
        }
        if ((variant & ((short) 1 << 4)) != 0) {
            buf.writeVarInt(msg.foodLevel);
        }
        if ((variant & ((short) 1 << 5)) != 0) {
            buf.writeVarInt(msg.health);
        }
        if ((variant & ((short) 1 << 6)) != 0) {
            buf.writeVarInt(msg.maxHealth);
        }
        if ((variant & ((short) 1 << 7)) != 0) {
            buf.writeByte(msg.xxa);
        }
        if ((variant & ((short) 1 << 8)) != 0) {
            buf.writeByte(msg.yya);
        }
        if ((variant & ((short) 1 << 9)) != 0) {
            buf.writeByte(msg.zza);
        }
        if ((variant & ((short) 1 << 10)) != 0) {
            buf.writeBoolean(msg.inShieldBlockCooldown);
        }
        if ((variant & ((short) 1 << 11)) != 0) {
            buf.writeUtf(msg.extraAnimation);
        }
        if ((variant & ((short) 1 << 12)) != 0) {
            buf.writeInt(msg.modelHashShort);
            buf.writeVarInt(msg.molangVarsServerBound.size());
            Object2FloatMaps.fastForEach(msg.molangVarsServerBound, entry -> {
                buf.writeUtf(entry.getKey());
                buf.writeFloat(entry.getFloatValue());
            });
        }
    }

    public static DispatchServerDrivenProperty decode(FriendlyByteBuf buf) {
        var entityId = buf.readVarInt();
        var variant = buf.readShort();
        var msg = new DispatchServerDrivenProperty(entityId);
        msg.variant = variant;

        if ((variant & ((short) 1 << 1)) != 0) {
            msg.flying = buf.readBoolean();
        }
        if ((variant & ((short) 1 << 2)) != 0) {
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
        if ((variant & ((short) 1 << 3)) != 0) {
            msg.expLevel = buf.readVarInt();
        }
        if ((variant & ((short) 1 << 4)) != 0) {
            msg.foodLevel = buf.readVarInt();
        }
        if ((variant & ((short) 1 << 5)) != 0) {
            msg.health = buf.readVarInt();
        }
        if ((variant & ((short) 1 << 6)) != 0) {
            msg.maxHealth = buf.readVarInt();
        }
        if ((variant & ((short) 1 << 7)) != 0) {
            msg.xxa = buf.readByte();
        }
        if ((variant & ((short) 1 << 8)) != 0) {
            msg.yya = buf.readByte();
        }
        if ((variant & ((short) 1 << 9)) != 0) {
            msg.zza = buf.readByte();
        }
        if ((variant & ((short) 1 << 10)) != 0) {
            msg.inShieldBlockCooldown = buf.readBoolean();
        }
        if ((variant & ((short) 1 << 11)) != 0) {
            msg.extraAnimation = buf.readUtf();
        }
        if ((variant & ((short) 1 << 12)) != 0) {
            msg.modelHashShort = buf.readInt();
            var size = buf.readVarInt();
            if (msg.isFull()) {
                var vars = msg.molangVarsClientBound = new Int2FloatOpenHashMap(size);
                for (var i = 0; i < size; ++i) {
                    var name = StringPool.computeIfAbsent(buf.readUtf());
                    var value = buf.readFloat();
                    vars.put(name, value);
                }
            } else {
                if (size == 0) {
                    msg.molangVarsClientBound = Int2FloatMaps.EMPTY_MAP;
                } else if (size == 1) {
                    var name = StringPool.computeIfAbsent(buf.readUtf());
                    var value = buf.readFloat();
                    msg.molangVarsClientBound = Int2FloatMaps.singleton(name, value);
                } else {
                    var nameArray = new int[size];
                    var valueArray = new float[size];
                    for (var i = 0; i < size; ++i) {
                        nameArray[i] = StringPool.computeIfAbsent(buf.readUtf());
                        valueArray[i] = buf.readFloat();
                    }
                    msg.molangVarsClientBound = new Int2FloatArrayMap(nameArray, valueArray);
                }
            }
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
                if ((msg.variant & ((short) 1 << 11)) != 0) {
                    if (!StringUtils.isEmpty(msg.extraAnimation)) {
                        cap.playExtraAnimation(msg.extraAnimation);
                    } else {
                        cap.stopExtraAnimation();
                    }
                }
                if ((msg.variant & ((short) 1 << 12)) != 0) {
                    if (msg.isFull()) {
                        cap.resetRoamingVars(msg.modelHashShort, (Int2FloatOpenHashMap) msg.molangVarsClientBound);
                    } else {
                        cap.updateRemoteRoamingVars(msg.modelHashShort, msg.molangVarsClientBound);
                    }
                }
                cap.getStateTracker().updateServerDrivenProperty(msg);
            });
        }
    }
}

