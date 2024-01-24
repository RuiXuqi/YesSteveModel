package com.elfmcys.yesstevemodel.geckolib3.util;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class MolangUtils {
    private static final HashMap<String, EquipmentSlot> SLOT_MAP;

    static {
        SLOT_MAP = new HashMap<>();
        SLOT_MAP.put("Chest", EquipmentSlot.CHEST);
        SLOT_MAP.put("Feet", EquipmentSlot.FEET);
        SLOT_MAP.put("Head", EquipmentSlot.HEAD);
        SLOT_MAP.put("Legs", EquipmentSlot.LEGS);
        SLOT_MAP.put("Mainhand", EquipmentSlot.MAINHAND);
        SLOT_MAP.put("Offhand", EquipmentSlot.OFFHAND);
    }

    public static float normalizeTime(long timestamp) {
        return ((float) (timestamp + 6000L) / 24000) % 1;
    }

    public static ResourceLocation parseResourceLocation(IContext<?> context, String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            context.debugPrint("Illegal resource location: %s", value);
            return null;
        }
        return id;
    }

    public static EquipmentSlot parseSlotType(IContext<?> context, String value) {
        if (value == null) {
            return null;
        }
        EquipmentSlot slot = SLOT_MAP.get(value);
        if (slot == null) {
            context.debugPrint("Unknown slot type: %s.", value);
            return null;
        }
        return slot;
    }
}
