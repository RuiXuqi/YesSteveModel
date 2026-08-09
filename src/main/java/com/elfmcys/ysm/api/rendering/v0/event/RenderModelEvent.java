package com.elfmcys.ysm.api.rendering.v0.event;

import com.elfmcys.ysm.api.rendering.v0.TargetKind;
import com.elfmcys.ysm.geckolib3.geo.GeoRenderData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

@Cancelable
public class RenderModelEvent extends Event implements IModBusEvent {
    private final Object target;
    private final TargetKind targetKind;
    private final GeoRenderData renderData;
    private final MultiBufferSource buffer;
    private final RenderType renderType;
    private final PoseStack poseStack;
    private final int light;
    private final int overlay;
    private final int color;

    public RenderModelEvent(Object target, TargetKind targetKind, GeoRenderData renderData, MultiBufferSource buffer, RenderType renderType, PoseStack poseStack, int light, int overlay, int color) {
        this.target = target;
        this.targetKind = targetKind;
        this.renderData = renderData;
        this.buffer = buffer;
        this.renderType = renderType;
        this.poseStack = poseStack;
        this.light = light;
        this.overlay = overlay;
        this.color = color;
    }

    /// 当前只有 Entity，以后说不定有别的
    public Object target() {
        return target;
    }

    public TargetKind targetKind() {
        return targetKind;
    }

    public GeoRenderData renderData() {
        return renderData;
    }

    public MultiBufferSource buffer() {
        return buffer;
    }

    public RenderType renderType() {
        return renderType;
    }

    public PoseStack pose() {
        return poseStack;
    }

    public int light() {
        return light;
    }

    public int overlay() {
        return overlay;
    }

    public int color() {
        return color;
    }
}
