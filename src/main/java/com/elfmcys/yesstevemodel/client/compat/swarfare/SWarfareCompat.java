package com.elfmcys.yesstevemodel.client.compat.swarfare;

import com.elfmcys.yesstevemodel.geckolib3.core.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.LoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class SWarfareCompat {
    private static final String MOD_ID = "superbwarfare";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
        if (INSTALLED) {
            MinecraftForge.EVENT_BUS.register(new ReplacePlayerArmRender());
        }
    }

    public static boolean isGun(ItemStack stack) {
        if (INSTALLED) {
            return SWarfareCompatInner.isGun(stack);
        }
        return false;
    }

    public static void renderOffsetHand(ItemStack offhandItem, GeoModelState geoModel, LivingEntity livingEntity, PoseStack poseStack, int packedLight, float partialTicks) {
        if (INSTALLED && SWarfareCompatInner.isGun(offhandItem)) {
            poseStack.pushPose();
            SWarfareCompatInner.renderOffhandGun(offhandItem, geoModel, livingEntity, poseStack, packedLight, partialTicks);
            poseStack.popPose();
        }
    }

    @Nullable
    public static PlayState playGunMainAnimation(LivingEntity livingEntity,
                                                 AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event,
                                                 String animationName, LoopType loopType) {
        if (INSTALLED && SWarfareCompatInner.isGun(livingEntity.getMainHandItem())) {
            return SWarfareCompatInner.playGunMainAnimation(event, animationName, loopType);
        }
        return null;
    }

    @Nullable
    public static PlayState playGunHoldAnimation(ItemStack mainHandItem,
                                                 AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event) {
        if (INSTALLED && SWarfareCompatInner.isGun(mainHandItem)) {
            return SWarfareCompatInner.playGunHoldAnimation(event, mainHandItem);
        }
        return null;
    }

    @Nullable
    public static PlayState playGunOnceAnimation(ItemStack mainHandItem, AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event) {
        if (INSTALLED && SWarfareCompatInner.isGun(mainHandItem)) {
            return SWarfareCompatInner.playGunOnceAnimation(event, mainHandItem);
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getGunId(ItemStack stack) {
        if (INSTALLED) {
            return ForgeRegistries.ITEMS.getKey(stack.getItem());
        }
        return null;
    }
}
