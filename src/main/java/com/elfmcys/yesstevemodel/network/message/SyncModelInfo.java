package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.event.EntityLoadEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncModelInfo {
    private final int entityId;
    private final String modelId;
    private final int modelHashShort;
    private final String selectTexture;
    private final String animation;
    private final boolean playAnimation;
    private final Object2FloatOpenHashMap<String> molangVarsServerBound;
    private final Int2FloatOpenHashMap molangVarsClientBound;

    private final DispatchServerDrivenProperty properties;

    public SyncModelInfo(int entityId, String modelId, int modelHashShort, String selectTexture, String animation, boolean playAnimation, Object2FloatOpenHashMap<String> molangVarsServerBound, Int2FloatOpenHashMap molangVarsClientBound, DispatchServerDrivenProperty properties) {
        this.entityId = entityId;
        this.modelId = modelId;
        this.modelHashShort = modelHashShort;
        this.selectTexture = selectTexture;
        this.animation = animation;
        this.playAnimation = playAnimation;
        this.molangVarsServerBound = molangVarsServerBound;
        this.molangVarsClientBound = molangVarsClientBound;
        this.properties = properties;
    }

    public static void encode(SyncModelInfo msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.modelId);
        buf.writeInt(msg.modelHashShort);
        buf.writeUtf(msg.selectTexture);
        buf.writeUtf(msg.animation);
        buf.writeBoolean(msg.playAnimation);
        buf.writeVarInt(msg.molangVarsServerBound.size());
        for (var entry : msg.molangVarsServerBound.object2FloatEntrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getFloatValue());
        }
        DispatchServerDrivenProperty.encode(msg.properties, buf);
    }

    public static SyncModelInfo decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String modelId = buf.readUtf();
        int modelHashShort = buf.readInt();
        String selectTexture = buf.readUtf();
        String animation = buf.readUtf();
        boolean playAnimation = buf.readBoolean();
        int size = buf.readVarInt();
        Int2FloatOpenHashMap molangVars = new Int2FloatOpenHashMap();
        for (int i = 0; i < size; i++) {
            int key = StringPool.computeIfAbsent(buf.readUtf());
            float value = buf.readFloat();
            molangVars.put(key, value);
        }
        var properties = DispatchServerDrivenProperty.decode(buf);
        return new SyncModelInfo(entityId, modelId, modelHashShort, selectTexture, animation, playAnimation, null, molangVars, properties);
    }

    public static void handle(SyncModelInfo message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            EntityLoadEvent.executeOnEntity(message.entityId,  entity -> handleCapability(entity, message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleCapability(Entity entity, SyncModelInfo msg) {
        entity.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
            cap.setModelAndTexture(msg.modelId, msg.selectTexture);
            cap.resetRoamingVars(msg.modelHashShort, msg.molangVarsClientBound);
            DispatchServerDrivenProperty.handle(entity, msg.properties);
            if (msg.playAnimation) {
                cap.playExtraAnimation(msg.animation);
            } else {
                cap.stopExtraAnimation();
            }
        });
    }
}
