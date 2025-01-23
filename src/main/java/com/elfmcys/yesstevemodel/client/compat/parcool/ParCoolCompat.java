package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.HybridAnimationController;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;

import static com.elfmcys.yesstevemodel.util.ControllerUtils.PARCOOL_CONTROLLER;

public class ParCoolCompat {
    private static final String MOD_ID = "parcool";
    private static boolean INSTALLED;
    private static boolean VERSION_3_3_1_0 = false;

    public static void init() {
        ModFileInfo modFileById = LoadingModList.get().getModFileById(MOD_ID);
        INSTALLED = modFileById != null;
        if (INSTALLED) {
            DefaultArtifactVersion modVersion = new DefaultArtifactVersion(modFileById.versionString());
            VERSION_3_3_1_0 = modVersion.compareTo(new DefaultArtifactVersion("3.3.1.0")) >= 0;
            ParCoolUnsafe.initFiledOffset();
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isVersion3310() {
        return VERSION_3_3_1_0;
    }

    public static void addParcoolPredicate(CustomPlayerEntity entity) {
        if (INSTALLED) {
            entity.addAnimationController(new HybridAnimationController<>(entity, PARCOOL_CONTROLLER, 2, new ParCoolPredicate()));
        }
    }

    public static boolean hasAnimation(Player player) {
        if (isInstalled()) {
            return ParCoolAnimationManger.hasAnimation(player);
        }
        return false;
    }

    @Nullable
    public static String getAnimation(Player player) {
        if (isInstalled()) {
            return ParCoolAnimationManger.getAnimation(player);
        }
        return null;
    }

    public static void addBinding(CtrlBinding binding) {
        if (isInstalled()) {
            ParCoolCtrlBinding.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(CtrlBinding binding) {
        binding.playerVar("parcool_state", ctx -> StringUtils.EMPTY);
    }
}
