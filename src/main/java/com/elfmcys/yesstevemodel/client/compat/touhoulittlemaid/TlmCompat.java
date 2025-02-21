package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.GeoMaidAnimatedRegister;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.MaidVehiclePredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.TLMBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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

    public static boolean isMaid(Entity entity) {
        return isInstalled() && TlmCompatInner.isMaid(entity);
    }

    public static boolean isChair(Entity entity) {
        return isInstalled() && TlmCompatInner.isChair(entity);
    }

    public static boolean isGohei(Item item) {
        return isInstalled() && TlmCompatInner.isGohei(item);
    }

    public static String getChairId(Entity entity) {
        return isInstalled() ? TlmCompatInner.getChairId(entity) : StringUtils.EMPTY;
    }

    public static boolean isMaidFishing(LivingEntity entity) {
        return isInstalled() && TlmCompatInner.maidIsFishing(entity);
    }

    public static void addMolangParser(Map<String, ObjectBinding> bindingMap) {
        if (isInstalled()) {
            bindingMap.put("tlm", TLMBinding.INSTANCE);
        }
    }

    @Nullable
    public static PlayState getMaidVehicleAnimation(AnimationEvent<AnimatableEntity<? extends LivingEntity>> event, LivingEntity entity, Entity vehicle) {
        if (isInstalled()) {
            return MaidVehiclePredicate.getMaidVehicleAnimation(event, entity, vehicle);
        }
        return null;
    }

    public static void markTacGunAnimationNeedReload(LivingEntity entity) {
        if (isInstalled()) {
            TlmCompatInner.markTacGunAnimationNeedReload(entity);
        }
    }

    public static SoundBuffer getSoundBuffer(Entity maid, String soundPath) {
        if (isInstalled()) {
            return TlmCompatInner.getSoundBuffer(maid, soundPath);
        }
        return null;
    }
}
