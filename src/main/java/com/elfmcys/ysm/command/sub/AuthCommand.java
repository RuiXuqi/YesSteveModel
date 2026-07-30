package com.elfmcys.ysm.command.sub;

import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.capability.ModelSelectionService;
import com.elfmcys.ysm.event.CommandRegistry;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.server.ServerModelService;
import com.elfmcys.ysm.network.NetworkHandler;
import com.elfmcys.ysm.network.forge.ControlHandler;
import com.elfmcys.ysm.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public final class AuthCommand {
    private static final String TARGETS = "targets";
    private static final String MODEL_PATH = "model_path";

    private AuthCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var auth = Commands.literal("auth").requires(source -> CommandUtil.hasPermission(source, 2));
        var targets = Commands.argument(TARGETS, EntityArgument.players());
        var path = Commands.argument(MODEL_PATH, StringArgumentType.string()).suggests(CommandRegistry.ALL_MODELS);
        auth.then(targets.then(Commands.literal("add").then(path.executes(context -> change(context, true)))));
        auth.then(Commands.argument(TARGETS, EntityArgument.players())
                .then(Commands.literal("remove").then(Commands.argument(MODEL_PATH, StringArgumentType.string())
                        .suggests(CommandRegistry.ALL_MODELS).executes(context -> change(context, false)))));
        auth.then(Commands.argument(TARGETS, EntityArgument.players())
                .then(Commands.literal("all").executes(AuthCommand::all)));
        auth.then(Commands.argument(TARGETS, EntityArgument.players())
                .then(Commands.literal("clear").executes(AuthCommand::clear)));
        return auth;
    }

    private static int change(CommandContext<CommandSourceStack> context, boolean add)
            throws CommandSyntaxException {
        var path = StringArgumentType.getString(context, MODEL_PATH);
        var snapshot = ServerModelService.instance().snapshot().orElseThrow();
        var model = snapshot.findPath(path);
        if (model.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown or ambiguous model path: " + path));
            return 0;
        }
        var hash = model.get().descriptor().modelHash();
        for (var player : EntityArgument.getPlayers(context, TARGETS)) {
            player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(capability -> {
                if (add) {
                    capability.addModel(hash);
                } else {
                    capability.removeModel(hash);
                    player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(selection -> {
                        if (hash.equals(selection.getModelHash())) {
                            ModelSelectionService.selectDefault(selection, snapshot);
                        }
                    });
                }
                NetworkHandler.sendToClientPlayer(ControlHandler.authorizedModels(capability.getAuthModels(), 1), player);
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int all(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var hashes = ServerModelService.instance().snapshot().orElseThrow().models().keySet();
        for (var player : EntityArgument.getPlayers(context, TARGETS)) {
            player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(capability -> {
                hashes.forEach(capability::addModel);
                NetworkHandler.sendToClientPlayer(ControlHandler.authorizedModels(capability.getAuthModels(), 1), player);
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var snapshot = ServerModelService.instance().snapshot().orElseThrow();
        for (var player : EntityArgument.getPlayers(context, TARGETS)) {
            player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(capability -> {
                capability.clear();
                player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(selection -> {
                    var selected = selection.getModelHash();
                    if (selected != null && snapshot.find(selected)
                            .map(handle -> handle.location().rootKind().accessPolicy()
                                    == AccessPolicy.SESSION_AUTHORIZED)
                            .orElse(false)) {
                        ModelSelectionService.selectDefault(selection, snapshot);
                    }
                });
                NetworkHandler.sendToClientPlayer(ControlHandler.authorizedModels(capability.getAuthModels(), 1), player);
            });
        }
        return Command.SINGLE_SUCCESS;
    }
}
