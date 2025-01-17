package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.google.common.collect.Maps;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;


public class SlashBladeResharped {
    /**
     * 兼容新旧两版拔刀剑，部分不一致的动画名在这里归一化
     */
    private static final Map<String, String> NAME_FIX = Maps.newHashMap();

    static {
        NAME_FIX.put("slashblade:combo_a4_ex", "slashblade:combo_a4ex");
    }

    static String getResharpedComboStateName(ISlashBladeState bladeState, long time) {
        ResourceLocation id = bladeState.getComboSeq();
        ComboState comboSeq = ComboStateRegistry.REGISTRY.get().getValue(id);
        if (comboSeq == null) {
            return StringUtils.EMPTY;
        }
        int timeout = comboSeq.getTimeoutMS();
        // standby 的剑技时间比动画还长，会有问题，需要和动画时长对齐
        if ("slashblade:standby".equals(id.toString())) {
            timeout -= 553;
        }
        if (time <= timeout) {
            return nameFix(id.toString());
        }
        return StringUtils.EMPTY;
    }

    private static String nameFix(String rawName) {
        if (NAME_FIX.containsKey(rawName)) {
            return NAME_FIX.get(rawName);
        }
        return rawName;
    }
}
