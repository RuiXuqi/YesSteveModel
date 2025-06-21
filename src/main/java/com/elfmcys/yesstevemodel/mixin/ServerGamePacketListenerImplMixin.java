package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.DispatchServerDrivenProperty;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAbilities(Lnet/minecraft/network/protocol/game/ServerboundPlayerAbilitiesPacket;)V", at=@At("TAIL"))
    private void afterUpdatePlayerAbility(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        NetworkHandler.broadcastToVisiblePlayersAndSelf(new DispatchServerDrivenProperty(player.getId(), packet.isFlying() ? (byte) 1 : (byte) 0), player);
    }
}
