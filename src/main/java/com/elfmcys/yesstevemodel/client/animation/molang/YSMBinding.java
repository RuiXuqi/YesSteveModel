package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.api.IArrowExtraInfo;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.*;
import com.elfmcys.yesstevemodel.client.animation.molang.variable.FirstPersonModHideVariable;
import com.elfmcys.yesstevemodel.client.animation.molang.variable.LadderFacingVariable;
import com.elfmcys.yesstevemodel.client.animation.molang.variable.MoveInputVariable;
import com.elfmcys.yesstevemodel.client.animation.molang.variable.TextureNameVariable;
import com.elfmcys.yesstevemodel.client.compat.curios.CuriosCompat;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.client.TlmClientCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.mixin.client.ArrowEntityAccessor;
import com.elfmcys.yesstevemodel.mixin.client.FishingHookAccessor;
import com.elfmcys.yesstevemodel.mixin.client.ThrowableItemProjectileAccessor;
import com.elfmcys.yesstevemodel.util.EquipmentUtil;
import com.elfmcys.yesstevemodel.util.LazyValue;
import com.elfmcys.yesstevemodel.util.PersonView;
import com.elfmcys.yesstevemodel.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

public class YSMBinding extends ContextBinding {
    public static final LazyValue<YSMBinding> INSTANCE = new LazyValue<>(YSMBinding::new);

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
        function("relative_block_name_any", new RelativeBlockNameAny());

        function("bone_rot", new BoneRotation());
        function("bone_pos", new BonePosition());
        function("bone_scale", new BoneScale());
        function("bone_pivot_abs", new BoneAbsolutePivot());

        var("head_yaw", ctx -> ctx.data().netHeadYaw);
        var("head_pitch", ctx -> ctx.data().headPitch);
        var("weather", ctx -> getWeather(ctx.level()));
        var("dimension_name", ctx -> ctx.level().dimension().location().toString());
        var("fps", ctx -> Minecraft.getInstance().getFps());
        var("time_delta", ctx -> ctx.animatableEntity().getStateTracker().getRenderTickDelta() / 20);

        var("hit_target_id", YSMBinding::getHitId);
        var("hit_target_type", YSMBinding::getHitType);

        entityVar("ground_speed2", YSMBinding::getGroundSpeed2);

        entityVar("input_vertical", MoveInputVariable::getVertical);
        entityVar("input_horizontal", MoveInputVariable::getHorizontal);
        entityVar("person_view", PersonView::getPersonView);
        entityVar("rendering_in_paperdoll", ctx -> RenderUtil.isRenderingInPaperDoll());
        entityVar("rendering_in_inventory", PersonView::isInInventory);
        entityVar("block_light", ctx -> ctx.level().getBrightness(LightLayer.BLOCK, ctx.entity().blockPosition()));
        entityVar("sky_light", ctx -> ctx.level().getBrightness(LightLayer.SKY, ctx.entity().blockPosition()));

        entityVar("is_passenger", ctx -> ctx.entity().isPassenger());
        entityVar("is_sleep", ctx -> ctx.entity().getPose() == Pose.SLEEPING);
        entityVar("is_sneak", ctx -> ctx.entity().onGround() && ctx.entity().getPose() == Pose.CROUCHING);
        entityVar("biome_category", ctx -> getBiomeCategory(ctx.entity()));
        entityVar("is_open_air", ctx -> isOpenAir(ctx.entity()));
        entityVar("eye_in_water", ctx -> ctx.entity().isUnderWater());
        entityVar("frozen_ticks", ctx -> ctx.entity().getTicksFrozen());
        entityVar("air_supply", ctx -> ctx.entity().getAirSupply());
        entityVar("delta_movement_length", ctx -> ctx.entity().getDeltaMovement().length());

        livingEntityVar("has_helmet", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.HEAD));
        livingEntityVar("has_chest_plate", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.CHEST));
        livingEntityVar("has_leggings", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.LEGS));
        livingEntityVar("has_boots", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.FEET));
        livingEntityVar("has_mainhand", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.MAINHAND));
        livingEntityVar("has_offhand", ctx -> getSlotValue(ctx.entity(), EquipmentSlot.OFFHAND));
        livingEntityVar("has_elytra", ctx -> !EquipmentUtil.getEquippedElytraItem(ctx.entity()).isEmpty());
        livingEntityVar("is_riptide", ctx -> ctx.entity().isAutoSpinAttack());
        livingEntityVar("armor_value", ctx -> ctx.entity().getArmorValue());
        livingEntityVar("hurt_time", ctx -> ctx.entity().hurtTime);
        livingEntityVar("is_close_eyes", ctx -> getEyeCloseState(ctx.animationEvent(), ctx.entity()));
        livingEntityVar("on_ladder", ctx -> ctx.entity().onClimbable());
        livingEntityVar("ladder_facing", new LadderFacingVariable());
        livingEntityVar("arrow_count", ctx -> ctx.entity().getArrowCount());
        livingEntityVar("stinger_count", ctx -> ctx.entity().getStingerCount());
        livingEntityVar("entity_type", YSMBinding::getEntityType);
        livingEntityVar("is_player", ctx -> "player".equals(getEntityType(ctx)));
        livingEntityVar("is_maid", ctx -> "maid".equals(getEntityType(ctx)));
        // 为了兼容其他模组，只有玩家能返回这个值，其他都是满值（20）
        livingEntityVar("food_level", YSMBinding::getFoodLevel);

        livingEntityVar("xxa", YSMBinding::getXxa);
        livingEntityVar("yya", YSMBinding::getYya);
        livingEntityVar("zza", YSMBinding::getZza);

        livingEntityVar("mainhand_charged_crossbow", ctx -> isChargedCrossbow(ctx, InteractionHand.MAIN_HAND));
        livingEntityVar("offhand_charged_crossbow", ctx -> isChargedCrossbow(ctx, InteractionHand.OFF_HAND));
        livingEntityVar("is_fishing", YSMBinding::isFishing);

        playerVar("texture_name", new TextureNameVariable());
        playerVar("first_person_mod_hide", new FirstPersonModHideVariable());
        playerVar("has_left_shoulder_parrot", ctx -> hasParrot(ctx.entity(), true));
        playerVar("has_right_shoulder_parrot", ctx -> hasParrot(ctx.entity(), false));
        playerVar("left_shoulder_parrot_variant", ctx -> getParrotVariant(ctx.entity(), true));
        playerVar("right_shoulder_parrot_variant", ctx -> getParrotVariant(ctx.entity(), false));

        playerVar("attack_damage", ctx -> ctx.entity().getAttributeValue(Attributes.ATTACK_DAMAGE));
        playerVar("attack_speed", ctx -> ctx.entity().getAttributeValue(Attributes.ATTACK_SPEED));
        playerVar("attack_knockback", ctx -> ctx.entity().getAttributeValue(Attributes.ATTACK_KNOCKBACK));
        playerVar("movement_speed", ctx -> ctx.entity().getAttributeValue(Attributes.MOVEMENT_SPEED));
        playerVar("knockback_resistance", ctx -> ctx.entity().getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        playerVar("luck", ctx -> ctx.entity().getAttributeValue(Attributes.LUCK));

        playerVar("block_reach", ctx -> ctx.entity().getAttributeValue(ForgeMod.BLOCK_REACH.get()));
        playerVar("entity_reach", ctx -> ctx.entity().getAttributeValue(ForgeMod.ENTITY_REACH.get()));
        playerVar("swim_speed", ctx -> ctx.entity().getAttributeValue(ForgeMod.SWIM_SPEED.get()));
        playerVar("entity_gravity", ctx -> ctx.entity().getAttributeValue(ForgeMod.ENTITY_GRAVITY.get()));
        playerVar("step_height_addition", ctx -> ctx.entity().getAttributeValue(ForgeMod.STEP_HEIGHT_ADDITION.get()));
        playerVar("nametag_distance", ctx -> ctx.entity().getAttributeValue(ForgeMod.NAMETAG_DISTANCE.get()));
        playerVar("in_shield_block_cooldown", YSMBinding::inShieldBlockCooldown);

        clientPlayerVar("elytra_rot_x", ctx -> Math.toDegrees(ctx.entity().elytraRotX));
        clientPlayerVar("elytra_rot_y", ctx -> Math.toDegrees(ctx.entity().elytraRotY));
        clientPlayerVar("elytra_rot_z", ctx -> Math.toDegrees(ctx.entity().elytraRotZ));

        function("first_order", new FirstOrderFunction());
        function("second_order", new SecondOrderFunction());
        function("particle", new ParticleFunction(false));
        function("abs_particle", new ParticleFunction(true));
        function("perlin_noise", new PerlinNoiseFunction());
        function("play_sound", new SoundFunction.Play());
        function("stop_sound", new SoundFunction.Stop());
        function("stop_all_sounds", new SoundFunction.StopAll());
        function("keyboard", new InputCheck.Keyboard());
        function("mouse", new InputCheck.Mouse());
        function("sync", new Sync());

        projectileVar("projectile_owner", ctx -> ctx.createChild(ctx.entity().getOwner()));

        throwableItemProjectileVar("throwable_item", YSMBinding::getThrowableItem);

        fishingHookVar("hooked_in", YSMBinding::getHookedIn);
        fishingHookVar("is_biting", ctx -> ((FishingHookAccessor) ctx.entity()).ysm$IsBiting());

        abstractArrowVar("on_ground_time", ctx -> ((IArrowExtraInfo) ctx.entity()).inGroundTime());
        abstractArrowVar("in_ground", ctx -> ((IArrowExtraInfo) ctx.entity()).isInGround());
        abstractArrowVar("is_spectral_arrow", ctx -> ctx.entity() instanceof SpectralArrow);
        abstractArrowVar("shoot_item_id", ctx -> ((IArrowExtraInfo) ctx.entity()).getShootItemId());

        // 模组联动
        CuriosCompat.addMolangBinding(this);
    }

    private static String getHitId(IContext<Object> context) {
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult instanceof BlockHitResult result) {
            if (result.getType() == HitResult.Type.MISS) {
                return "";
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return "";
            }
            BlockState blockState = level.getBlockState(result.getBlockPos());
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
            if (id != null) {
                return id.toString();
            } else {
                return "";
            }
        }

        if (hitResult instanceof EntityHitResult result) {
            Entity entity = result.getEntity();
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (id != null) {
                return id.toString();
            } else {
                return "";
            }
        }

        return "";
    }

    private static String getHitType(IContext<Object> context) {
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult == null) {
            return StringUtils.EMPTY;
        }
        return switch (hitResult.getType()) {
            case BLOCK -> "block";
            case ENTITY -> "entity";
            default -> StringUtils.EMPTY;
        };
    }

    private static String getHookedIn(IContext<FishingHook> ctx) {
        Entity hooked = ((FishingHookAccessor) ctx.entity()).ysm$GetHookedIn();
        if (hooked != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(hooked.getType());
            if (id != null) {
                return id.toString();
            }
        }
        return "";
    }

    private static String getThrowableItem(IContext<ThrowableItemProjectile> ctx) {
        ThrowableItemProjectile entity = ctx.entity();
        if (entity instanceof ThrowableItemProjectileAccessor accessor) {
            Item item = accessor.ysm$GetDefaultItem();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) {
                return id.toString();
            }
        }
        return "";
    }

    private static float getGroundSpeed2(IContext<Entity> ctx) {
        var stateStacker = ctx.animatableEntity().getStateTracker();
        var posDelta = stateStacker.getPositionDelta();
        return 20 * Mth.sqrt((float) ((posDelta.x * posDelta.x) + (posDelta.z * posDelta.z))) / stateStacker.getRenderTickDelta();
    }

    private static float getXxa(IContext<LivingEntity> ctx) {
        if (ctx.animatableEntity() instanceof PlayerAnimatableCapability cap && !cap.isLocalPlayer()) {
            return cap.getStateTracker().xxa();
        } else {
            return ctx.entity().xxa;
        }
    }

    private static float getYya(IContext<LivingEntity> ctx) {
        if (ctx.animatableEntity() instanceof PlayerAnimatableCapability cap && !cap.isLocalPlayer()) {
            return cap.getStateTracker().yya();
        } else {
            return ctx.entity().yya;
        }
    }

    private static float getZza(IContext<LivingEntity> ctx) {
        if (ctx.animatableEntity() instanceof PlayerAnimatableCapability cap && !cap.isLocalPlayer()) {
            return cap.getStateTracker().zza();
        } else {
            return ctx.entity().zza;
        }
    }

    private static boolean inShieldBlockCooldown(IContext<Player> context) {
        if (context.animatableEntity() instanceof PlayerAnimatableCapability cap) {
            return cap.getStateTracker().inShieldBlockCooldown();
        } else {
            return false;
        }
    }

    private static boolean isFishing(IContext<LivingEntity> ctx) {
        LivingEntity entity = ctx.entity();
        if (entity instanceof Player player) {
            return player.fishing != null;
        }
        return TlmClientCompat.isMaidFishing(entity);
    }

    private static boolean isChargedCrossbow(IContext<LivingEntity> ctx, InteractionHand hand) {
        ItemStack itemInHand = ctx.entity().getItemInHand(hand);
        return itemInHand.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemInHand);
    }

    private static String getEntityType(IContext<LivingEntity> ctx) {
        LivingEntity entity = ctx.entity();
        if (entity instanceof Player) {
            return "player";
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) {
            return StringUtils.EMPTY;
        }
        if ("touhou_little_maid".equals(key.getNamespace()) && "maid".equals(key.getPath())) {
            return "maid";
        }
        return key.toString();
    }

    private static Object getFoodLevel(IContext<LivingEntity> ctx) {
        if (ctx.animatableEntity() instanceof PlayerAnimatableCapability cap && !cap.isLocalPlayer()) {
            return cap.getStateTracker().foodLevel();
        } else if (ctx.entity() instanceof Player player) {
            return player.getFoodData().getFoodLevel();
        } else {
            return 20;
        }
    }

    private static boolean getEyeCloseState(AnimationEvent<?> animationEvent, LivingEntity player) {
        float remainder = (animationEvent.getRenderTicks() + Math.abs(player.getUUID().getLeastSignificantBits()) % 10) % 90;
        boolean isBlinkTime = 85 < remainder && remainder < 90;
        return player.isSleeping() || isBlinkTime;
    }

    private static boolean getSlotValue(LivingEntity entity, EquipmentSlot slot) {
        return !EquipmentUtil.getEquippedItem(entity, slot).isEmpty();
    }

    private static int getWeather(ClientLevel world) {
        if (world.isThundering()) {
            return 2;
        } else if (world.isRaining()) {
            return 1;
        }
        return 0;
    }

    @Deprecated
    private String getBiomeCategory(Entity entity) {
        return null;
    }

    private static Object dumpMods(IContext<?> context) {
        if (!context.isDebugEnabled()) {
            return null;
        }

        ModList.get().getMods().stream().sorted(Comparator.comparing(IModInfo::getDisplayName)).forEach(mod -> {
            context.debugPrint(Component.literal("Mod: display ").append(ComponentUtils.copyOnClickText(mod.getDisplayName()))
                    .append(Component.literal("  id ").append(ComponentUtils.copyOnClickText(mod.getModId()))));
        });
        return null;
    }

    private static Object dumpEffects(IContext<Entity> context) {
        if (!context.isDebugEnabled()) {
            return null;
        }

        Collection<MobEffectInstance> effects;
        if (context.entity() instanceof Arrow) {
            effects = ((ArrowEntityAccessor) context.entity()).getEffects();
        } else if (context.entity() instanceof LivingEntity) {
            effects = ((LivingEntity) context.entity()).getActiveEffects();
        } else {
            return null;
        }

        for (MobEffectInstance instance : effects) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
            context.debugPrint(Component.literal("Effect: display ").append(ComponentUtils.copyOnClickText(instance.getEffect().getDisplayName().getString(99)))
                    .append(Component.literal("  name ").append(ComponentUtils.copyOnClickText(id.toString())))
                    .append("  lv=").append(String.valueOf(instance.getAmplifier() + 1)));
        }

        return null;
    }

    private static Object dumpBiome(IContext<Entity> context) {
        if (!context.isDebugEnabled()) {
            return null;
        }

        Holder<Biome> biome = context.entity().level().getBiome(context.entity().blockPosition());
        biome.unwrapKey().ifPresent(p -> {
            context.debugPrint(Component.literal("Name ").append(ComponentUtils.copyOnClickText(p.location().toString())));
        });
        biome.tags().forEach(tag -> {
            context.debugPrint(Component.literal("Tag ").append(ComponentUtils.copyOnClickText(tag.location().toString())));
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

    private static String getParrotVariant(Player player, boolean leftShoulder) {
        CompoundTag shoulderTag = leftShoulder ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
        return EntityType.byString(shoulderTag.getString("id"))
                .filter(type -> type == EntityType.PARROT)
                .map(type -> Parrot.Variant.byId(shoulderTag.getInt("Variant")).name().toLowerCase(Locale.ENGLISH))
                .orElse("empty");
    }

    private static boolean hasParrot(Player player, boolean leftShoulder) {
        CompoundTag shoulderTag = leftShoulder ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
        return !shoulderTag.isEmpty();
    }
}
