package com.elfmcys.yesstevemodel.client.compat;

import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.LoadingModList;

public class FirstPersonCompat {
    private static final String LEGACY_MOD_ID = "firstpersonmod";
    private static final String MOD_ID = "firstperson";
    private static boolean INSTALLED;

    public static void init() {
        INSTALLED = LoadingModList.get().getModFileById(MOD_ID) != null || LoadingModList.get().getModFileById(LEGACY_MOD_ID) != null;
        if (INSTALLED) {
            registerOffset();
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static void hideHead(IBone head) {
        head.setHidden(shouldHideHead());
    }

    private static void registerOffset() {
        FirstPersonAPI.registerPlayerHandler((PlayerOffsetHandler)(entity, delta, original, current) ->
                new Vec3(current.x(), 1.5 - CustomPlayerModel.FIRST_PERSON_HEAD_POS / 16, current.z()));
    }

    // Native Access
    public static boolean isRenderingPlayer() {
        return INSTALLED && RenderSystem.isOnRenderThread() && FirstPersonAPI.isRenderingPlayer();
    }

    public static boolean shouldHideHead() {
        return isRenderingPlayer();
    }

    public static boolean isEnabled() {
        return FirstPersonAPI.isEnabled();
    }
}