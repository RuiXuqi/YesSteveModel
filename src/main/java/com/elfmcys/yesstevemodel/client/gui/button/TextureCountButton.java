package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.data.ClientModelInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TextureCountButton extends FlatColorButton {
    public TextureCountButton(int x, int y) {
        super(x, y, 20, 20, Component.empty(), (b) -> {
        });
    }

    @Override
    public Component getMessage() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getCapability(PlayerGeoCapabilityProvider.CAP).map(cap -> {
                ResourceLocation modelId = cap.getModelId();
                ClientModelInfo modelInfo = ClientModelManager.getModelInfo().get(modelId);
                if (modelInfo != null) {
                    String countText = String.valueOf(modelInfo.textureIds().size());
                    return Component.literal(countText);
                }
                return super.getMessage();
            }).orElse(super.getMessage());
        }
        return super.getMessage();
    }
}
