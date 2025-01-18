package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeAnimation.isSlashBlade;

public class SlashBladeCompat {
    private static final String SLASH_BLADE_ID = "slashblade";
    private static boolean IS_LOADED = false;
    private static boolean IS_RESHARPED = false;

    public static void init() {
        ModList.get().getModContainerById(SLASH_BLADE_ID).ifPresent(modContainer -> {
            IS_LOADED = true;
            try {
                ArtifactVersion modVersion = modContainer.getModInfo().getVersion();
                // 旧版拔刀剑最后是 0.1.2 版本
                VersionRange versionRange = VersionRange.createFromVersionSpec("(,0.1.2]");
                IS_RESHARPED = !versionRange.containsVersion(modVersion);
            } catch (InvalidVersionSpecificationException e) {
                e.fillInStackTrace();
            }
            if (!IS_RESHARPED) {
                SlashBladeUnsafe.initFiledOffset();
            }
        });
    }

    public static boolean isSlashBladeLoaded() {
        return IS_LOADED;
    }

    public static boolean isSlashBladeItem(ItemStack stack) {
        return isSlashBladeLoaded() && isSlashBlade(stack);
    }

    public static String getAnimationName(AnimationEvent<CustomPlayerEntity> event) {
        if (isSlashBladeLoaded()) {
            return SlashBladeAnimation.getAnimationName(event);
        }
        return StringUtils.EMPTY;
    }

    @Nullable
    public static PlayState playMainAnimation(Player player, AnimationEvent<CustomPlayerEntity> event, String animationName, ILoopType loopType) {
        if (isSlashBladeLoaded() && isSlashBladeItem(player.getMainHandItem())) {
            return SlashBladeAnimation.playMainAnimation(event, animationName, loopType);
        }
        return null;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isSlashBladeLoaded()) {
            SlashBladeBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    public static boolean isResharped() {
        return IS_RESHARPED;
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.playerVar("slashblade_animation", ctx -> StringUtils.EMPTY);
    }
}
