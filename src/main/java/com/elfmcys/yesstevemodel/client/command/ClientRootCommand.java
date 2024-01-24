package com.elfmcys.yesstevemodel.client.command;

import com.elfmcys.yesstevemodel.client.command.sub.MolangCommand;
import com.elfmcys.yesstevemodel.util.CommandUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ClientRootCommand {
    private static final String ROOT_NAME = "ysmclient";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .requires(source -> CommandUtil.isLocalPlayer(source.getEntity()));
        root.then(MolangCommand.get());
        dispatcher.register(root);
    }
}
