package com.elfmcys.ysm.command.sub;

import com.elfmcys.ysm.util.CommandUtil;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ExportCommand {
    private static final String DISABLED_MESSAGE =
            "commands.yes_steve_model.export.unstable";

    private ExportCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("export")
                .requires(source -> CommandUtil.hasPermission(source, 2))
                .executes(ExportCommand::reject)
                .then(Commands.argument("ignored", StringArgumentType.greedyString())
                        .executes(ExportCommand::reject));
    }

    private static int reject(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable(DISABLED_MESSAGE));
        return 0;
    }
}
