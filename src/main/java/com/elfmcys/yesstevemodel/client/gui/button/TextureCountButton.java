package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class TextureCountButton extends FlatColorButton {
    public TextureCountButton(int x, int y) {
        super(x, y, 20, 20, Component.empty(), (b) -> {
        });
    }

    @Override
    public Component getMessage() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getCapability(PlayerGeoCapabilityProvider.CAP).map(cap -> ClientModelManager.getModel(cap.getModelId()).map(model -> {
                String countText = String.valueOf(model.textures().size());
                return (Component) Component.literal(countText);
            }).orElseGet(super::getMessage)).orElse(super.getMessage());
        }
        return super.getMessage();
    }
}
