package com.elfmcys.yesstevemodel.client.animation.condition;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCompat;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ConditionalChair {
    private static final String EMPTY = "";
    private final ObjectOpenHashSet<String> idTest = new ObjectOpenHashSet<>();
    private final String idPre;

    public ConditionalChair() {
        this.idPre = "chair$";
    }

    public void addTest(String name) {
        int preSize = this.idPre.length();
        if (name.length() <= preSize) {
            return;
        }
        String substring = name.substring(preSize);
        if (name.startsWith(idPre) && ResourceLocation.isValidResourceLocation(substring)) {
            idTest.add(substring);
        }
    }

    public String doTest(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (TlmCompat.isChair(vehicle)) {
            return doIdTest(vehicle);
        }
        return EMPTY;
    }

    private String doIdTest(Entity entity) {
        if (idTest.isEmpty()) {
            return EMPTY;
        }
        String modelId = TlmCompat.getChairId(entity);
        if (idTest.contains(modelId)) {
            return idPre + modelId;
        }
        return EMPTY;
    }
}
