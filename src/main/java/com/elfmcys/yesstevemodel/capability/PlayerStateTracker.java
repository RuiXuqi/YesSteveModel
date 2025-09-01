package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.api.IEntityExtraInfo;
import com.elfmcys.yesstevemodel.geckolib3.model.EntityStateTracker;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerStateTracker extends EntityStateTracker<Player> implements IEntityExtraInfo {
    private final Object2ByteOpenHashMap<MobEffect> effects;
    private final boolean localPlayer;

    private boolean remoteFlying;
    private int expLevel;
    private int health;
    private int maxHealth;
    private int foodLevel;

    private float xxa;
    private float yya;
    private float zza;

    private ItemStack mainhandItemStack = ItemStack.EMPTY;
    private ItemStack offhandItemStack = ItemStack.EMPTY;

    private static float YAW_SPEED;
    private static float LAST_YAW;

    public PlayerStateTracker(Player player, boolean localPlayer) {
        super(player);
        this.effects = new Object2ByteOpenHashMap<>(8);
        this.localPlayer = localPlayer;
    }

    public void updateServerDrivenProperty(DispatchServerDrivenProperty msg) {
        if (msg.variant == 0 || msg.variant == 1) {
            remoteFlying = msg.flying;
        }
        if (msg.effects != null) {
            if (msg.variant == 0) {
                effects.clear();
            }
            effects.putAll(msg.effects);
        }
        if (msg.variant == 0 || msg.variant == 3) {
            expLevel = msg.expLevel;
        }
        if (msg.variant == 0 || msg.variant == 4) {
            foodLevel = msg.foodLevel;
        }
        if (msg.variant == 0 || msg.variant == 5) {
            health = msg.health;
        }
        if (msg.variant == 0 || msg.variant == 6) {
            maxHealth = msg.maxHealth;
        }
        if (msg.variant == 0 || msg.variant == 7) {
            xxa = msg.xxa;
        }
        if (msg.variant == 0 || msg.variant == 8) {
            yya = msg.yya;
        }
        if (msg.variant == 0 || msg.variant == 9) {
            zza = msg.zza;
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

    public ItemStack getHandItem(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return mainhandItemStack;
        } else {
            return offhandItemStack;
        }
    }

    public void setHandItem(ItemStack stack, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainhandItemStack = stack;
        } else {
            this.offhandItemStack = stack;
        }
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
