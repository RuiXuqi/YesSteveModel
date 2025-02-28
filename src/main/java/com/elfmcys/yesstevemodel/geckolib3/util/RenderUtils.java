package com.elfmcys.yesstevemodel.geckolib3.util;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.mixin.client.MinecraftAccessor;
import com.elfmcys.yesstevemodel.mixin.client.TimerAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

//Native Association
public final class RenderUtils {
    public static void translateMatrixToBone(PoseStack poseStack, IBone bone) {
        poseStack.translate(-bone.getPositionX() / 16f, bone.getPositionY() / 16f, bone.getPositionZ() / 16f);
    }

    public static void rotateMatrixAroundBone(PoseStack poseStack, IBone bone) {
        if (bone.getRotationZ() != 0 || bone.getRotationX() != 0 || bone.getRotationY() != 0) {
            Quaternionf rot = new Quaternionf();
            rot.rotateZYX(bone.getRotationZ(), bone.getRotationY(), bone.getRotationX());
            poseStack.mulPose(rot);
        }
    }

    /**
     * 如果缩放全为 0，则返回 true
     */
    public static boolean scaleMatrixForBone(PoseStack poseStack, IBone bone) {
        float scaleX = bone.getScaleX();
        float scaleY = bone.getScaleY();
        float scaleZ = bone.getScaleZ();
        poseStack.scale(scaleX, scaleY, scaleZ);
        return scaleX == 0 && scaleY == 0 && scaleZ == 0;
    }

    public static void translateToPivotPoint(PoseStack poseStack, IBone bone) {
        poseStack.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
    }

    public static void translateAwayFromPivotPoint(PoseStack poseStack, IBone bone) {
        poseStack.translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f);
    }

    public static void translateAndRotateMatrixForBone(PoseStack poseStack, IBone bone) {
        translateToPivotPoint(poseStack, bone);
        rotateMatrixAroundBone(poseStack, bone);
    }

    /**
     * 如果缩放为 0，则返回 true
     */
    public static boolean prepMatrixForBone(PoseStack poseStack, IBone bone) {
        translateMatrixToBone(poseStack, bone);
        translateToPivotPoint(poseStack, bone);
        rotateMatrixAroundBone(poseStack, bone);
        boolean scaleAllIsZero = scaleMatrixForBone(poseStack, bone);
        translateAwayFromPivotPoint(poseStack, bone);
        return scaleAllIsZero;
    }

    public static boolean prepMatrixForLocator(PoseStack poseStack, List<IBone> locatorHierarchy) {
        boolean scaleCheck = false;
        for (int i = 0; i < locatorHierarchy.size() - 1; i++) {
            boolean result = RenderUtils.prepMatrixForBone(poseStack, locatorHierarchy.get(i));
            if (result) {
                scaleCheck = true;
            }
        }
        IBone lastBone = locatorHierarchy.get(locatorHierarchy.size() - 1);
        RenderUtils.translateMatrixToBone(poseStack, lastBone);
        RenderUtils.translateToPivotPoint(poseStack, lastBone);
        RenderUtils.rotateMatrixAroundBone(poseStack, lastBone);
        RenderUtils.scaleMatrixForBone(poseStack, lastBone);
        return scaleCheck;
    }

    public static Matrix4f invertAndMultiplyMatrices(Matrix4f baseMatrix, Matrix4f inputMatrix) {
        inputMatrix = new Matrix4f(inputMatrix);
        inputMatrix.invert();
        inputMatrix.mul(baseMatrix);
        return inputMatrix;
    }

    public static float getRenderTickTime() {
        return (float) ((TimerAccessor) ((MinecraftAccessor) Minecraft.getInstance()).getTimer()).getLastMs() / 50f;
    }
}
