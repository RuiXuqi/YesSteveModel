package com.elfmcys.ysm.client.model;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoLocator;
import com.elfmcys.ysm.geckolib3.geo.render.built.GeoLocatorType;
import net.minecraft.resources.ResourceLocation;

public class PlayerLocator extends GeoLocatorType {
    private static PlayerLocator INSTANCE;

    public final GeoLocator head = register("Head");
    public final GeoLocator leftHandLocator = register("LeftHandLocator");
    public final GeoLocator rightHandLocator = register("RightHandLocator");
    public final GeoLocator backpackLocator = register("BackpackLocator");
    public final GeoLocator elytraLocator = register("ElytraLocator");
    public final GeoLocator pistolLocator = register("PistolLocator");
    public final GeoLocator rifleLocator = register("RifleLocator");
    public final GeoLocator leftWaistLocator = register("LeftWaistLocator");
    public final GeoLocator rightWaistLocator = register("RightWaistLocator");
    public final GeoLocator leftShoulderLocator = register("LeftShoulderLocator");
    public final GeoLocator rightShoulderLocator = register("RightShoulderLocator");
    public final GeoLocator bladeLocator = register("BladeLocator");
    public final GeoLocator sheathLocator = register("SheathLocator");
    public final GeoLocator passengerLocator = register("PassengerLocator");

    @SuppressWarnings("removal")
    private PlayerLocator() {
        super(new ResourceLocation(YesSteveModel.MOD_ID, "player"));
    }

    public static void init() {
        if (INSTANCE == null) {
            synchronized (PlayerLocator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PlayerLocator();
                    // 外部注册插在这里
                    INSTANCE.freeze();
                }
            }
        }
    }

    public static PlayerLocator get() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        throw new IllegalStateException("PlayerLocator has not been initialized");
    }
}
