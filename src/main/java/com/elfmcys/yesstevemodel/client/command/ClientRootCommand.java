package com.elfmcys.yesstevemodel.client.command;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.command.sub.MolangCommand;
import com.elfmcys.yesstevemodel.client.command.sub.SimpleWatchCommand;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.roaming.RoamingStruct;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import com.elfmcys.yesstevemodel.util.CommandUtil;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Set;

public class ClientRootCommand {
    private static final String ROOT_NAME = "ysmclient";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .requires(source -> CommandUtil.isLocalPlayer(source.getEntity()));
        root.then(MolangCommand.get());
        root.then(SimpleWatchCommand.get());
        dispatcher.register(root);
    }

    @SuppressWarnings("unchecked")
    public static final SuggestionProvider<CommandSourceStack> ALL_VARS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "vars"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider && FMLEnvironment.dist == Dist.CLIENT) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return Suggestions.empty();
            }
            Set<String> vars = Sets.newHashSet();
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
                // v 变量
                cap.getAnimationProcessor().visitScopedVariableNames(name -> {
                    vars.add(String.format("v.%s", name));
                });

                // v.roaming 变量
                Struct remoteStruct = cap.getRemoteStruct();
                if (remoteStruct instanceof RoamingStruct struct) {
                    struct.getAllName().forEach(s -> {
                        Object object = struct.getProperty(StringPool.getName(s));
                        if (object != null) {
                            vars.add(String.format("v.roaming.%s", s));
                        }
                    });
                }

                // 其他上下文变量
                CustomMolangParser.getAllBinding().forEach((key, value) -> {
                    if (value instanceof ContextBinding ctx) {
                        ctx.getAllName().forEach(s -> vars.add(String.format("%s.%s", key, s)));
                    }
                });

                // 自定义函数
                ClientModelManager.getModel(cap.getModelId()).ifPresent(model -> {
                    for (var name : model.userFunctions().keySet()) {
                        vars.add(String.format("fn.%s", StringPool.getString(name)));
                    }
                });
            });
            return SharedSuggestionProvider.suggest(vars, builder);
        }
        return Suggestions.empty();
    });

    public static final SuggestionProvider<CommandSourceStack> ALL_CONTROLLERS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "controllers"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider && FMLEnvironment.dist == Dist.CLIENT) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return Suggestions.empty();
            }
            Set<String> vars = Sets.newHashSet();
            player.getCapability(PlayerAnimatableCapabilityProvider.CAP)
                    .ifPresent(cap -> cap.getAnimationData()
                            .getAnimationControllers()
                            .forEach(c -> vars.add(c.getName())));
            return SharedSuggestionProvider.suggest(vars, builder);
        }
        return Suggestions.empty();
    });
}
