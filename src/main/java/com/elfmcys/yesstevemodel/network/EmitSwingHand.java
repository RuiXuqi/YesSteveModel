package com.elfmcys.yesstevemodel.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EmitSwingHand {
    private final InteractionHand hand;

    public EmitSwingHand(InteractionHand hand) {
        this.hand = hand;
    }

    public static void encode(EmitSwingHand message, FriendlyByteBuf buf) {
        buf.writeEnum(message.hand);
    }

    public static EmitSwingHand decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new EmitSwingHand(hand);
    }

    public static void handle(EmitSwingHand message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        var player = context.getSender();
        if (context.getDirection().getReceptionSide().isServer() && player != null) {
            context.enqueueWork(() -> {
                // 自行实现一套原版的 swing，避免特殊情况
                swing(message, player);
            });
        }
        context.setPacketHandled(true);
    }

    private static void swing(EmitSwingHand message, ServerPlayer player) {
        InteractionHand hand = message.hand;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.onEntitySwing(player)) {
            if (!player.swinging || player.swingTime >= getCurrentSwingDuration(player) / 2 || player.swingTime < 0) {
                player.swingTime = -1;
                player.swinging = true;
                player.swingingArm = hand;
                if (player.level() instanceof ServerLevel) {
                    ClientboundAnimatePacket clientboundanimatepacket = new ClientboundAnimatePacket(player, hand == InteractionHand.MAIN_HAND ? 0 : 3);
                    ServerChunkCache serverchunkcache = ((ServerLevel) player.level()).getChunkSource();
                    serverchunkcache.broadcast(player, clientboundanimatepacket);
                }
            }
        }
    }

    private static int getCurrentSwingDuration(LivingEntity entity) {
        if (MobEffectUtil.hasDigSpeed(entity)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(entity));
        } else {
            return entity.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + entity.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
        }
    }
}
