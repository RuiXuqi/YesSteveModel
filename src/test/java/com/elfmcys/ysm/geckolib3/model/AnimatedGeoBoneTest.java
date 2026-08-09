package com.elfmcys.ysm.geckolib3.model;

import com.elfmcys.ysm.geckolib3.geo.render.built.GeoBone;
import mixel.asset.model.data.GeoModelOuterClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimatedGeoBoneTest {
    @Test
    void packsIndependentRenderAttributesWithIdentityDefaults() {
        var attributes = new float[AnimatedGeoModel.BONE_ATTRIBUTE_COUNT];
        var data = GeoModelOuterClass.Bone.newInstance()
                .setName("root")
                .addRotate(0).addRotate(0).addRotate(0)
                .addPivot(0).addPivot(0).addPivot(0);
        var bone = new AnimatedGeoBone(new GeoBone(data, null), attributes, 0);

        assertEquals(0xFFFFFF, (int) attributes[12]);
        assertEquals(0xFFFF, (int) attributes[13]);

        bone.setColor(1, 2, 3);
        assertEquals(0x030201, (int) attributes[12]);

        bone.setTransparency(127);
        assertEquals(0xFF7F, (int) attributes[13]);
        bone.setGlow(7);
        assertEquals(0x077F, (int) attributes[13]);

        bone.setTransparency(255);
        assertEquals(0x07FF, (int) attributes[13]);
        bone.setGlow(-1);
        assertEquals(0xFFFF, (int) attributes[13]);
    }
}
