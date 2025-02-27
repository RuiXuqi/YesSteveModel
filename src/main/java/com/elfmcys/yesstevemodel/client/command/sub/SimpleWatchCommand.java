package com.elfmcys.yesstevemodel.client.command.sub;

import com.elfmcys.yesstevemodel.capability.PlayerGeoCapabilityProvider;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.gui.DebugAnimationScreen;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
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

import java.util.List;
import java.util.function.Supplier;

import static com.elfmcys.yesstevemodel.client.command.ClientRootCommand.ALL_CONTROLLERS;
import static com.elfmcys.yesstevemodel.client.command.ClientRootCommand.ALL_VARS;

public class SimpleWatchCommand {
    private static final String WATCH_NAME = "watch";

    private static final String VAR_NAME = "var";
    private static final String STATE_NAME = "state";
    private static final String CLEAR_NAME = "clear";

    private static final String EXPRESSION_NAME = "exp";
    private static final String CONTROLLER_NAME = "controller";

    /**
     * 简化版本
     * ysmclient watch var <exp> 等效于 ysmclient molang watch add post <exp_name> <exp>
     */
    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> watch = Commands.literal(WATCH_NAME);

        LiteralArgumentBuilder<CommandSourceStack> var = Commands.literal(VAR_NAME);
        LiteralArgumentBuilder<CommandSourceStack> state = Commands.literal(STATE_NAME);
        LiteralArgumentBuilder<CommandSourceStack> clear = Commands.literal(CLEAR_NAME);

        Supplier<RequiredArgumentBuilder<CommandSourceStack, String>> exp = () -> Commands.argument(EXPRESSION_NAME, StringArgumentType.greedyString()).suggests(ALL_VARS);
        Supplier<RequiredArgumentBuilder<CommandSourceStack, String>> controller = () -> Commands.argument(CONTROLLER_NAME, StringArgumentType.string()).suggests(ALL_CONTROLLERS);

        watch.then(var.then(exp.get().executes(SimpleWatchCommand::addExpression)));
        watch.then(state.then(controller.get().executes(SimpleWatchCommand::getState)));
        watch.then(clear.executes(SimpleWatchCommand::clearExpression));

        return watch;
    }

    @SuppressWarnings("all")
    private static int addExpression(CommandContext<CommandSourceStack> ctx) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        Minecraft mc = Minecraft.getInstance();
        String exp = StringArgumentType.getString(ctx, EXPRESSION_NAME);
        IValue value;
        try {
            value = CustomMolangParser.parseSingleExpressionUnsafe(exp);
        } catch (ParseException e) {
            ctx.getSource().sendFailure(Component.translatable("message.yes_steve_model.model.debug_animation.parser_error", e.getMessage()));
            return Command.SINGLE_SUCCESS;
        }
        mc.execute(() -> mc.player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            cap.getDebugInfo().add(DebugInfo.Phase.POST_ANIMATION, exp, value);
            // 强制打开调试界面
            DebugAnimationKey.TYPE = DebugAnimationKey.DebugType.CUSTOM;
            cap.getDebugInfo().setEnabled(true);
        }));

        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("all")
    private static int clearExpression(CommandContext<CommandSourceStack> ctx) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.player.getCapability(PlayerGeoCapabilityProvider.CAP)
                .ifPresent(cap -> cap.getDebugInfo().clear()));
        DebugAnimationScreen.clearDebugControllerIndex();
        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("all")
    private static int getState(CommandContext<CommandSourceStack> ctx) {
        if (!isClientReady()) {
            return Command.SINGLE_SUCCESS;
        }
        Minecraft mc = Minecraft.getInstance();
        String controllerName = StringArgumentType.getString(ctx, CONTROLLER_NAME);
        mc.execute(() -> mc.player.getCapability(PlayerGeoCapabilityProvider.CAP).ifPresent(cap -> {
            List<IAnimationController> controllers = cap.getAnimationData().getAnimationControllers();
            for (int index = 0; index < controllers.size(); index++) {
                IAnimationController controller = controllers.get(index);
                if (controller.getName().equals(controllerName)) {
                    DebugAnimationScreen.addDebugControllerIndex(index);
                }
            }
            // 强制打开调试界面
            DebugAnimationKey.TYPE = DebugAnimationKey.DebugType.CUSTOM;
            cap.getDebugInfo().setEnabled(true);
        }));

        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("ConstantValue")
    private static boolean isClientReady() {
        return Minecraft.getInstance() != null && Minecraft.getInstance().player != null;
    }
}
