package com.elfmcys.ysm.network.forge;

import com.elfmcys.ysm.capability.ProjectileModelInfoCapability;
import com.elfmcys.ysm.capability.VehicleModelInfoCapability;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.util.ProtoBytes;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftStateHandlerTest {
    @Test
    void mapsProjectileAndVehicleStateWithoutNbt() {
        var hashBytes = new byte[Hash256.SIZE];
        hashBytes[0] = 42;
        var hash = new Hash256(hashBytes);
        var variables = new Object2FloatOpenHashMap<String>();
        variables.put("query.test", 1.5F);
        var projectile = new ProjectileModelInfoCapability();
        projectile.init(hash, variables);
        var vehicle = new VehicleModelInfoCapability();
        vehicle.update(hash, variables);

        var projectileMessage = MinecraftStateHandler.projectile(7, projectile);
        var vehicleMessage = MinecraftStateHandler.vehicle(9, vehicle);

        assertEquals(7, projectileMessage.getEntity().getEntityId());
        assertEquals(9, vehicleMessage.getEntity().getEntityId());
        assertTrue(!projectileMessage.getEntity().hasPlayerId());
        assertTrue(!vehicleMessage.getEntity().hasPlayerId());
        assertTrue(ProtoBytes.equals(hash, projectileMessage.getModel().getModelHash()));
        assertTrue(ProtoBytes.equals(hash, vehicleMessage.getModel().getModelHash()));
        assertEquals("query.test", projectileMessage.getMolangVariables().get(0).getName());
        assertEquals(1.5F, vehicleMessage.getMolangVariables().get(0).getValue());
    }
}
