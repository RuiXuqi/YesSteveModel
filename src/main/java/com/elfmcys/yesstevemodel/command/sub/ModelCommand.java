package com.elfmcys.yesstevemodel.command.sub;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.event.CommandRegistry;
import com.elfmcys.yesstevemodel.model.ServerModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.CommandUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ModelCommand {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();
    private static final String MODEL_NAME = "model";
    private static final String RELOAD_NAME = "reload";
    private static final String SET_NAME = "set";
    private static final String TARGETS_NAME = "targets";
    private static final String MODEL_ID_NAME = "model_id";
    private static final String TEXTURE_ID_NAME = "texture_id";
    private static final String IGNORE_AUTH_NAME = "ignore_auth";
    private static final String EXPORT_NAME = "export";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> model = Commands.literal(MODEL_NAME);
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(RELOAD_NAME);
        model.then(reload.executes(ModelCommand::reloadAllPack));

        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal(SET_NAME);
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets = Commands.argument(TARGETS_NAME, EntityArgument.players());
        RequiredArgumentBuilder<CommandSourceStack, String> modelId = Commands.argument(MODEL_ID_NAME, StringArgumentType.string()).suggests(CommandRegistry.ALL_MODELS);
        RequiredArgumentBuilder<CommandSourceStack, String> textureId = Commands.argument(TEXTURE_ID_NAME, StringArgumentType.string()).suggests(CommandRegistry.ALL_TEXTURES);
        RequiredArgumentBuilder<CommandSourceStack, Boolean> ignoreAuth = Commands.argument(IGNORE_AUTH_NAME, BoolArgumentType.bool());

        model.then(set.then(targets.then(modelId.then(textureId.executes(context -> setModel(context, false))))));
        model.then(set.then(targets.then(modelId.then(textureId.then(ignoreAuth.executes(ModelCommand::setModelIgnoreAuth))))));

        LiteralArgumentBuilder<CommandSourceStack> export = Commands.literal(EXPORT_NAME);
        model.then(export.executes(ModelCommand::exportAllPackInfo));
        return model;
    }

    private static int setModelIgnoreAuth(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean ignoreAuth = BoolArgumentType.getBool(context, IGNORE_AUTH_NAME);
        return setModel(context, ignoreAuth);
    }

    private static int setModel(CommandContext<CommandSourceStack> context, boolean ignoreAuth) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS_NAME);
        String modelName = StringArgumentType.getString(context, MODEL_ID_NAME);
        String textureName = StringArgumentType.getString(context, TEXTURE_ID_NAME);
        if (!ServerModelManager.getModels().containsKey(modelName)) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.yes_steve_model.export.not_exist",
                    modelName), true);
            return Command.SINGLE_SUCCESS;
        }

        ServerModel info = ServerModelManager.getModels().get(modelName);
        if (info.textures().isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }

        if (ignoreAuth) {
            targets.forEach(player -> player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> {
                cap.setModelAndTexture(modelName, textureName);
                context.getSource().sendSuccess(() -> Component.translatable("message.yes_steve_model.model.set.success",
                        modelName, player.getScoreboardName()), true);
            }));
            return Command.SINGLE_SUCCESS;
        }

        targets.forEach(player -> player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap ->
                player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(authCap -> {
                    if (!ServerModelManager.getAuthModels().contains(modelName) || authCap.containModel(modelName)) {
                        cap.setModelAndTexture(modelName, textureName);
                        context.getSource().sendSuccess(() -> Component.translatable("message.yes_steve_model.model.set.success",
                                modelName, player.getScoreboardName()), true);
                    } else {
                        context.getSource().sendSuccess(() -> Component.translatable("message.yes_steve_model.model.set.need_auth",
                                modelName, player.getScoreboardName()), true);
                    }
                })));
        return Command.SINGLE_SUCCESS;
    }

    private static int exportAllPackInfo(CommandContext<CommandSourceStack> context) {
        String infoText = GSON.toJson(ServerModelManager.getModels());
        context.getSource().sendSuccess(() -> Component.literal(infoText), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadAllPack(final CommandContext<CommandSourceStack> context) {
        final StopWatch watch = StopWatch.createStarted();
        boolean queued = ServerModelManager.reloadAndSync(result -> {
            if (result.message() != null) {
                CommandUtil.sendAsyncFeedback(context.getSource(), CommandUtil.wrapMessage(result.message()), true);
            }
            if (result.success()) {
                CommandUtil.sendAsyncFeedback(context.getSource(), Component.translatable("message.yes_steve_model.model.reload.complete", watch.getTime(TimeUnit.MICROSECONDS) / 1000.0), true);
                watch.reset();
                watch.start();
            }
        }, result -> {
            watch.stop();
            if (!result.success()) {
                CommandUtil.sendAsyncFeedback(context.getSource(), CommandUtil.wrapMessage(result.error()), true);
            } else if (!result.playerErrors().isEmpty()) {
                // 这里可以获悉哪些玩家同步失败，但目前没用到
                for (Component error : result.playerErrors().values()) {
                    CommandUtil.sendAsyncFeedback(context.getSource(), CommandUtil.wrapMessage(error), true);
                }
                if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                    CommandUtil.sendAsyncFeedback(context.getSource(), Component.translatable("message.yes_steve_model.model.sync.complete", watch.getTime(TimeUnit.MICROSECONDS) / 1000.0), true);
                }
            }
        });
        if (!queued) {
            // 有其它重载任务正在进行
            context.getSource().sendFailure(Component.translatable("message.yes_steve_model.model.reload.in_progress"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
