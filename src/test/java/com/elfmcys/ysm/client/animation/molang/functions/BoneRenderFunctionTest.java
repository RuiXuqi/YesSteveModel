package com.elfmcys.ysm.client.animation.molang.functions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoneRenderFunctionTest {
    @Test
    void validatesFunctionArity() {
        var color = new BoneRenderFunction.Color();
        var transparency = new BoneRenderFunction.Transparency();
        var glow = new BoneRenderFunction.Glow();

        assertTrue(color.validateArgumentSize(4));
        assertFalse(color.validateArgumentSize(3));
        assertTrue(transparency.validateArgumentSize(2));
        assertTrue(glow.validateArgumentSize(2));
    }

    @Test
    void roundsAndClampsNumericInputs() {
        assertEquals(0, BoneRenderFunction.roundedClamp(Float.NaN, 0, 255));
        assertEquals(0, BoneRenderFunction.roundedClamp(-10, 0, 255));
        assertEquals(2, BoneRenderFunction.roundedClamp(1.5f, 0, 255));
        assertEquals(255,
                BoneRenderFunction.roundedClamp(Float.POSITIVE_INFINITY, 0, 255));
        assertEquals(-1,
                BoneRenderFunction.roundedClamp(Float.NEGATIVE_INFINITY, -1, 15));
        assertEquals(15, BoneRenderFunction.roundedClamp(20, -1, 15));
    }
}
