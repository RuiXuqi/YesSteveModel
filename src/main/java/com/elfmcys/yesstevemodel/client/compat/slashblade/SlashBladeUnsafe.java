package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.util.UnsafeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import org.apache.commons.lang3.StringUtils;

public class SlashBladeUnsafe {
    private static long FIELD_OFFSET = -1;

    static void initFiledOffset() {
        try {
            FIELD_OFFSET = UnsafeUtil.getUnsafe().objectFieldOffset(SlashBladeState.class.getDeclaredField("comboSeq"));
        } catch (NoSuchFieldException e) {
            e.fillInStackTrace();
        }
    }

    static String getOldComboStateName(SlashBladeState state, long time) {
        Object object = UnsafeUtil.getUnsafe().getObject(state, FIELD_OFFSET);
        if (object instanceof mods.flammpfeil.slashblade.capability.slashblade.ComboState comboState) {
            int timeout = comboState.getTimeoutMS();
            if (time > timeout) {
                return StringUtils.EMPTY;
            }
            String name = comboState.getName();
            if (name.startsWith("ex_")) {
                name = name.substring(3);
            }
            return "slashblade:" + name;
        }
        return StringUtils.EMPTY;
    }
}
