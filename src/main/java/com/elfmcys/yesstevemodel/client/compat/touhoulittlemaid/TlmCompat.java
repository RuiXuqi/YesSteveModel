package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.GeoMaidAnimatedRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

@OnlyIn(Dist.CLIENT)
public class TlmCompat {
    private static final String MOD_ID = "touhou_little_maid";
    private static final VersionRange VERSION_RANGE;
    private static boolean INSTALLED = false;

    static {
        try {
            VERSION_RANGE = VersionRange.createFromVersionSpec("[1.1.15,)");
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
        ModList.get().getModContainerById(MOD_ID).ifPresent(modContainer -> {
            ArtifactVersion version = modContainer.getModInfo().getVersion();
            if (VERSION_RANGE.containsVersion(version)) {
                INSTALLED = true;
            } else {
                // 开发环境下，version 是空的，所以需要额外判断
                INSTALLED = !FMLEnvironment.production;
            }
            if (INSTALLED) {
                TlmCompatInner.registerEvent();
                // 使用事件
                TlmCompatInner.registerYsmEntityMaidRenderer();
                // 注册主动画
                GeoMaidAnimatedRegister.registerAnimationState();
            }
        });
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isMaid(LivingEntity entity) {
        return isInstalled() && TlmCompatInner.isMaid(entity);
    }

    public static boolean isMaidFishing(LivingEntity entity) {
        return isInstalled() && TlmCompatInner.maidIsFishing(entity);
    }

    public static void markTacGunAnimationNeedReload(LivingEntity entity) {
        if (isInstalled()) {
            TlmCompatInner.markTacGunAnimationNeedReload(entity);
        }
    }
}
