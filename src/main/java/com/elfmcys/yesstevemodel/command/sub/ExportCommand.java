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
import org.jetbrains.annotations.Nullable;

public class ExportCommand {
    private static final String EXPORT_NAME = "export";
    private static final String MODEL_ID_NAME = "model_id";
    private static final String EXTRA_NAME = "extra";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> export = Commands.literal(EXPORT_NAME).requires(src -> CommandUtil.hasPermission(src, 2));
        RequiredArgumentBuilder<CommandSourceStack, String> modelId = Commands.argument(MODEL_ID_NAME, StringArgumentType.string()).suggests(CommandRegistry.ALL_MODELS);
        RequiredArgumentBuilder<CommandSourceStack, String> extra = Commands.argument(EXTRA_NAME, StringArgumentType.greedyString());
        export.then(modelId.executes(ExportCommand::exportModel));
        export.then(modelId.then(extra.executes(ExportCommand::exportModelWithExtra)));
        return export;
    }

    private static int exportModel(final CommandContext<CommandSourceStack> context) {
        String modelId = StringArgumentType.getString(context, MODEL_ID_NAME);
        exportModel(context.getSource(), modelId, null);
        return Command.SINGLE_SUCCESS;
    }

    private static int exportModelWithExtra(final CommandContext<CommandSourceStack> context) {
        String modelId = StringArgumentType.getString(context, MODEL_ID_NAME);
        String extra = StringArgumentType.getString(context, EXTRA_NAME);
        exportModel(context.getSource(), modelId, extra);
        return Command.SINGLE_SUCCESS;
    }

    private static void exportModel(CommandSourceStack source, String modelId, @Nullable String extra) {
        ThreadTools.submit(() -> {
            ExportModelResult result = ServerModelManager.exportModel(modelId, extra);
            if (result.message() != null) {
                CommandUtil.sendAsyncFeedback(source, CommandUtil.wrapMessage(result.message()), false);
            }
            if (result.success()) {
                CommandUtil.sendAsyncFeedback(source, Component.translatable("commands.yes_steve_model.export.success", result.filePath()), false);
            }
        });
    }
}
