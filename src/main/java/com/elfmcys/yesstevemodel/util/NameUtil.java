package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.model.ClientModel;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

public final class NameUtil {
    public static Component getModeName(ClientModel model, String modelId) {
        String name = model.clientModelInfo().name();
        Component component;
        if (StringUtils.isBlank(name)) {
            component = Component.literal(modelId);
        } else {
            component = Component.literal(name);
        }
        return component;
    }
}
