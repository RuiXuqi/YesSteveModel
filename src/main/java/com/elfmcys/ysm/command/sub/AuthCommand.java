package com.elfmcys.ysm.command.sub;

import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.event.CommandRegistry;
import com.elfmcys.ysm.model.ServerModelManager;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.message.SyncAuthModels;
import com.elfmcys.ysm.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class AuthCommand {
    private static final String AUTH_NAME = "auth";
    private static final String ADD_NAME = "add";
    private static final String REMOVE_NAME = "remove";
    private static final String ALL_NAME = "all";
    private static final String CLEAR_NAME = "clear";
    private static final String TARGETS_NAME = "targets";
    private static final String MODEL_ID_NAME = "model_id";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> auth = Commands.literal(AUTH_NAME).requires(src -> CommandUtil.hasPermission(src, 2));
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets = Commands.argument(TARGETS_NAME, EntityArgument.players());
        RequiredArgumentBuilder<CommandSourceStack, String> modelId = Commands.argument(MODEL_ID_NAME, StringArgumentType.string()).suggests(CommandRegistry.ALL_MODELS);

        auth.then(targets.then(Commands.literal(ADD_NAME).then(modelId.executes(AuthCommand::addAuthModel))));
        auth.then(targets.then(Commands.literal(REMOVE_NAME).then(modelId.executes(AuthCommand::removeAuthModel))));
        auth.then(targets.then(Commands.literal(ALL_NAME).executes(AuthCommand::addAllAuthModel)));
        auth.then(targets.then(Commands.literal(CLEAR_NAME).executes(AuthCommand::clearAuthModel)));
        return auth;
    }

    private static int addAuthModel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS_NAME);
        String modelId = StringArgumentType.getString(context, MODEL_ID_NAME);
        if (!ServerModelManager.getModels().containsKey(modelId)) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.yes_steve_model.export.not_exist",
                    modelId), true);
            return Command.SINGLE_SUCCESS;
        }
        targets.forEach(player -> player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(cap -> {
            cap.addModel(modelId);
            NetworkHandler.sendToClientPlayer(new SyncAuthModels(cap.getAuthModels()), player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.yes_steve_model.auth_model.add.info",
                    modelId, player.getScoreboardName()), true);
        }));
        return Command.SINGLE_SUCCESS;
    }

    private static int addAllAuthModel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS_NAME);
        targets.forEach(player -> player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(cap -> {
            ServerModelManager.getModels().keySet().forEach(cap::addModel);
            NetworkHandler.sendToClientPlayer(new SyncAuthModels(cap.getAuthModels()), player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.yes_steve_model.auth_model.all.info",
                    player.getScoreboardName()), true);
        }));
        return Command.SINGLE_SUCCESS;
    }

    private static int removeAuthModel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS_NAME);
        String modelId = StringArgumentType.getString(context, MODEL_ID_NAME);
        targets.forEach(player -> player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(ownModelsCap -> {
            ownModelsCap.removeModel(modelId);
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> {
                if (ServerModelManager.getAuthModels().contains(modelIdCap.getModelId()) && !ownModelsCap.containModel(modelIdCap.getModelId())) {
                    modelIdCap.setDefault();
                }
            });
            NetworkHandler.sendToClientPlayer(new SyncAuthModels(ownModelsCap.getAuthModels()), player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.yes_steve_model.auth_model.remove.info",
                    modelId, player.getScoreboardName()), true);
        }));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearAuthModel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS_NAME);
        targets.forEach(player -> player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(ownModelCap -> {
            ownModelCap.clear();
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(modelIdCap -> {
                if (ServerModelManager.getAuthModels().contains(modelIdCap.getModelId())) {
                    modelIdCap.setDefault();
                }
            });
            NetworkHandler.sendToClientPlayer(new SyncAuthModels(ownModelCap.getAuthModels()), player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.yes_steve_model.auth_model.clear.info",
                    player.getScoreboardName()), true);
        }));
        return Command.SINGLE_SUCCESS;
    }
}
