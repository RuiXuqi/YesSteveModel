package com.elfmcys.yesstevemodel.client.compat.slashblade;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.capability.slashblade.CapabilitySlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SlashBladeRender {
    private static final ResourceLocation RESOURCE_DEFAULT_MODEL = new ResourceLocation("slashblade", "model/blade.obj");
    private static final ResourceLocation RESOURCE_DEFAULT_TEXTURE = new ResourceLocation("slashblade", "model/blade.png");

    public static void renderSlashBlade(PoseStack matrixStack, MultiBufferSource bufferIn, int lightIn, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        stack.getCapability(CapabilitySlashBlade.BLADESTATE).ifPresent(bladeState -> {
            ResourceLocation texture = bladeState.getTexture().orElse(RESOURCE_DEFAULT_TEXTURE);
            WavefrontObject obj = BladeModelManager.getInstance().getModel(bladeState.getModel().orElse(RESOURCE_DEFAULT_MODEL));
            String part;
            if (bladeState.isBroken()) {
                part = "blade_damaged";
            } else {
                part = "blade";
            }
            BladeRenderState.renderOverrided(stack, obj, part, texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, part + "_luminous", texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverrided(stack, obj, "sheath", texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, "sheath_luminous", texture, matrixStack, bufferIn, lightIn);
        });
    }

    public static void renderMainhandSlashBlade(LivingEntity livingEntity, GeoModelState model, PoseStack matrixStack,
                                                MultiBufferSource bufferIn, int lightIn, ItemStack stack, float partialTicks) {
        if (stack.getItem() instanceof ItemSlashBlade) {
            List<IBone> leftWaistBones = model.leftWaistBones();
            List<IBone> bladeBones = model.bladeBones();
            List<IBone> sheathBones = model.sheathBones();

            // 如果没有 bladeBones 和 sheathBones，说明是旧版渲染
            if (bladeBones.isEmpty() || sheathBones.isEmpty() || leftWaistBones.isEmpty()) {
                oldMainhandSlashBlade(livingEntity, model, matrixStack, bufferIn, lightIn, stack, partialTicks, leftWaistBones);
            } else {
                stack.getCapability(CapabilitySlashBlade.BLADESTATE).ifPresent(bladeState -> {
                    newMainhandSlashBlade(bladeState, matrixStack, bufferIn, lightIn, stack, leftWaistBones, bladeBones, sheathBones);
                });
            }
        }
    }

    private static void newMainhandSlashBlade(ISlashBladeState bladeState, PoseStack matrixStack, MultiBufferSource bufferIn, int lightIn, ItemStack stack,
                                              List<IBone> leftWaistBones, List<IBone> bladeBones, List<IBone> sheathBones) {

        ResourceLocation texture = bladeState.getTexture().orElse(RESOURCE_DEFAULT_TEXTURE);
        WavefrontObject obj = BladeModelManager.getInstance().getModel(bladeState.getModel().orElse(RESOURCE_DEFAULT_MODEL));
        String part;
        if (bladeState.isBroken()) {
            part = "blade_damaged";
        } else {
            part = "blade";
        }

        IBone leftWaist = leftWaistBones.get(leftWaistBones.size() - 1);
        // 如果缩放不为 0 才渲染
        if (leftWaist.getScaleX() != 0 || leftWaist.getScaleY() != 0 || leftWaist.getScaleZ() != 0) {
            matrixStack.pushPose();

            // 移动到定位组
            RenderUtils.prepMatrixForLocator(matrixStack, leftWaistBones);
            // 定位点定在刀中心，刀朝向前方（默认）
            matrixStack.translate(0, 0, -0.7);
            matrixStack.scale(0.01F, 0.01F, 0.01F);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180));

            BladeRenderState.renderOverrided(stack, obj, part, texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, part + "_luminous", texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverrided(stack, obj, "sheath", texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, "sheath_luminous", texture, matrixStack, bufferIn, lightIn);

            matrixStack.popPose();
        }

        IBone blade = bladeBones.get(bladeBones.size() - 1);
        // 如果缩放不为 0 才渲染
        if (blade.getScaleX() != 0 || blade.getScaleY() != 0 || blade.getScaleZ() != 0) {
            matrixStack.pushPose();

            // 移动到定位组
            RenderUtils.prepMatrixForLocator(matrixStack, bladeBones);
            // 定位点定在刀中心，刀朝向前方（默认）
            matrixStack.translate(0, 0, -0.7);
            matrixStack.scale(0.01F, 0.01F, 0.01F);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180));

            BladeRenderState.renderOverrided(stack, obj, part, texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, part + "_luminous", texture, matrixStack, bufferIn, lightIn);

            matrixStack.popPose();
        }

        IBone sheath = sheathBones.get(sheathBones.size() - 1);
        // 如果缩放不为 0 才渲染
        if (sheath.getScaleX() != 0 || sheath.getScaleY() != 0 || sheath.getScaleZ() != 0) {
            matrixStack.pushPose();

            // 移动到定位组
            RenderUtils.prepMatrixForLocator(matrixStack, sheathBones);
            // 定位点定在刀中心，刀朝向前方（默认）
            matrixStack.translate(0, 0, -0.7);
            matrixStack.scale(0.01F, 0.01F, 0.01F);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180));

            BladeRenderState.renderOverrided(stack, obj, "sheath", texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, "sheath_luminous", texture, matrixStack, bufferIn, lightIn);

            matrixStack.popPose();
        }
    }

    private static void oldMainhandSlashBlade(LivingEntity livingEntity, GeoModelState model, PoseStack matrixStack, MultiBufferSource bufferIn,
                                              int lightIn, ItemStack stack, float partialTicks, List<IBone> leftWaistBones) {
        matrixStack.pushPose();
        // 主手的刀渲染在左边
        if (!leftWaistBones.isEmpty()) {
            translateToWaist(HumanoidArm.LEFT, matrixStack, model);
        } else {
            matrixStack.translate(-0.25, 1.25, 0);
            matrixStack.mulPose(Axis.XP.rotationDegrees(20));
        }
        matrixStack.translate(0, 0, -0.7);
        matrixStack.scale(0.01F, 0.01F, 0.01F);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
        matrixStack.mulPose(Axis.ZP.rotationDegrees(180));
        if (stack.isEmpty()) {
            return;
        }
        stack.getCapability(CapabilitySlashBlade.BLADESTATE).ifPresent(bladeState -> {
            ResourceLocation texture = bladeState.getTexture().orElse(RESOURCE_DEFAULT_TEXTURE);
            WavefrontObject obj = BladeModelManager.getInstance().getModel(bladeState.getModel().orElse(RESOURCE_DEFAULT_MODEL));
            String part;
            if (bladeState.isBroken()) {
                part = "blade_damaged";
            } else {
                part = "blade";
            }
            BladeRenderState.renderOverrided(stack, obj, "sheath", texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, "sheath_luminous", texture, matrixStack, bufferIn, lightIn);
            long time = livingEntity.level().getGameTime() - bladeState.getLastActionTime();
            if (time < 5) {
                float i = time + partialTicks;
                matrixStack.translate(0, 0, -0.5 / 0.007);
                matrixStack.mulPose(Axis.YP.rotationDegrees(60 + i * 48));
                matrixStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            BladeRenderState.renderOverrided(stack, obj, part, texture, matrixStack, bufferIn, lightIn);
            BladeRenderState.renderOverridedLuminous(stack, obj, part + "_luminous", texture, matrixStack, bufferIn, lightIn);
        });
        matrixStack.popPose();
    }

    public static void renderOffhandSlashBlade(GeoModelState model, PoseStack matrixStack, MultiBufferSource bufferIn, int lightIn, ItemStack stack) {
        if (stack.getItem() instanceof ItemSlashBlade) {
            matrixStack.pushPose();
            // 副手的刀渲染在右边
            if (!model.rightWaistBones().isEmpty()) {
                translateToWaist(HumanoidArm.RIGHT, matrixStack, model);
            } else {
                matrixStack.translate(0.25, 1.25, 0);
                matrixStack.mulPose(Axis.XP.rotationDegrees(5));
            }
            matrixStack.translate(0, 0, -0.7);
            matrixStack.scale(0.01F, 0.01F, 0.01F);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180));
            SlashBladeRender.renderSlashBlade(matrixStack, bufferIn, lightIn, stack);
            matrixStack.popPose();
        }
    }

    private static void translateToWaist(HumanoidArm arm, PoseStack poseStack, GeoModelState geoModel) {
        if (arm == HumanoidArm.LEFT) {
            RenderUtils.prepMatrixForLocator(poseStack, geoModel.leftWaistBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, geoModel.rightWaistBones());
        }
    }
}
