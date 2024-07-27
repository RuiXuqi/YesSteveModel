package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerGeoCapability extends CustomPlayerInstance {
    public PlayerGeoCapability(AbstractClientPlayer player) {
        super(player, true, player instanceof LocalPlayer);
    }

    private boolean isFirstPersonModActive() {
        if (animatable.getEntity() instanceof LocalPlayer) {
            return Minecraft.getInstance().options.getCameraType().isFirstPerson() && FirstPersonCompat.isInstalled() && FirstPersonCompat.isEnabled();
        }
        return false;
    }

    @Override
    public boolean canUpdateAsync() {
        // 在第一人称下，如果安装了第一人称模组并启用，则异步更新是多余的
        return !isFirstPersonModActive();
    }
}
