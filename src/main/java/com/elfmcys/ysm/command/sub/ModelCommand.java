package com.elfmcys.ysm.command.sub;

import com.elfmcys.ysm.capability.AuthModelsCapabilityProvider;
import com.elfmcys.ysm.capability.ModelInfoCapabilityProvider;
import com.elfmcys.ysm.event.CommandRegistry;
import com.elfmcys.ysm.model.source.AccessPolicy;
import com.elfmcys.ysm.model.server.ServerModelService;
import com.elfmcys.ysm.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class ModelCommand {
    private static final String TARGETS = "targets";
    private static final String MODEL_PATH = "model_path";
    private static final String TEXTURE = "texture";
    private static final String IGNORE_AUTH = "ignore_auth";

    private ModelCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var model = Commands.literal("model").requires(source -> CommandUtil.hasPermission(source, 2));
        model.then(Commands.literal("reload").executes(ModelCommand::reload));
        model.then(Commands.literal("errors").executes(ModelCommand::errors));
        model.then(Commands.literal("disable")
                .then(Commands.argument(TARGETS, EntityArgument.players())
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(ModelCommand::disable))));
        var set = Commands.literal("set")
                .then(Commands.argument(TARGETS, EntityArgument.players())
                        .then(Commands.argument(MODEL_PATH, StringArgumentType.string())
                                .suggests(CommandRegistry.ALL_MODELS)
                                .then(Commands.argument(TEXTURE, StringArgumentType.string())
                                        .suggests(CommandRegistry.ALL_TEXTURES)
                                        .executes(context -> set(context, false))
                                        .then(Commands.argument(IGNORE_AUTH, BoolArgumentType.bool())
                                                .executes(context -> set(context,
                                                        BoolArgumentType.getBool(context, IGNORE_AUTH)))))));
        model.then(set);
        return model;
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean ignoreAuth)
            throws CommandSyntaxException {
        var path = StringArgumentType.getString(context, MODEL_PATH);
        var model = ServerModelService.instance().snapshot().flatMap(snapshot -> snapshot.findPath(path));
        if (model.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown or ambiguous model path: " + path));
            return 0;
        }
        var handle = model.get();
        var texture = StringArgumentType.getString(context, TEXTURE);
        if ("-".equals(texture)) {
            var requested = handle.view().getManifest().getInfo().getSettings().getDefaultTexture();
            texture = handle.view().getPlayer().getTextureNames().contains(requested)
                    ? requested : handle.view().getPlayer().getTextureNames().stream().sorted().findFirst().orElse("");
        }
        if (!handle.view().getPlayer().getTextureNames().contains(texture)) {
            context.getSource().sendFailure(Component.literal("Unknown texture for " + path + ": " + texture));
            return 0;
        }

        var hash = handle.descriptor().modelHash();
        var selectedTexture = texture;
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS);
        for (var player : targets) {
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(selection ->
                    player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(auth -> {
                        if (!ignoreAuth && handle.location().rootKind().accessPolicy()
                                == AccessPolicy.SESSION_AUTHORIZED
                                && !auth.containModel(hash)) {
                            context.getSource().sendFailure(Component.literal(
                                    player.getScoreboardName() + " is not authorized for " + path));
                            return;
                        }
                        selection.setModelAndTexture(hash, selectedTexture);
                        selection.setMandatory(true);
                        selection.stopAnimation(player);
                    }));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Reloading model catalog..."), true);
        ServerModelService.instance().reload(true).thenAccept(result ->
                CommandUtil.sendAsyncFeedback(context.getSource(), Component.literal(result.success()
                        ? "Model catalog reloaded: " + result.modelCount() + " model(s), "
                        + result.errorCount() + " scan error(s)"
                        : "Model catalog reload failed: " + result.message()), true));
        return Command.SINGLE_SUCCESS;
    }

    private static int errors(CommandContext<CommandSourceStack> context) {
        var errors = ServerModelService.instance().lastReport().errors();
        if (errors.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("The last model scan had no errors."), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Component.literal("Model scan errors: " + errors.size()), false);
        for (var error : errors) {
            var text = "[" + error.rootKind() + "] " + error.source() + ": " + error.message();
            if (!error.detail().isBlank()) {
                text += "\n" + error.detail();
            }
            var output = text;
            context.getSource().sendSuccess(() -> Component.literal(output), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var disabled = BoolArgumentType.getBool(context, "value");
        for (var player : EntityArgument.getPlayers(context, TARGETS)) {
            player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP)
                    .ifPresent(capability -> capability.setDisabled(disabled));
        }
        return Command.SINGLE_SUCCESS;
    }
}
