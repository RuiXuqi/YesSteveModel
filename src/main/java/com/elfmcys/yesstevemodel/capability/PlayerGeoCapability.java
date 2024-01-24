package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.instance.CustomPlayerInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.CameraType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerGeoCapability extends CustomPlayerInstance {
    public PlayerGeoCapability(AbstractClientPlayer player) {
        super(player, true);
    }

    @Override
    public boolean canUpdateAsync() {
        if (animatable.getEntity() instanceof LocalPlayer) {
            // 在第一人称下，如果安装了第一人称模组并启用，则异步更新是多余的
            return Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON || !FirstPersonCompat.isInstalled() || !FirstPersonCompat.isEnabled();
        }
        return true;
    }
}
