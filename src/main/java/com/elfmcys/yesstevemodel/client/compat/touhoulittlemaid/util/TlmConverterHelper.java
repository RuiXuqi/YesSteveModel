package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.util;

import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoBoneState;
import com.elfmcys.yesstevemodel.geckolib3.model.GeoModelState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TlmConverterHelper {
    /**
     * 转换成 TLM 下 Bone 的格式
     */
    public static ILocationBone convertToTlmGeoBone(GeoBoneState geoBoneState) {
        return new ILocationBone() {
            @Override
            public float getRotationX() {
                return geoBoneState.getRotationX();
            }

            @Override
            public float getRotationY() {
                return geoBoneState.getRotationY();
            }

            @Override
            public float getRotationZ() {
                return geoBoneState.getRotationZ();
            }

            @Override
            public float getPositionX() {
                return geoBoneState.getPositionX();
            }

            @Override
            public float getPositionY() {
                return geoBoneState.getPositionY();
            }

            @Override
            public float getPositionZ() {
                return geoBoneState.getPositionZ();
            }

            @Override
            public float getScaleX() {
                return geoBoneState.getScaleX();
            }

            @Override
            public float getScaleY() {
                return geoBoneState.getScaleY();
            }

            @Override
            public float getScaleZ() {
                return geoBoneState.getScaleZ();
            }

            @Override
            public float getPivotX() {
                return geoBoneState.getPivotX();
            }

            @Override
            public float getPivotY() {
                return geoBoneState.getPivotY();
            }

            @Override
            public float getPivotZ() {
                return geoBoneState.getPivotZ();
            }
        };
    }

    /**
     * 转换成 TLM 下 AnimatedModel 的格式
     */
    public static ILocationModel convertToTlmAnimatedModel(GeoModelState geoModelState) {
        return new ILocationModel() {
            @Override
            public List<ILocationBone> leftHandBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.leftHandBones());
            }

            @Override
            public List<List<? extends ILocationBone>> extraLeftHandBones() {
                List<List<? extends ILocationBone>> output = new ReferenceArrayList<>();
                geoModelState.extraLeftHandBones().forEach(list -> output.add(convertToTlmAnimatedGeoBones(list)));
                return output;
            }

            @Override
            public List<ILocationBone> rightHandBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.rightHandBones());
            }

            @Override
            public List<List<? extends ILocationBone>> extraRightHandBones() {
                List<List<? extends ILocationBone>> output = new ReferenceArrayList<>();
                geoModelState.extraRightHandBones().forEach(list -> output.add(convertToTlmAnimatedGeoBones(list)));
                return output;
            }

            @Override
            public List<ILocationBone> leftWaistBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.leftWaistBones());
            }

            @Override
            public List<ILocationBone> rightWaistBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.rightWaistBones());
            }

            @Override
            public List<ILocationBone> backpackBones() {
                // 获取 backpack 定位组，如果 backpack 定位组不存在，才获取 elytra 组
                List<IBone> backpackBones = geoModelState.backpackBones();
                if (backpackBones.isEmpty()) {
                    return convertToTlmAnimatedGeoBones(geoModelState.elytraBones());
                } else {
                    return convertToTlmAnimatedGeoBones(backpackBones);
                }
            }

            @Override
            public List<ILocationBone> tacPistolBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.tacPistolBones());
            }

            @Override
            public List<ILocationBone> tacRifleBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.tacRifleBones());
            }

            @Override
            public List<ILocationBone> headBones() {
                return convertToTlmAnimatedGeoBones(geoModelState.headBones());
            }
        };
    }

    private static List<ILocationBone> convertToTlmAnimatedGeoBones(List<IBone> bones) {
        return bones.stream()
                .map(bone -> ((GeoBoneState) bone).<ILocationBone>getTlmBone())
                .toList();
    }
}