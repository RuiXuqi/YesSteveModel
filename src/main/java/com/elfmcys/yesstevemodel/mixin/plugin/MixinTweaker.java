package com.elfmcys.yesstevemodel.mixin.plugin;

import com.elfmcys.yesstevemodel.client.compat.create.CreateCompat;
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
        CreateCompat.init();
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            List<String> list = Lists.newArrayList();

            if (ParCoolCompat.isInstalled()) {
                list.add("client.parcool.AnimationAccessor");
                list.add("client.parcool.DodgeAnimatorAccessor");
                list.add("client.parcool.FlippingAnimatorAccessor");
                list.add("client.parcool.HorizontalWallRunAnimatorAccessor");
                list.add("client.parcool.RollAnimatorAccessor");
                list.add("client.parcool.SpeedVaultAnimatorAccessor");
                list.add("client.parcool.WallJumpAnimatorAccessor");
            }

            if (CreateCompat.isInstalled()) {
                list.add("client.create.PlayerSkyhookRendererAccessor");
            }

            if (list.isEmpty()) {
                return null;
            } else {
                return list;
            }
        }
        return null;
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