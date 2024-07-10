package com.elfmcys.yesstevemodel.client.animation.condition;

import com.elfmcys.yesstevemodel.client.compat.tacz.TACZCompat;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

public class ConditionTAC {
    private static final String EMPTY = "";
    private final ObjectOpenHashSet<String> nameTest = new ObjectOpenHashSet<>();
    private final ObjectOpenHashSet<ResourceLocation> idTest = new ObjectOpenHashSet<>();

    public void addTest(String name) {
        if (!name.startsWith("tac:") || !name.contains("$")) {
            return;
        }
        String[] split = StringUtils.split(name, "$", 2);
        if (split.length < 2) {
            return;
        }
        String itemId = split[1];
        if (ResourceLocation.isValidResourceLocation(itemId)) {
            nameTest.add(name);
            idTest.add(new ResourceLocation(itemId));
        }
    }

    public String doTest(ItemStack itemInHand, String prefix) {
        if (itemInHand.isEmpty()) {
            return EMPTY;
        }
        ResourceLocation gunId = TACZCompat.getGunId(itemInHand);
        if (gunId == null) {
            return EMPTY;
        }
        if (idTest.contains(gunId)) {
            String animationName = prefix.substring(0, prefix.length() - 1) + "$" + gunId;
            if (nameTest.contains(animationName)) {
                return animationName;
            }
        }
        return EMPTY;
    }
}
