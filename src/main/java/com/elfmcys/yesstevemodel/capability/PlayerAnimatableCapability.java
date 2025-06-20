package com.elfmcys.yesstevemodel.capability;

import com.elfmcys.yesstevemodel.client.compat.FirstPersonCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerAnimatableCapability extends CustomPlayerEntity {
    public PlayerAnimatableCapability(AbstractClientPlayer player) {
        super(player, player instanceof LocalPlayer, true);
    }

    private boolean isFirstPersonModActive() {
        if (isLocalPlayer()) {
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
