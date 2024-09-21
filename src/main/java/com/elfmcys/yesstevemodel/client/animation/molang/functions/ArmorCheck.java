package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import org.apache.commons.lang3.StringUtils;

public class ArmorCheck extends LivingEntityFunction {
    private static final String ID_PREFIX = "$";
    private static final String TAG_PREFIX = "#";
    private static final String EMPTY = "empty";
    private static final int FALSE = 0;
    private static final int TRUE = 1;

    public static ArmorCheck armorCheck() {
        return new ArmorCheck();
    }

    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null || !slotType.isArmor()) {
            return null;
        }

        String input = arguments.getAsString(context, 1);
        LivingEntity entity = context.entity().entity();

        if (StringUtils.isBlank(input)) {
            return FALSE;
        }

        ItemStack item = entity.getItemBySlot(slotType);

        // 为空的特殊判断
        if (item.isEmpty() && input.equals(EMPTY)) {
            return TRUE;
        }

        String subInput = input.substring(1);
        if (input.startsWith(ID_PREFIX)) {
            ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item.getItem());
            if (registryName == null) {
                return FALSE;
            }
            boolean equals = subInput.equals(registryName.toString());
            return equals ? TRUE : FALSE;
        }

        if (input.startsWith(TAG_PREFIX)) {
            ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
            if (tags == null) {
                return FALSE;
            }
            ResourceLocation tag = new ResourceLocation(subInput);
            TagKey<Item> tagKey = tags.createTagKey(tag);
            return item.is(tagKey) ? TRUE : FALSE;
        }

        return FALSE;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 2 || size == 3;
    }
}
