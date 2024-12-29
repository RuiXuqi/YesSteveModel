package com.elfmcys.yesstevemodel.command;

import com.elfmcys.yesstevemodel.command.sub.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RootCommand {
    private static final String ROOT_NAME = "ysm";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME);
        root.then(ModelCommand.get());
        root.then(AuthCommand.get());
        root.then(ExportCommand.get());
        root.then(PlayAnimationCommand.get());
        root.then(MolangCommand.get());
        root.then(PingCommand.get());
        dispatcher.register(root);
    }
}
