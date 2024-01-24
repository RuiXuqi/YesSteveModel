package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// Native Access
public class NativeRenderer {
    // 一定要传魔法值，不能直接传 BoneName
    // Native Association
    public static final int RENDER_MODE_ALL = 0;
    public static final int RENDER_MODE_LEFT_ARM = 1;
    public static final int RENDER_MODE_RIGHT_ARM = 2;
    public static final int RENDER_MODE_BACKGROUND = 3;

    private static boolean IS_ASYNC_SCOPE = false;

    public static void renderModel(VertexConsumer vertexConsumer, PoseStack.Pose poseState,
                                   GeoModel model, float[] state, int renderMode,
                                   int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        nRenderModel(vertexConsumer, poseState, GeneralConfig.USE_COMPATIBILITY_RENDERER.get(), model, state, renderMode, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public static void beginAsyncScope() {
        IS_ASYNC_SCOPE = true;
    }

    private static native void nRenderModel(VertexConsumer vertexConsumer, PoseStack.Pose poseState, boolean useCompatibilityRenderer,
                                            GeoModel model, float[] state, int renderMode,
                                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha);

    public static boolean isAsyncScope() {
        return IS_ASYNC_SCOPE;
    }

    public static void endAsyncScope() {
        IS_ASYNC_SCOPE = false;
    }

    // Native Access
    @SuppressWarnings("all")
    static class LegacyWriter {
        // Native Association
        private static final int FLOAT_STRIDE = 12;
        private static final int INT_STRIDE = 2;

        private static final int FLOAT_INDEX_X = 0;
        private static final int FLOAT_INDEX_Y = 1;
        private static final int FLOAT_INDEX_Z = 2;
        private static final int FLOAT_INDEX_R = 3;
        private static final int FLOAT_INDEX_G = 4;
        private static final int FLOAT_INDEX_B = 5;
        private static final int FLOAT_INDEX_A = 6;
        private static final int FLOAT_INDEX_TEX_U = 7;
        private static final int FLOAT_INDEX_TEX_V = 8;
        private static final int FLOAT_INDEX_NORMAL_X = 9;
        private static final int FLOAT_INDEX_NORMAL_Y = 10;
        private static final int FLOAT_INDEX_NORMAL_Z = 11;

        private static final int INT_INDEX_OVERLAY_UV = 0;
        private static final int INT_INDEX_LIGHT_MAP_UV = 1;

        // Native Access
        public static void flushVertex(final VertexConsumer vertexConsumer, final int vertexCount, final float[] floatValues, final int[] intValues) {
            for (int i = 0, floatOffset = 0, intOffset = 0; i < vertexCount; i++, floatOffset += FLOAT_STRIDE, intOffset += INT_STRIDE) {
                vertexConsumer.vertex(
                        floatValues[floatOffset + FLOAT_INDEX_X],
                        floatValues[floatOffset + FLOAT_INDEX_Y],
                        floatValues[floatOffset + FLOAT_INDEX_Z],
                        floatValues[floatOffset + FLOAT_INDEX_R],
                        floatValues[floatOffset + FLOAT_INDEX_G],
                        floatValues[floatOffset + FLOAT_INDEX_B],
                        floatValues[floatOffset + FLOAT_INDEX_A],
                        floatValues[floatOffset + FLOAT_INDEX_TEX_U],
                        floatValues[floatOffset + FLOAT_INDEX_TEX_V],
                        intValues[intOffset + INT_INDEX_OVERLAY_UV],
                        intValues[intOffset + INT_INDEX_LIGHT_MAP_UV],
                        floatValues[floatOffset + FLOAT_INDEX_NORMAL_X],
                        floatValues[floatOffset + FLOAT_INDEX_NORMAL_Y],
                        floatValues[floatOffset + FLOAT_INDEX_NORMAL_Z]);
            }
        }
    }
}
