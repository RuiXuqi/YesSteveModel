package com.elfmcys.yesstevemodel.client.compat.bettercombat;

import com.elfmcys.yesstevemodel.client.compat.bettercombat.event.PlayerAttackEvent;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.util.PersonView;
import com.elfmcys.yesstevemodel.util.ReflectionUtil;
import com.elfmcys.yesstevemodel.util.UnsafeUtil;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.fml.loading.LoadingModList;

public class BetterCombatCompat {
    private static final String MOD_ID = "bettercombat";
    private static boolean INSTALLED;
    private static long OFFSET_ATTACK_ANIM = -1;

    public static void init() {
        if (LoadingModList.get().getModFileById(MOD_ID) != null) {
            ReflectionUtil.getField(AbstractClientPlayer.class, "attackAnimation", AttackAnimationSubStack.class).ifPresent(field -> {
                OFFSET_ATTACK_ANIM = UnsafeUtil.getUnsafe().objectFieldOffset(field);
            });
            if (OFFSET_ATTACK_ANIM == -1) {
                return;
            }
            BetterCombatClientEvents.ATTACK_START.register(new PlayerAttackEvent());
            INSTALLED = true;
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean shouldSkipRendering(CustomPlayerEntity entity) {
        return INSTALLED && PersonView.isFirstPersonView(entity);
    }

    public static AttackAnimationSubStack getAnimStack(AbstractClientPlayer player) {
        return (AttackAnimationSubStack) UnsafeUtil.getUnsafe().getObject(player, OFFSET_ATTACK_ANIM);
    }
}
