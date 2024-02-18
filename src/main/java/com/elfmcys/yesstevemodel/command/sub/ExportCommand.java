package com.elfmcys.yesstevemodel.command.sub;

import com.elfmcys.yesstevemodel.event.CommandRegistry;
import com.elfmcys.yesstevemodel.model.ExportModelResult;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.CommandUtil;
import com.elfmcys.yesstevemodel.util.ThreadTools;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ExportCommand {
    private static final String EXPORT_NAME = "export";
    private static final String MODEL_ID_NAME = "model_id";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> export = Commands.literal(EXPORT_NAME);
        RequiredArgumentBuilder<CommandSourceStack, String> modelId = Commands.argument(MODEL_ID_NAME, StringArgumentType.string()).suggests(CommandRegistry.ALL_MODELS);
        export.then(modelId.executes(ExportCommand::exportModel));
        return export;
    }

    private static int exportModel(final CommandContext<CommandSourceStack> context) {
        final String modelName = StringArgumentType.getString(context, MODEL_ID_NAME);
        ThreadTools.submit(() -> {
            ExportModelResult result = ServerModelManager.exportModel(modelName);
            if (result.message() != null) {
                CommandUtil.sendAsyncFeedback(context.getSource(), CommandUtil.wrapMessage(result.message()), true);
            }
            if (result.success()) {
                CommandUtil.sendAsyncFeedback(context.getSource(), Component.translatable("commands.yes_steve_model.export.success", result.filePath()), true);
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
