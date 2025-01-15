package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationBuilder;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.google.common.collect.Maps;
import mods.flammpfeil.slashblade.capability.slashblade.CapabilitySlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class SlashBladeAnimation {
    /**
     * 未来兼容新旧两版拔刀剑，部分不一致的动画名在这里归一化
     */
    private static final Map<String, String> NAME_FIX = Maps.newHashMap();

    static {
        NAME_FIX.put("slashblade:combo_a4_ex", "slashblade:combo_a4ex");
    }

    static String getAnimationName(AnimationEvent<CustomPlayerEntity> event) {
        Player player = event.getAnimatableEntity().getEntity();
        return getCombName(player.getMainHandItem(), player.level());
    }

    static String getAnimationName(IContext<AbstractClientPlayer> context) {
        AbstractClientPlayer player = context.entity();
        return getCombName(player.getMainHandItem(), player.level());
    }

    /**
     * slashblade:idle
     * slashblade:run
     * slashblade:walk
     */
    static PlayState playMainAnimation(AnimationEvent<CustomPlayerEntity> event, String animationName, ILoopType loopType) {
        String name = "slashblade:" + animationName;
        String modelId = event.getAnimatableEntity().getModelId();
        Optional<Animation> playerAnimation = ClientModelManager.getPlayerAnimation(modelId, name);
        if (playerAnimation.isPresent()) {
            return playAnimation(event, name, loopType);
        }
        return playAnimation(event, animationName, loopType);
    }

    @NotNull
    private static String getCombName(ItemStack mainHandItem, Level level) {
        if (!(mainHandItem.getItem() instanceof ItemSlashBlade)) {
            return StringUtils.EMPTY;
        }
        return mainHandItem.getCapability(CapabilitySlashBlade.BLADESTATE).map(bladeState -> {
            long time = (level.getGameTime() - bladeState.getLastActionTime()) * 50;
            ResourceLocation id = bladeState.getComboSeq();
            ComboState comboSeq = ComboStateRegistry.REGISTRY.get().getValue(id);
            if (comboSeq == null) {
                return StringUtils.EMPTY;
            }
            int timeout = comboSeq.getTimeoutMS();
            if (time <= timeout) {
                return nameFix(id.toString());
            }
            return StringUtils.EMPTY;
        }).orElse(StringUtils.EMPTY);
    }

    private static String nameFix(String rawName) {
        if (NAME_FIX.containsKey(rawName)) {
            return NAME_FIX.get(rawName);
        }
        return rawName;
    }

    @NotNull
    private static PlayState playAnimation(AnimationEvent<?> event, String animationName, ILoopType loopType) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animationName, loopType));
        return PlayState.CONTINUE;
    }
}
