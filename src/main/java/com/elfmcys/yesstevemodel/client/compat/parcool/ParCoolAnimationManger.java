package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.common.capability.Animation;
import com.elfmcys.yesstevemodel.mixin.client.AnimationMixin;
import com.google.common.collect.Maps;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ParCoolAnimationManger {
    private static final HashMap<Class<? extends Animator>, String> INDEX_MAP = Maps.newHashMap();

    // 跑酷模组没有给这些动画命名，所以我们手动给命名吧
    static String getAnimationName(Animator animator) {
        return INDEX_MAP.computeIfAbsent(animator.getClass(), clz -> getAnimationNameFromClassName(clz.getSimpleName()));
    }

    @Nullable
    static String getAnimation(Player player) {
        Animation animation = Animation.get(player);
        if (animation.hasAnimator()) {
            return getAnimationName(((AnimationMixin) animation).getAnimator());
        }
        return null;
    }

    static String getAnimationNameFromClassName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        if (name.endsWith("Animator")) {
            name = name.substring(0, name.length() - "Animator".length());
        }
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        sb.insert(0, "parcool:");
        return sb.toString();
    }
}
