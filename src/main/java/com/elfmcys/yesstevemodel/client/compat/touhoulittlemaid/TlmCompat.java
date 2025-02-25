package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.GeoMaidAnimatedRegister;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.molang.TLMBindingInner;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.animation.predicate.MaidVehiclePredicate;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.input.OpenRouletteScreen;
import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
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

    public static boolean isSit(Entity entity) {
        return isInstalled() && TlmCompatInner.isSit(entity);
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

    public static void addBinding(TLMBinding binding) {
        if (isInstalled()) {
            TLMBindingInner.addInnerBinding(binding);
        } else {
            addEmptyBinding(binding);
        }
    }

    /**
     * 没有安装此模组时，这些 molang 应该存在，否则会报错
     */
    private static void addEmptyBinding(TLMBinding binding) {
        binding.livingEntityVar("is_begging", ctx -> false);
        binding.livingEntityVar("is_sitting", ctx -> false);
        binding.livingEntityVar("has_backpack", ctx -> false);
        binding.livingEntityVar("favorability_point", ctx -> 0);
        binding.livingEntityVar("favorability_level", ctx -> 0);
        binding.livingEntityVar("task_id", ctx -> StringUtils.EMPTY);
        binding.livingEntityVar("schedule", ctx -> StringUtils.EMPTY);
        binding.livingEntityVar("activity", ctx -> StringUtils.EMPTY);
        binding.livingEntityVar("gomoku_win_count", ctx -> 0);
        binding.livingEntityVar("gomoku_rank", ctx -> 1);
        binding.livingEntityVar("game_statue", ctx -> StringUtils.EMPTY);
        binding.livingEntityVar("backpack_type", ctx -> StringUtils.EMPTY);
        binding.livingEntityVar("is_entity", ctx -> true);
        binding.livingEntityVar("is_statue", ctx -> false);
        binding.livingEntityVar("is_garage_kit", ctx -> false);
        binding.livingEntityVar("show_item", ctx -> StringUtils.EMPTY);
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

    public static boolean pointToMaid() {
        return isInstalled() && OpenRouletteScreen.pointToMaid();
    }

    public static void onRouletteMainKeyPressed() {
        if (isInstalled()) {
            OpenRouletteScreen.onRouletteMainKeyPressed();
        }
    }
}
