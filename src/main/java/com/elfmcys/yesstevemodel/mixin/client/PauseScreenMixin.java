package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.gui.AndroidCompat;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At(value = "TAIL"))
    private void ysmInit(CallbackInfo ci) {
        Button button = AndroidCompat.addYsmSkinButton((PauseScreen) (Object) this);
        if (button != null) {
            this.addRenderableWidget(button);
        }
    }
}
