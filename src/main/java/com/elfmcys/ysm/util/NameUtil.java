package com.elfmcys.ysm.util;

import com.elfmcys.ysm.client.model.ModelRenderTarget;
import net.minecraft.network.chat.Component;

public final class NameUtil {
    public static Component getModeName(ModelRenderTarget model, String modelId) {
        return Component.literal(model.getDisplayName(modelId));
    }
}
