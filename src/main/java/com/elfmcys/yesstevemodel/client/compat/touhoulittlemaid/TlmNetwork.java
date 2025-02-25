package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.network.message.SubmitVariableChanges;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

/**
 * 这个类是客户端和服务端都可能用到的类
 */
public class TlmNetwork {
    private static final String MOD_ID = "touhou_little_maid";

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isMaid(Entity entity) {
        if (isInstalled()) {
            return TlmNetworkInner.isMaid(entity);
        }
        return false;
    }

    public static void setRouletteAnim(Entity entity, String classifyId, int extraAnimIndex) {
        if (isInstalled()) {
            TlmNetworkInner.setRouletteAnima(entity, classifyId, extraAnimIndex);
        }
    }

    public static void handleVariableChanges(Entity entity, SubmitVariableChanges message) {
        if (isInstalled()) {
            TlmNetworkInner.handleVariableChanges(entity, message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleExecuteMolang(Entity entity, String molangExpression) {
        if (isInstalled()) {
            TlmNetworkInner.handleExecuteMolang(entity, molangExpression);
        }
    }
}
