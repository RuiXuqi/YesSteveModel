package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class VanillaPlayerRenderEvent {
    private static final ResourceLocation STEVE_SKIN_LOCATION = new ResourceLocation("textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_SKIN_LOCATION = new ResourceLocation("textures/entity/player/slim/alex.png");
    private static final String STEVE = "steve";
    private static final String ALEX = "alex";

    public static void onRenderPlayer(SpecialPlayerRenderEvent event) {
        Player player = event.getPlayer();
        CustomPlayerEntity animatable = event.getCustomPlayer();
        if (isVanillaPlayer(event.getModelId()) && player instanceof AbstractClientPlayer) {
            AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
            ResourceLocation location;
            Minecraft minecraft = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(clientPlayer.getGameProfile());
            if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                location = minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            } else {
                location = getDefaultSkin(event.getModelId());
            }
            animatable.setTexture(location);
        }
    }

    private static boolean isVanillaPlayer(ResourceLocation modelId) {
        return modelId.getPath().equals(STEVE) || modelId.getPath().equals(ALEX);
    }

    private static ResourceLocation getDefaultSkin(ResourceLocation modelId) {
        return modelId.getPath().equals(STEVE) ? STEVE_SKIN_LOCATION : ALEX_SKIN_LOCATION;
    }
}
