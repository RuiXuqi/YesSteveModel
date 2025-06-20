package com.elfmcys.yesstevemodel.client.animation.molang.functions;

import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapability;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.function.entity.PlayerEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.EmitMolangSync;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

public class Sync extends PlayerEntityFunction {
    private static final int MAX_ARGS_SIZE = 16;

    @Override
    protected Object eval(ExecutionContext<IContext<AbstractClientPlayer>> context, ArgumentCollection arguments) {
        if (!context.entity().allowEmitting()) {
            return null;
        }

        if (context.entity().animatableEntity() instanceof PlayerAnimatableCapability) {
            // 确认不是 GUI 渲染用
            if (NetworkHandler.isRemoteChannelPresent()) {
                // 确认服务端已安装本模组
                if (context.entity().entity() instanceof LocalPlayer) {
                    // 确认为 LocalPlayer 实体，向服务器发起同步
                    NetworkHandler.sendToServer(new EmitMolangSync(packArguments(context, arguments)));
                    return null;
                } else {
                    // 确认为 RemotePlayer 实体，不执行而是等待服务端下发
                    return null;
                }
            } else {
                // 服务端未安装，在本地触发同步事件
            }
        } else {
            // GUI 渲染用，在本地触发同步事件
        }

        if (context.entity().animatableEntity() instanceof CustomPlayerEntity animatableEntity) {
            // 触发同步事件
            animatableEntity.molangSync(packArguments(context, arguments));
        } else {
            // 应该没有这种情况
        }

        return null;
    }

    private static FloatArrayList packArguments(ExecutionContext<IContext<AbstractClientPlayer>> context, ArgumentCollection arguments) {
        var args = new FloatArrayList(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            args.add(arguments.getAsFloat(context, i));
        }
        return args;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size <= MAX_ARGS_SIZE;
    }
}
