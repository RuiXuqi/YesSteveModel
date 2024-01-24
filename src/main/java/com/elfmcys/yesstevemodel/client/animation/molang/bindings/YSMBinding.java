package com.elfmcys.yesstevemodel.client.animation.molang.bindings;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.client.animation.molang.bindings.functions.*;
import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.mixin.client.ArrowEntityAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;

public class YSMBinding extends ContextBinding {
    public static final YSMBinding INSTANCE = new YSMBinding();

    @SuppressWarnings("resource")
    private YSMBinding() {
        function("dump_equipped_item", new DumpEquippedItem());
        function("dump_relative_block", new DumpRelativeBlock());
        var("dump_mods", YSMBinding::dumpMods);
        entityVar("dump_effects", YSMBinding::dumpEffects);
        entityVar("dump_biome", YSMBinding::dumpBiome);

        function("mod_version", new ModVersion());
        function("equipped_enchantment_level", new EquippedEnchantmentLevel());
        function("effect_level", new EffectLevel());
        function("relative_block_name", new RelativeBlockName());

        var("texture_name", ctx -> ctx.geoInstance().getTextureName());
        var("head_yaw", ctx -> ctx.data().netHeadYaw);
        var("head_pitch", ctx -> ctx.data().headPitch);
        var("weather", ctx -> getWeather(ctx.level()));
        var("dimension_name", ctx -> ctx.level().dimension().location().toString());

        entityVar("is_passenger", ctx -> ctx.entity().isPassenger());
        entityVar("is_sleep", ctx -> ctx.entity().getPose() == Pose.SLEEPING);
        entityVar("is_sneak", ctx -> ctx.entity().onGround() && ctx.entity().getPose() == Pose.CROUCHING);
        entityVar("biome_category", ctx -> getBiomeCategory(ctx.entity()));
        entityVar("is_open_air", ctx-> isOpenAir(ctx.entity()));

        livingEntityVar("has_helmet", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.HEAD));
        livingEntityVar("has_chest_plate", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.CHEST));
        livingEntityVar("has_leggings", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.LEGS));
        livingEntityVar("has_boots", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.FEET));
        livingEntityVar("has_mainhand", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.MAINHAND));
        livingEntityVar("has_offhand", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.OFFHAND));
        livingEntityVar("has_elytra", ctx -> ctx.entity().getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA);
        livingEntityVar("is_riptide", ctx -> ctx.entity().isAutoSpinAttack());
        livingEntityVar("armor_value", ctx -> ctx.entity().getArmorValue());
        livingEntityVar("hurt_time", ctx -> ctx.entity().hurtTime);
        livingEntityVar("is_close_eyes", ctx -> getEyeCloseState(ctx.animationEvent(), ctx.entity()));

        playerVar("elytra_rot_x", ctx -> Math.toDegrees(ctx.entity().elytraRotX));
        playerVar("elytra_rot_y", ctx -> Math.toDegrees(ctx.entity().elytraRotY));
        playerVar("elytra_rot_z", ctx -> Math.toDegrees(ctx.entity().elytraRotZ));
        playerVar("food_level", ctx -> ctx.entity().getFoodData().getFoodLevel());    // 之前默认值是 2
        if (FirstPersonCompat.isInstalled()) {
            playerVar("first_person_mod_hide", ctx -> {
                if (ctx.entity() instanceof LocalPlayer) {
                    return FirstPersonCompat.shouldHideHead();
                } else {
                    return false;
                }
            });
        } else {
            constValue("first_person_mod_hide", 0f);
        }

        abstractArrowVar("on_ground_time", ctx -> ((IArrowExtraInfo) ctx.entity()).inGroundTime());
        abstractArrowVar("in_ground", ctx -> ((IArrowExtraInfo) ctx.entity()).isInGround());
        abstractArrowVar("projectile_owner", ctx -> ctx.createChild(ctx.entity().getOwner()));
        abstractArrowVar("delta_movement_length", ctx -> ctx.entity().getDeltaMovement().length());
        abstractArrowVar("is_spectral_arrow", ctx-> ctx.entity() instanceof SpectralArrow);
    }

    private static boolean getEyeCloseState(AnimationEvent<?> animationEvent, LivingEntity player) {
        double remainder = (animationEvent.getAnimationTick() + Math.abs(player.getUUID().getLeastSignificantBits()) % 10) % 90;
        boolean isBlinkTime = 85 < remainder && remainder < 90;
        return player.isSleeping() || isBlinkTime;
    }

    private static boolean getSlotValue(LivingEntity player, EquipmentSlot slot) {
        return !player.getItemBySlot(slot).isEmpty();
    }

    private static int getWeather(ClientLevel world) {
        if (world.isThundering()) {
            return 2;
        } else if (world.isRaining()) {
            return 1;
        }
        return 0;
    }

    private String getBiomeCategory(Entity entity) {
        return null;
    }

    private static Object dumpMods(IContext<?> context) {
        if (!context.isDebugEnabled()) {
            return null;
        }

        ModList.get().getMods().stream().sorted(Comparator.comparing(IModInfo::getDisplayName)).forEach(mod -> {
            context.debugPrint("Mod: display='%s' id='%s'", mod.getDisplayName(), mod.getModId());
        });
        return null;
    }

    private static Object dumpEffects(IContext<Entity> context) {
        if (!context.isDebugEnabled()) {
            return null;
        }

        if (context.entity() instanceof Arrow) {
            for (MobEffectInstance instance : ((ArrowEntityAccessor) context.entity()).getEffects()) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
                context.debugPrint("Effect: display='%s' name='%s' lv=%s",
                        instance.getEffect().getDisplayName().getString(99), id, instance.getAmplifier());
            }
        } else if (context.entity() instanceof LivingEntity) {
            for (MobEffectInstance instance : ((LivingEntity) context.entity()).getActiveEffects()) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
                context.debugPrint("Effect: display='%s' name='%s' lv=%s",
                        instance.getEffect().getDisplayName().getString(99), id, instance.getAmplifier());
            }
        }

        return null;
    }

    private static Object dumpBiome(IContext<Entity> context) {
        if (!context.isDebugEnabled()) {
            return null;
        }

        Holder<Biome> biome = context.entity().level().getBiome(context.entity().blockPosition());


        biome.unwrapKey().ifPresent(p -> {
            context.debugPrint("Name: '%s'", p.location());
        });
        biome.tags().forEach(tag -> {
            context.debugPrint("Tag: '%s'", tag.location());
        });

        return null;
    }

    private static boolean isOpenAir(Entity entity) {
        BlockPos blockpos = entity.blockPosition();
        if (!entity.level().canSeeSky(blockpos)) {
            return false;
        }
        if (entity.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > blockpos.getY()) {
            return false;
        }
        return true;
    }
}
