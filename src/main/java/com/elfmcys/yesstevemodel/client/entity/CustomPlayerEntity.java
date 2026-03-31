package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventWrapper;
import com.elfmcys.yesstevemodel.client.compat.IrisCompat;
import com.elfmcys.yesstevemodel.client.controller.collections.PlayerControllerCollection;
import com.elfmcys.yesstevemodel.client.model.ClientModel;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimationState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.SetPlayAnimation;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class CustomPlayerEntity extends CustomHumanoidEntity<Player> implements IRoamingEntity {
    protected final boolean localPlayer;

    protected boolean isPlayingExtraAnimation = false;
    protected String extraAnimationName = "idle";
    protected boolean isExtraAnimationDirty = false;

    private List<IValue> syncHandler = null;

    public CustomPlayerEntity(Player player, boolean localPlayer, boolean asyncUpdate) {
        super(player, asyncUpdate);
        this.localPlayer = localPlayer;
        if (player instanceof LocalPlayer) {
            setInitialized();
        }
    }

    @Override
    protected void onSetupAnimationController() {
        getModelContainer().playerModel().playerControllerFactory().accept(this);
    }

    @Override
    protected boolean isImmutableRender(AnimationEvent<?> animEvent) {
        // 例外：local player 渲染 iris 阴影时应恒为第三人称，不能视为 immutable
        return animEvent.isRenderingInLevelExclusive() || (!localPlayer && IrisCompat.isRenderingShadow());
    }

    @Nullable
    public Struct getRoamingStruct() {
        return null;
    }

    public boolean isLocalPlayer() {
        return localPlayer;
    }

    @Override
    protected void onLoadModelContainer(ClientModel newModel) {
        super.onLoadModelContainer(newModel);
        syncHandler = newModel.assets().eventHandlers().get(MolangEventWrapper.SYNC);
    }

    @Override
    protected void resetModelContainer() {
        super.resetModelContainer();
        syncHandler = null;
    }

    @Override
    protected void resetGeoModel() {
        super.resetGeoModel();
        isPlayingExtraAnimation = false;
        extraAnimationName = "idle";
        isExtraAnimationDirty = false;
    }

    public void playExtraAnimation(String animationName) {
        if (getAnimation(animationName)!= null) {
            this.extraAnimationName = animationName;
            this.isPlayingExtraAnimation = true;
            this.isExtraAnimationDirty = true;
        } else {
            this.isPlayingExtraAnimation = false;
        }
    }

    public void clearExtraAnimationDirty() {
        this.isExtraAnimationDirty = false;
    }

    public boolean isPlayingExtraAnimation() {
        return isPlayingExtraAnimation;
    }

    public boolean shouldResetExtraAnimation() {
        return isExtraAnimationDirty;
    }

    public String getExtraAnimationName() {
        return this.extraAnimationName;
    }

    public void stopExtraAnimation() {
        this.isPlayingExtraAnimation = false;
    }

    @Override
    protected void preAnimationSetup(float seekTime, boolean shouldTick) {
        super.preAnimationSetup(seekTime, shouldTick);
        // 设置 roaming 变量
        getAnimationProcessor().putRemoteStruct(getRoamingStruct());
    }

    @Override
    protected void postAnimationSetup(float seekTime, boolean shouldTick) {
        super.postAnimationSetup(seekTime, shouldTick);
        if (localPlayer && shouldTick) {
            if (isPlayingExtraAnimation() && getCodedAnimationStates(PlayerControllerCollection.CAP_CONTROLLER) == AnimationState.IDLE) {
                stopExtraAnimation();
                if (NetworkHandler.isRemoteChannelPresent()) {
                    NetworkHandler.sendToServer(SetPlayAnimation.stop());
                }
            }
        }
    }

    public void molangSync(FloatArrayList args) {
        if (syncHandler != null) {
            executeMolangExp(MolangEventWrapper.wrap(syncHandler, args), true, false, null);
        }
    }
}
