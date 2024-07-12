package com.elfmcys.yesstevemodel.mixin.plugin;

import com.elfmcys.yesstevemodel.client.compat.parcool.ParCoolCompat;
import com.elfmcys.yesstevemodel.util.Keep;
import com.google.common.collect.Lists;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinTweaker implements IMixinConfigPlugin {
    public MixinTweaker() {
        ParCoolCompat.init();
    }

    @Keep
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Keep
    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Keep
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Keep
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Keep
    @Override
    public List<String> getMixins() {
        if (ParCoolCompat.isInstalled() && FMLEnvironment.dist == Dist.CLIENT) {
            return Lists.newArrayList("client.parcool.AnimationAccessor",
                    "client.parcool.DodgeAnimatorAccessor",
                    "client.parcool.FlippingAnimatorAccessor",
                    "client.parcool.HorizontalWallRunAnimatorAccessor",
                    "client.parcool.RollAnimatorAccessor",
                    "client.parcool.SpeedVaultAnimatorAccessor",
                    "client.parcool.WallJumpAnimatorAccessor");
        } else {
            return null;
        }
    }

    @Keep
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Keep
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}