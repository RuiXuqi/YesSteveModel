package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.entity.IPreviewEntity;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.DebugSource;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class CustomGuiPlayerEntity extends CustomPlayerEntity implements IPreviewEntity {
    private final PreviewAnimationInfo guiAnimationInfo;
    private boolean allowEmitting;

    public CustomGuiPlayerEntity() {
        super(new FakePlayer(), false, false);
        guiAnimationInfo = new PreviewAnimationInfo();
    }

    @Override
    public @NotNull PreviewAnimationInfo getPreviewInfo() {
        return guiAnimationInfo;
    }

    @Override
    public void setAllowEmitting(boolean value) {
        this.allowEmitting = value;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected boolean allowEmitting() {
        return allowEmitting;
    }

    @Override
    public boolean isFakePlayer() {
        return true;
    }

    @Override
    protected AnimationEvent<?> performUpdate(float partialTicks) {
        if (entity instanceof FakePlayer fakePlayer && !fakePlayer.updateClientLevel()) {
            return null;
        }
        return super.performUpdate(partialTicks);
    }

    @Override
    public void waitForCapabilityUpdate() {
        if (entity instanceof LocalPlayer player) {
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(PlayerAnimatableCapability::waitForAsyncUpdate);
        }
    }

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    @Override
    public DebugSource getDebugSource() {
        return null;
    }

    @Override
    protected @NotNull HumanoidResourceHolder createResourceHolder(ClientModel model, boolean isFallback) {
        return new HumanoidResourceHolder(model, isFallback, false, true, 15 * 20);
    }

    private static class FakePlayer extends Player {
        @SuppressWarnings("DataFlowIssue")
        public FakePlayer() {
            super(Minecraft.getInstance().level, BlockPos.ZERO, 0, createRandomGameProfile());
        }

        private static GameProfile createRandomGameProfile() {
            var uuid = UUID.randomUUID();
            return new GameProfile(uuid, "ysm_" + uuid.toString().replace('-', '_'));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        public boolean updateClientLevel() {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                this.setLevel(level);
                return true;
            } else {
                return false;
            }
        }
    }
}
