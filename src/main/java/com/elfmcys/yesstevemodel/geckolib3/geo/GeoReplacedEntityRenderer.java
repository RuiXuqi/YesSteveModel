package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.api.ILivingRenderer;
import com.elfmcys.yesstevemodel.client.entity.CustomHumanoidEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.elfmcys.yesstevemodel.mixin.client.LivingEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends CustomHumanoidEntity<TEntity>> extends LivingEntityRenderer<TEntity, PlayerModel<TEntity>> implements IGeoRenderer<T> {
    protected final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    protected MultiBufferSource rtb = null;
    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    public static int getPackedOverlay(LivingEntity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle currentModelRenderCycle) {
        this.currentModelRenderCycle = currentModelRenderCycle;
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick,
                            MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn,
                            float red, float green, float blue, float alpha) {
        this.renderEarlyMat = new Matrix4f(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public void renderAnimatableEntity(T animatableEntity, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderAnimatableEntity(animatableEntity, null,  entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public void renderAnimatableEntity(T animatableEntity, @Nullable ResourceLocation textureOverride, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<>(animatableEntity.getEntity(), this, partialTick, poseStack, bufferSource, packedLight)))
            return;
        var event = animatableEntity.updateAnimation(partialTick);
        final TEntity entity = animatableEntity.getEntity();
        var mc = Minecraft.getInstance();
        if (event != null && mc.player != null) {
            final EntityModelData data = event.getExtraData();
            this.dispatchedMat = new Matrix4f(poseStack.last().pose());

            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();

            if (entity.getPose() == Pose.SLEEPING) {
                Direction direction = entity.getBedOrientation();
                if (direction != null) {
                    float eyeOffset = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                    poseStack.translate(-direction.getStepX() * eyeOffset, 0, -direction.getStepZ() * eyeOffset);
                }
            }

            setupRotations(entity, poseStack, data.lerpedAge, data.lerpBodyRot, partialTick);
            preRenderCallback(entity, poseStack, partialTick);
            poseStack.translate(0, 0.01f, 0);

            var bodyVisible = this.isBodyVisible(entity) && !entity.isInvisibleTo(mc.player);
            var glowing = mc.shouldEntityAppearGlowing(entity);
            var renderType = getRenderType(textureOverride == null ? animatableEntity.getTextureLocation() : textureOverride, bodyVisible, glowing);

            var model = animatableEntity.getLoadedGeoModel();
            var renderLayersFirst = animatableEntity.renderLayersFirst();
            var renderColor = getRenderColor(animatableEntity, partialTick, poseStack, bufferSource, null, packedLight);
            var textureIndex = textureOverride == null ? animatableEntity.getTextureIndex() : 0;

            preRender(model, animatableEntity, partialTick, poseStack, bufferSource, null,
                    packedLight, getPackedOverlay(entity, getOverlayProgress(entity, partialTick)),
                    renderColor.getRed() / 255f, renderColor.getGreen() / 255f,
                    renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
            if (renderLayersFirst && !entity.isSpectator()) {
                renderLayer(animatableEntity, partialTick, poseStack, bufferSource, packedLight, event, data);
            }
            if (renderType != null) {
                render(model, animatableEntity, partialTick, renderType, poseStack, bufferSource, textureIndex, null,
                        packedLight, getPackedOverlay(entity, getOverlayProgress(entity, partialTick)),
                        renderColor.getRed() / 255f, renderColor.getGreen() / 255f,
                        renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
            }
            if (!renderLayersFirst && !entity.isSpectator()) {
                renderLayer(animatableEntity, partialTick, poseStack, bufferSource, packedLight, event, data);
            }
            poseStack.popPose();
        }
        ((ILivingRenderer) this).superRender(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(entity, this, partialTick, poseStack, bufferSource, packedLight));
    }

    protected void renderLayer(T animatableEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
            layerRenderer.render(poseStack, bufferSource, packedLight, animatableEntity, event.getLimbSwing(), event.getLimbSwingAmount(), partialTick,
                    data.lerpedAge, data.rawNetHeadYaw, data.rawHeadPitch);
        }
    }

    protected float getOverlayProgress(TEntity entity, float partialTicks) {
        return 0.0F;
    }

    protected void preRenderCallback(TEntity entity, PoseStack poseStack, float partialTick) {
    }

    @Override
    protected void setupRotations(TEntity pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        int deathTime = pEntityLiving.deathTime;
        boolean autoSpineAttach = pEntityLiving.isAutoSpinAttack();
        if (deathTime > 0) {
            pEntityLiving.deathTime = 0;
        }
        if (autoSpineAttach) {
            ((LivingEntityAccessor) pEntityLiving).setFlag(4, false);
        }

        // 爬梯时，禁止旋转
        if (pEntityLiving.onClimbable()) {
            Optional<BlockPos> climbablePos = pEntityLiving.getLastClimbablePos();
            if (climbablePos.isPresent()) {
                BlockState blockState = pEntityLiving.level().getBlockState(climbablePos.get());
                Optional<Direction> optionalValue = blockState.getOptionalValue(HorizontalDirectionalBlock.FACING);
                if (optionalValue.isPresent()) {
                    pRotationYaw = optionalValue.get().getOpposite().get2DDataValue() * 90;
                }
            }
        }

        super.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);

        if (deathTime > 0) {
            pEntityLiving.deathTime = deathTime;
        }
        if (autoSpineAttach) {
            ((LivingEntityAccessor) pEntityLiving).setFlag(4, true);
        }
    }

    @Override
    public boolean shouldShowName(TEntity entity) {
        double nameRenderDistance = entity.isDiscrete() ? 32d : 64d;
        if (this.entityRenderDispatcher.distanceToSqr(entity) >= nameRenderDistance * nameRenderDistance) {
            return false;
        }
        return entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && Minecraft.renderNames();
    }

    public final boolean addLayer(GeoLayerRenderer<T> layer) {
        return this.layerRenderers.add(layer);
    }

    @Override
    public MultiBufferSource getCurrentRTB() {
        return this.rtb;
    }

    @Override
    public void setCurrentRTB(MultiBufferSource bufferSource) {
        this.rtb = bufferSource;
    }
}
