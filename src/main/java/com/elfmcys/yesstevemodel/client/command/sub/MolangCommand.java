package com.elfmcys.yesstevemodel.client.command.sub;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.DebugInfo;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import static com.elfmcys.yesstevemodel.client.command.ClientRootCommand.ALL_VARS;

public class MolangCommand {
    private static final String MOLANG_NAME = "molang";

    private static final String WATCH_NAME = "watch";
    private static final String ADD_NAME = "add";
    private static final String PRE_NAME = "pre";
    private static final String POST_NAME = "post";
    private static final String CLEAR_NAME = "clear";
    private static final String REMOVE_NAME = "remove";

    private static final String EXPRESSION_NAME_NAME = "exp_name";
    private static final String EXPRESSION_NAME = "exp";

    private static final String EXECUTE_NAME = "execute";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> molang = Commands.literal(MOLANG_NAME);

        LiteralArgumentBuilder<CommandSourceStack> watch = Commands.literal(WATCH_NAME);
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal(ADD_NAME);
        LiteralArgumentBuilder<CommandSourceStack> pre = Commands.literal(PRE_NAME);
        LiteralArgumentBuilder<CommandSourceStack> post = Commands.literal(POST_NAME);
        LiteralArgumentBuilder<CommandSourceStack> clear = Commands.literal(CLEAR_NAME);
        LiteralArgumentBuilder<CommandSourceStack> remove = Commands.literal(REMOVE_NAME);

        LiteralArgumentBuilder<CommandSourceStack> execute = Commands.literal(EXECUTE_NAME);

        Supplier<RequiredArgumentBuilder<CommandSourceStack, String>> expName = () -> Commands.argument(EXPRESSION_NAME_NAME, StringArgumentType.string());
        Supplier<RequiredArgumentBuilder<CommandSourceStack, String>> exp = () -> Commands.argument(EXPRESSION_NAME, StringArgumentType.greedyString()).suggests(ALL_VARS);

        watch.then(add.then(pre.then(expName.get().then(exp.get().executes(ctx -> addExpression(ctx, DebugInfo.Phase.PRE_ANIMATION)))))
                      .then(post.then(expName.get().then(exp.get().executes(ctx -> addExpression(ctx, DebugInfo.Phase.POST_ANIMATION))))))
                .then(remove.then(expName.get().executes(MolangCommand::removeExpression)))
                .then(clear.executes(MolangCommand::clearExpression));
        molang.then(watch)
              .then(execute.then(exp.get().executes(MolangCommand::executeMolang)));

        return molang;
    }

    private static int addExpression(CommandContext<CommandSourceStack> ctx, DebugInfo.Phase phase) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        String expName = StringArgumentType.getString(ctx, EXPRESSION_NAME_NAME);
        String exp = StringArgumentType.getString(ctx, EXPRESSION_NAME);
        IValue value;
        try {
            value = CustomMolangParser.parseSingleExpressionUnsafe(exp);
        } catch (ParseException e) {
            ctx.getSource().sendFailure(Component.translatable("message.yes_steve_model.model.debug_animation.parser_error", e.getMessage()));
            return Command.SINGLE_SUCCESS;
        }
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                    cap.getDebugInfo().add(phase, expName, value);
                }));

        return Command.SINGLE_SUCCESS;
    }

    private static int removeExpression(CommandContext<CommandSourceStack> ctx) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        String expName = StringArgumentType.getString(ctx, EXPRESSION_NAME_NAME);
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                    cap.getDebugInfo().remove(expName);
                }));

        return Command.SINGLE_SUCCESS;
    }

    private static int clearExpression(CommandContext<CommandSourceStack> ctx) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
                    cap.getDebugInfo().clear();
                }));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMolang(CommandContext<CommandSourceStack> ctx) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        String exp = StringArgumentType.getString(ctx, EXPRESSION_NAME);
        IValue value;
        try {
            value = CustomMolangParser.parseSingleExpressionUnsafe(exp);
        } catch (ParseException e) {
            ctx.getSource().sendFailure(Component.translatable("message.yes_steve_model.model.debug_animation.parser_error", e.getMessage()));
            return Command.SINGLE_SUCCESS;
        }

        Minecraft.getInstance().player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.executeMolangExp(value, true, false, result -> {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.yes_steve_model.model.debug_animation.result", result));
            });
        });

        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("ConstantValue")
    private static boolean isClientReady() {
        return Minecraft.getInstance() != null && Minecraft.getInstance().player != null;
    }
}
