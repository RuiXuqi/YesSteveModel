package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.compat.IrisCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomHumanoidEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.model.AnimatableEntity;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;

import java.util.concurrent.ExecutionException;

@SuppressWarnings("all")
public final class RenderUtil {
    private static boolean renderingInInventory = false;
    private static boolean renderingInPaperDoll = false;
    private static boolean renderingLevel = false;

    public static void setRenderingInInventory(boolean value) {
        renderingInInventory = value;
    }

    public static boolean isRenderingInInventory() {
        return renderingInInventory;
    }

    public static void setRenderingInPaperDoll(boolean renderingEntitiesInPaperDoll) {
        RenderUtil.renderingInPaperDoll = renderingEntitiesInPaperDoll;
    }

    public static boolean isRenderingInPaperDoll() {
        return renderingInPaperDoll;
    }

    public static void setRenderingLevel(boolean renderingLevel) {
        RenderUtil.renderingLevel = renderingLevel;
    }

    public static boolean isRenderingLevel() {
        return renderingLevel ||
                IrisCompat.isRenderingShadow() ||
                FirstPersonCompat.isRenderingPlayer();
    }

    public static boolean isRenderingLevelExclusive() {
        return renderingLevel && !FirstPersonCompat.isRenderingPlayer();
    }

    public static <T extends LivingEntity, TAnimatable extends AnimatableEntity<T> & IPreviewEntity> void renderTextureScreenEntity(float pPosX, float pPosY, float pScale, float pitch, float yaw, float partialTicks, TAnimatable entity, GeoReplacedEntityRenderer<T, ? super TAnimatable> renderer, boolean showGround) {
        setRenderingInInventory(true);
        var living = entity.getEntity();

        PoseStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushPose();
        viewStack.translate(pPosX, pPosY, 1250.0D);
        viewStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale(pScale, pScale, pScale);
        poseStack.translate(0, 0.8, 0);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
        zp.mul(xp);
        poseStack.mulPose(zp);

        float yBodyRot = living.yBodyRot;
        float yBodyRotO = living.yBodyRotO;
        float yRot = living.getYRot();
        float yRotO = living.yRotO;
        float xRot = living.getXRot();
        float xRotO = living.xRotO;
        float yHeadRotO = living.yHeadRotO;
        float yHeadRot = living.yHeadRot;
        Pose pose = living.getPose();

        living.yBodyRot = -yaw;
        living.yBodyRotO = -yaw;
        living.setYRot(180);
        living.yRotO = 180;
        living.setXRot(0);
        living.xRotO = 0;
        living.yHeadRot = -yaw;
        living.yHeadRotO = -yaw;

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        xp.conjugate();
        dispatcher.overrideCameraOrientation(xp);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            var guiAnim = entity.getPreviewInfo();
            if (guiAnim.hasPreview("sleep")) {
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90));
                poseStack.translate(0.5, 0.5625, 0);
                living.setPose(Pose.SLEEPING);
            }
            if (guiAnim.hasPreview("swim") || guiAnim.hasPreview("swim_stand")) {
                living.setPose(Pose.SWIMMING);
            }
            if (guiAnim.hasPreview("sneak") || guiAnim.hasPreview("sneaking")) {
                living.setPose(Pose.CROUCHING);
            }
            if (guiAnim.hasPreview("sit")) {
                poseStack.translate(0, -0.5, 0);
            }
            if (guiAnim.hasPreview("ride")) {
                poseStack.translate(0, 0.85, 0);
            }
            if (guiAnim.hasPreview("ride_pig")) {
                poseStack.translate(0, 0.3125, 0);
            }
            if (guiAnim.hasPreview("boat")) {
                poseStack.translate(0, -0.45, 0);
            }
            try {
                renderExtraEntity(yaw, entity, partialTicks, poseStack, dispatcher, bufferSource);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (guiAnim.hasPreview("sleep")) {
                renderBed(pScale, pitch, yaw, bufferSource);
            }
            renderGround(pScale, pitch, yaw, bufferSource);
            bufferSource.endBatch();
            renderer.renderAnimatableEntity(entity, 0, partialTicks, poseStack, bufferSource, 0xf000f0);
        });
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);

        living.yBodyRot = yBodyRot;
        living.yBodyRotO = yBodyRotO;
        living.setYRot(yRot);
        living.yRotO = yRotO;
        living.setXRot(xRot);
        living.xRotO = xRotO;
        living.yHeadRotO = yHeadRotO;
        living.yHeadRot = yHeadRot;
        living.setPose(pose);

        viewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        setRenderingInInventory(false);
    }

    private static void renderBed(float scale, float pitch, float yaw, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.8, 0);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
        zp.mul(xp);
        poseStack.mulPose(zp);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180));
        poseStack.translate(-0.5, 0, 0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_BED.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
    }

    private static void renderGround(float scale, float pitch, float yaw, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.8, 0);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(-10 + pitch);
        zp.mul(xp);
        poseStack.mulPose(zp);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-1.5, -1, -2.5);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                poseStack.translate(0, 0, 1);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
            }
            poseStack.translate(1, 0, -3);
        }
        poseStack.translate(-1, 1, 1);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
        poseStack.translate(0, 0, 1);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_TULIP.defaultBlockState(), poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
    }

    private static <TAnimatable extends AnimatableEntity<?> & IPreviewEntity> void renderExtraEntity(float yaw, TAnimatable animatableEntity, float partialTicks, PoseStack poseStack, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource) throws ExecutionException {
        var player = animatableEntity.getEntity();
        var guiAnim = animatableEntity.getPreviewInfo();

        if (guiAnim.hasPreview("ride")) {
            Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(player.level()));
            renderExtraEntity(yaw, player, poseStack, dispatcher, bufferSource, entity, partialTicks);
            return;
        }
        if (guiAnim.hasPreview("ride_pig")) {
            Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(player.level()));
            renderExtraEntity(yaw, player, poseStack, dispatcher, bufferSource, entity, partialTicks);
            return;
        }
        if (guiAnim.hasPreview("boat")) {
            Entity entity = AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(player.level()));
            renderExtraEntity(yaw, player, poseStack, dispatcher, bufferSource, entity, partialTicks);
            return;
        }
    }

    private static void renderExtraEntity(float yaw, Entity player, PoseStack poseStack, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource, Entity entity, float partialTicks) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        dispatcher.render(entity, 0, -entity.getPassengersRidingOffset() - player.getMyRidingOffset(), 0, 0, partialTicks, poseStack, bufferSource, 0xf000f0);
        poseStack.popPose();
    }

    public static <T extends LivingEntity, TAnimatable extends CustomHumanoidEntity<T>> void renderModelInGui(
            float pPosX,
            float pPosY,
            float pScale,
            float partialTicks,
            TAnimatable animatableEntity,
            GeoReplacedEntityRenderer<T, TAnimatable> renderer,
            boolean disablePreviewRotation,
            boolean disableEquipments) {
        setRenderingInInventory(true);
        var living = animatableEntity.getEntity();

        PoseStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushPose();
        viewStack.translate(pPosX, pPosY, 1050.0D);
        viewStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, disablePreviewRotation ? 5.5 : 0, 1000.0D);
        poseStack.scale(pScale, pScale, pScale);
        Quaternionf zp = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf xp = Axis.XP.rotationDegrees(disablePreviewRotation ? 0 : -10);
        zp.mul(xp);
        poseStack.mulPose(zp);

        float yBodyRot = living.yBodyRot;
        float yBodyRotO = living.yBodyRotO;
        float yRot = living.getYRot();
        float yRotO = living.yRotO;
        float xRot = living.getXRot();
        float xRotO = living.xRotO;
        float yHeadRotO = living.yHeadRotO;
        float yHeadRot = living.yHeadRot;

        ItemStack[] itemStacks;
        if (disableEquipments && living instanceof Player player) {
            itemStacks = new ItemStack[EquipmentSlot.values().length];
            int i = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                itemStacks[i] = player.getItemBySlot(slot);
                if (slot == EquipmentSlot.MAINHAND) {
                    player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
                } else if (slot == EquipmentSlot.OFFHAND) {
                    player.getInventory().offhand.set(0, ItemStack.EMPTY);
                } else {
                    player.getInventory().armor.set(slot.getIndex(), ItemStack.EMPTY);
                }
                i++;
            }
        } else {
            itemStacks = null;
        }

        float yRotGui = disablePreviewRotation ? 180 : 200;
        living.yBodyRot = yRotGui;
        living.yBodyRotO = yRotGui;
        living.setYRot(yRotGui);
        living.yRotO = yRotGui;
        living.setXRot(0);
        living.xRotO = 0;
        living.yHeadRot = living.getYRot();
        living.yHeadRotO = living.getYRot();

        // 修正骑乘时 GUI 界面歪头的 bug
        if (living.getVehicle() instanceof LivingEntity vehicle) {
            float vehicleYRot = vehicle.getYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(vehicleYRot - yRotGui));
            living.yHeadRot = vehicleYRot;
            living.yHeadRotO = vehicleYRot;
        }

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        xp.conjugate();
        dispatcher.overrideCameraOrientation(xp);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            renderer.renderAnimatableEntity(animatableEntity, 0, partialTicks, poseStack, bufferSource, 0xf000f0);
        });
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);

        living.yBodyRot = yBodyRot;
        living.yBodyRotO = yBodyRotO;
        living.setYRot(yRot);
        living.yRotO = yRotO;
        living.setXRot(xRot);
        living.xRotO = xRot;
        living.yHeadRotO = yHeadRotO;
        living.yHeadRot = yHeadRot;

        if (itemStacks != null) {
            Player player = (Player) living;
            int i = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack itemStack = itemStacks[i];
                if (slot == EquipmentSlot.MAINHAND) {
                    player.getInventory().items.set(player.getInventory().selected, itemStack);
                } else if (slot == EquipmentSlot.OFFHAND) {
                    player.getInventory().offhand.set(0, itemStack);
                } else {
                    player.getInventory().armor.set(slot.getIndex(), itemStack);
                }
                i++;
            }
        }

        viewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        setRenderingInInventory(false);
    }

    public static void renderExtraPlayerEntity(GuiGraphics pGuiGraphics, LocalPlayer player, double posX, double posY, float scale, float yawOffset, int z, float partialTicks) {
        RenderUtil.setRenderingInPaperDoll(true);
        PoseStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushPose();
        viewStack.translate(posX + scale * 0.5, posY + scale * 2, 0);
        viewStack.scale(1, 1, -1);
        RenderSystem.applyModelViewMatrix();
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0, 0, -z);
        pGuiGraphics.pose().scale(scale, scale, scale);
        Quaternionf zRot = Axis.ZP.rotationDegrees(180.1F);
        Quaternionf yRot = Axis.YP.rotationDegrees(Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) + yawOffset - 180);
        zRot.mul(yRot);
        pGuiGraphics.pose().mulPose(zRot);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher renderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        yRot.conjugate();
        renderDispatcher.overrideCameraOrientation(yRot);
        renderDispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> renderDispatcher.render(player, 0, 0, 0.0D, 0.0F, partialTicks, pGuiGraphics.pose(), pGuiGraphics.bufferSource(), 15728880));
        pGuiGraphics.flush();
        renderDispatcher.setRenderShadow(true);
        pGuiGraphics.pose().popPose();
        viewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderUtil.setRenderingInPaperDoll(false);
    }
}
