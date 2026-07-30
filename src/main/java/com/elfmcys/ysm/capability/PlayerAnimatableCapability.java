package com.elfmcys.ysm.capability;

import com.elfmcys.ysm.client.compat.FirstPersonCompat;
import com.elfmcys.ysm.client.compat.bettercombat.BetterCombatCompat;
import com.elfmcys.ysm.client.entity.CustomPlayerEntity;
import com.elfmcys.ysm.client.model.ModelRenderTarget;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.ysm.geckolib3.model.AnimatableEntity;
import com.elfmcys.ysm.geckolib3.model.AnimatedGeoModel;
import com.elfmcys.ysm.molang.runtime.Struct;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class PlayerAnimatableCapability extends CustomPlayerEntity {
    private final ClientRoamingSession roamingSession;

    public PlayerAnimatableCapability(Player player) {
        super(player, player instanceof LocalPlayer, true);
        roamingSession = new ClientRoamingSession(
                player, player instanceof LocalPlayer, this::reloadGeoModel);
    }

    @Override
    protected PlayerStateTracker createStateTracker(Player entity) {
        return new PlayerStateTracker(entity, entity instanceof LocalPlayer);
    }

    public PlayerStateTracker getStateTracker() {
        return (PlayerStateTracker) super.getStateTracker();
    }

    @Override
    public @Nullable Struct getRoamingStruct() {
        return roamingSession.currentStruct();
    }

    @Override
    protected void onModelRenderTargetLoaded(ModelRenderTarget newModel) {
        super.onModelRenderTargetLoaded(newModel);
        roamingSession.modelLoaded(getModelRenderTarget().info().hashShort());
    }

    @Override
    protected void resetModelRenderTarget() {
        roamingSession.modelReset();
        super.resetModelRenderTarget();
    }

    @Override
    public void onLoadGeoModel(AnimatedGeoModel model) {
        super.onLoadGeoModel(model);
        roamingSession.geoModelLoaded();
    }

    @Override
    protected void resetGeoModel() {
        roamingSession.geoModelReset();
        super.resetGeoModel();
    }

    @Override
    protected void codeAnimation(AnimationEvent<? extends AnimatableEntity<Player>> animationEvent, boolean update) {
        super.codeAnimation(animationEvent, update);

        // 更新第一人称相机偏移与头部隐藏
        AnimatedGeoModel model = getLoadedGeoModel();
        if (model != null && isLocalPlayer()) {
            var ctx = animationEvent.getRenderContext();
            if (ctx.firstPersonMod()) {
                if (model.getFirstPersonHead() != null) {
                    model.getFirstPersonHead().setHidden(true);
                }
                if (model.getFirstPersonViewLocator() != null) {
                    FirstPersonCompat.setHeadPos(model.getFirstPersonViewLocator().getPivot().y * getHeightScale());
                } else if (update) {
                    // TODO
                    /*
                    if (!model.headBones().isEmpty()) {
                        var head = model.headBones().get(model.headBones().size() - 1);
                        FirstPersonCompat.setHeadPos(head == null ? 24f : (head.getPivotY() * getHeightScale()));
                    }
                    */
                }
            }
        }
    }

    @Override
    protected void recoverLastCodedAnimation(boolean lastFrameUpdated) {
        super.recoverLastCodedAnimation(lastFrameUpdated);

        AnimatedGeoModel model = getLoadedGeoModel();
        if (model != null && isLocalPlayer()) {
            if ((FirstPersonCompat.isInstalled() || BetterCombatCompat.isInstalled()) && model.getFirstPersonHead() != null) {
                model.getFirstPersonHead().setHidden(false);
            }
        }
    }

    public void resetRoamingVars(int modelHashShort, Int2FloatOpenHashMap vars) {
        roamingSession.resetFromServer(modelHashShort, vars);
    }

    public boolean hasRoamingStorage(int hashShort) {
        return roamingSession.hasStorage(hashShort);
    }

    public void updateRemoteRoamingVars(int modelHashShort, Int2FloatMap vars) {
        roamingSession.updateRemote(modelHashShort, vars);
    }

    public void handleRoamingVarsChanges() {
        roamingSession.flushLocalChanges();
    }

    public RoamingSnapshot roamingSnapshot(int authoritativeModelKey) {
        return roamingSession.snapshot(authoritativeModelKey);
    }

    public record RoamingSnapshot(int modelKey, Int2FloatOpenHashMap values) {
    }

    public void moveFrom(PlayerAnimatableCapability source) {
        roamingSession.moveFrom(source.roamingSession);
        updateModelAndTexture(source.getModelHash(), source.textureName);
        setDisabled(source.isDisabled());
    }

    @Override
    @NotNull
    protected HumanoidResourceHolder createResourceHolder(ModelRenderTargetLease lease, boolean isFallback) {
        return new HumanoidResourceHolder(lease, isFallback, true, true, 30 * 20);
    }

}
