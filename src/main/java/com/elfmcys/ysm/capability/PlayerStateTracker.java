package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.client.entity.HumanoidStateTracker;
import com.elfmcys.ysm.network.message.DispatchServerDrivenProperty;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

public class PlayerStateTracker extends HumanoidStateTracker<Player> {
    private final boolean localPlayer;
    private final Object2ByteOpenHashMap<MobEffect> effects;

    private boolean remoteFlying;
    private int expLevel;
    private int health;
    private int maxHealth;
    private int foodLevel;

    private float xxa;
    private float yya;
    private float zza;

    private boolean inShieldBlockCooldown;

    private static float YAW_SPEED;
    private static float LAST_YAW;

    public PlayerStateTracker(Player player, boolean localPlayer) {
        super(player);
        this.localPlayer = localPlayer;
        this.effects = new Object2ByteOpenHashMap<>(8);
    }

    @Override
    public void reset() {
        super.reset();

        effects.clear();

        remoteFlying = false;
        expLevel = 0;
        health = 0;
        maxHealth = 0;
        foodLevel = 0;

        xxa = 0;
        yya = 0;
        zza = 0;

        inShieldBlockCooldown = false;
    }

    public void updateServerDrivenProperty(DispatchServerDrivenProperty msg) {
        if ((msg.variant & ((short) 1 << 1)) != 0) {
            remoteFlying = msg.flying;
        }
        if ((msg.variant & ((short) 1 << 2)) != 0) {
            if (msg.isFull()) {
                effects.clear();
            }
            effects.putAll(msg.effects);
        }
        if ((msg.variant & ((short) 1 << 3)) != 0) {
            expLevel = msg.expLevel;
        }
        if ((msg.variant & ((short) 1 << 4)) != 0) {
            foodLevel = msg.foodLevel;
        }
        if ((msg.variant & ((short) 1 << 5)) != 0) {
            health = msg.health;
        }
        if ((msg.variant & ((short) 1 << 6)) != 0) {
            maxHealth = msg.maxHealth;
        }
        if ((msg.variant & ((short) 1 << 7)) != 0) {
            xxa = msg.xxa / 127f;
        }
        if ((msg.variant & ((short) 1 << 8)) != 0) {
            yya = msg.yya / 127f;
        }
        if ((msg.variant & ((short) 1 << 9)) != 0) {
            zza = msg.zza / 127f;
        }
        if ((msg.variant & ((short) 1 << 10)) != 0) {
            inShieldBlockCooldown = msg.inShieldBlockCooldown;
        }
    }

    public boolean isFlying() {
        if (localPlayer) {
            return entity.getAbilities().flying;
        }
        return remoteFlying;
    }

    public int expLevel() {
        return expLevel;
    }

    public int health() {
        return health;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int foodLevel() {
        return foodLevel;
    }

    public float xxa() {
        return xxa;
    }

    public float yya() {
        return yya;
    }

    public float zza() {
        return zza;
    }

    public boolean inShieldBlockCooldown() {
        return inShieldBlockCooldown;
    }

    public byte getEffectLevel(MobEffect effect) {
        if (localPlayer) {
            var instance = entity.getEffect(effect);
            return instance != null ? (byte) (instance.getAmplifier() + 1) : 0;
        } else {
            return effects.getOrDefault(effect, (byte) 0);
        }
    }

    @Override
    protected void updateEntityTickData(int currentRenderTick, int lastRenderTick) {
        if (localPlayer) {
            updateLocalPlayerYawSpeed(entity, currentRenderTick, lastRenderTick);
        }
        super.updateEntityTickData(currentRenderTick, lastRenderTick);
    }

    private static void updateLocalPlayerYawSpeed(Player entity, int currentRenderTick, int lastRenderTick) {
        float yaw = entity.getYRot();   // local player 的 yRot 每帧都更新，不需要处理 partialTicks
        if (lastRenderTick > 0) {
            YAW_SPEED = (yaw - LAST_YAW) * 20 / (currentRenderTick - lastRenderTick);
        }
        LAST_YAW = yaw;
    }

    public static float getLocalPlayerYawSpeed() {
        return YAW_SPEED;
    }
}
