package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.client.animation.condition.InnerClassify;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class HandItemCheck extends LivingEntityFunction {
    private static final String ID_PREFIX = "$";
    private static final String TAG_PREFIX = "#";
    private static final String EXTRA_PREFIX = ":";
    private static final String EMPTY = "empty";
    private static final int FALSE = 0;
    private static final int TRUE = 1;

    private final Condition condition;

    private HandItemCheck(Condition condition) {
        this.condition = condition;
    }

    public static HandItemCheck holdCheck() {
        return new HandItemCheck((entity, hand) -> {
            if (entity.swinging && entity.swingingArm == hand) {
                return false;
            }
            return !entity.isUsingItem() || entity.getUsedItemHand() != hand;
        });
    }

    /**
     * 和前一个 hold 基本相同，但是不检测挥动或者使用状态，一直执行
     */
    public static HandItemCheck hold2Check() {
        return new HandItemCheck((entity, hand) -> true);
    }

    public static HandItemCheck swingCheck() {
        return new HandItemCheck((entity, hand) -> entity.swinging && !entity.isSleeping());
    }

    public static HandItemCheck useCheck() {
        return new HandItemCheck((entity, hand) -> entity.isUsingItem() && !entity.isSleeping());
    }

    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null || slotType.isArmor()) {
            return FALSE;
        }

        String input = arguments.getAsString(context, 1);
        LivingEntity entity = context.entity().entity();

        if (StringUtils.isBlank(input)) {
            return FALSE;
        }

        ItemStack item = entity.getItemBySlot(slotType);
        InteractionHand hand = slotType == EquipmentSlot.OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        if (!this.condition.test(entity, hand)) {
            return FALSE;
        }

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

        if (input.startsWith(EXTRA_PREFIX)) {
            String innerName = InnerClassify.getClassify(item);
            if (StringUtils.isNotBlank(innerName) && innerName.equals(subInput)) {
                return TRUE;
            }
            String anim = item.getUseAnimation().name().toLowerCase(Locale.ENGLISH);
            if (anim.equals(subInput)) {
                return TRUE;
            }
            return FALSE;
        }

        return FALSE;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 2 || size == 3;
    }

    private interface Condition {
        boolean test(LivingEntity entity, InteractionHand hand);
    }
}
