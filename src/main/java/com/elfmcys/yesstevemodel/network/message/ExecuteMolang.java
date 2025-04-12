package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCommonCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExecuteMolang {
    private final int[] entityIds;
    private final String molangExpression;

    public ExecuteMolang(int entityId, String molangExpression) {
        this.entityIds = new int[]{entityId};
        this.molangExpression = molangExpression;
    }

    public ExecuteMolang(int[] entityIds, String molangExpression) {
        this.entityIds = entityIds;
        this.molangExpression = molangExpression;
    }

    public static void encode(ExecuteMolang message, FriendlyByteBuf buf) {
        buf.writeVarIntArray(message.entityIds);
        buf.writeUtf(message.molangExpression);
    }

    public static ExecuteMolang decode(FriendlyByteBuf buf) {
        int[] entityId = buf.readVarIntArray();
        String molangString = buf.readUtf();
        return new ExecuteMolang(entityId, molangString);
    }

    public static void handle(ExecuteMolang message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handlePacket(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handlePacket(final ExecuteMolang message) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        for (int entityId : message.entityIds) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof AbstractClientPlayer player) {
                player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                    try {
                        IValue value = CustomMolangParser.parseSingleExpressionUnsafe(message.molangExpression);
                        cap.executeMolangExp(value, true, null);
                    } catch (ParseException e) {
                        YesSteveModel.LOGGER.error("Failed to execute molang " + message.molangExpression, e);
                    }
                });
            } else if (TlmCommonCompat.isMaid(entity)) {
                TlmCommonCompat.handleExecuteMolang(entity, message.molangExpression);
            }
        }
    }
}
