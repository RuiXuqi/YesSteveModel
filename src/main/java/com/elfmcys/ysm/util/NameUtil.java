package com.elfmcys.ysm.util;

import com.elfmcys.ysm.client.model.ClientModel;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

public final class NameUtil {
    public static Component getModeName(ClientModel model, String modelId) {
        String name = model.clientInfo().name();
        Component component;
        if (StringUtils.isBlank(name)) {
            component = Component.literal(modelId);
        } else {
            component = Component.literal(name);
        }
        return component;
    }
}
