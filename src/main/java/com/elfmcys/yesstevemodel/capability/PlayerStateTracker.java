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

    private ItemStack mainhandItemStack = ItemStack.EMPTY;
    private ItemStack offhandItemStack = ItemStack.EMPTY;

    private static float YAW_SPEED;
    private static float LAST_YAW;

    public PlayerStateTracker(Player player, boolean localPlayer) {
        super(player);
        effects = new Object2ByteOpenHashMap<>(8);
        this.localPlayer = localPlayer;
    }

    public void updateServerDrivenProperty(DispatchServerDrivenProperty msg) {
        if (msg.flying >= 0) {
            remoteFlying = msg.flying != 0;
        }
        if (!msg.effects.isEmpty()) {
            if (msg.full) {
                effects.clear();
            }
            effects.putAll(msg.effects);
        }
        if (msg.expLevel > 0) {
            expLevel = msg.expLevel;
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

    public byte getEffectLevel(MobEffect effect) {
        if (localPlayer) {
            var instance = entity.getEffect(effect);
            return instance != null ? (byte) (instance.getAmplifier() + 1) : 0;
        } else {
            return effects.getOrDefault(effect, (byte) 0);
        }
    }

    @Override
    protected void updateRenderTickData(float currentFrameTime, float lastFrameTime, float partialTicks) {
        if (localPlayer) {
            updateLocalPlayerYawSpeed(entity, currentFrameTime, lastFrameTime);
        }
        super.updateRenderTickData(currentFrameTime, lastFrameTime, partialTicks);
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

    private static void updateLocalPlayerYawSpeed(Player entity, float currentFrameTime, float lastFrameTime) {
        float yaw = entity.getYRot();
        if (lastFrameTime > 0) {
            YAW_SPEED = (yaw - LAST_YAW) * 1000 / (currentFrameTime - lastFrameTime);
        }
        LAST_YAW = yaw;
    }

    public static float getLocalPlayerYawSpeed() {
        return YAW_SPEED;
    }
}
