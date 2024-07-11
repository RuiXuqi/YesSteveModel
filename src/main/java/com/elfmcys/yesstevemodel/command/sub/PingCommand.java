package com.elfmcys.yesstevemodel.command.sub;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.ServerInfo;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

public class PingCommand {
    private static final String MOLANG_NAME = "ping";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> ping = Commands.literal(MOLANG_NAME).executes(PingCommand::ping);
        return ping;
    }

    private static int ping(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.translatable("message.yes_steve_model.client.ping_result", ModList.get().getModFileById(YesSteveModel.MOD_ID).versionString()));
        if (!NetworkHandler.isPlayerChannelPresent(player)) {
            NetworkHandler.sendToClientPlayer(new ServerInfo(), player);
        }
        return Command.SINGLE_SUCCESS;
    }
}
