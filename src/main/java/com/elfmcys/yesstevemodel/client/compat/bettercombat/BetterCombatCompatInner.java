package com.elfmcys.yesstevemodel.client.compat.bettercombat;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.event.PlayerAttackEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.util.ReflectionUtil;
import com.elfmcys.yesstevemodel.util.UnsafeUtil;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.minecraft.client.player.AbstractClientPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

public class BetterCombatCompatInner {
    private static long OFFSET_ATTACK_ANIM = -1;

    static void innerInit() {
        ReflectionUtil.getField(AbstractClientPlayer.class, "attackAnimation", AttackAnimationSubStack.class).ifPresent(field -> {
            OFFSET_ATTACK_ANIM = UnsafeUtil.getUnsafe().objectFieldOffset(field);
        });
        if (OFFSET_ATTACK_ANIM == -1) {
            return;
        }
        BetterCombatClientEvents.ATTACK_START.register(new PlayerAttackEvent());
    }

    static void addInnerBinding(CtrlBinding binding) {
        binding.clientPlayerVar("bcombat_attack_animation", BetterCombatCompatInner::getAttackAnimation);
    }

    private static AttackAnimationSubStack getAnimStack(AbstractClientPlayer player) {
        return (AttackAnimationSubStack) UnsafeUtil.getUnsafe().getObject(player, OFFSET_ATTACK_ANIM);
    }

    private static String getAttackAnimation(IContext<AbstractClientPlayer> context) {
        AttackAnimationSubStack animStack = getAnimStack(context.entity());
        if (animStack == null) {
            return StringUtils.EMPTY;
        }
        IAnimation animation = animStack.base.getAnimation();
        if (animation == null) {
            return StringUtils.EMPTY;
        }
        if (!animation.isActive()) {
            return StringUtils.EMPTY;
        }
        if (!(animation instanceof KeyframeAnimationPlayer keyframe)) {
            return StringUtils.EMPTY;
        }
        HashMap<String, Object> extraData = keyframe.getData().extraData;
        Object name = extraData.getOrDefault("name", StringUtils.EMPTY);
        if (!(name instanceof String strName)) {
            return StringUtils.EMPTY;
        }
        return strName;
    }
}
