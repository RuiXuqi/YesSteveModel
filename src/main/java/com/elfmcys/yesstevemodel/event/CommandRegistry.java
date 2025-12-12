package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.command.ClientRootCommand;
import com.elfmcys.yesstevemodel.command.RootCommand;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
@SuppressWarnings("removal")
public final class CommandRegistry {
    public static final SuggestionProvider<CommandSourceStack> ALL_MODELS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "models"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider) {
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                return SharedSuggestionProvider.suggest(ServerModelManager.getModels().keySet().stream().map(CommandRegistry::filterSuggestionStr).toList(), builder);
            } else {
                return SharedSuggestionProvider.suggest(ClientModelManager.getModels().keySet().stream().map(CommandRegistry::filterSuggestionStr).toList(), builder);
            }
        } else {
            return Suggestions.empty();
        }
    });

    public static final SuggestionProvider<CommandSourceStack> ALL_ANIMATIONS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "animations"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider) {
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                // Fixme: 应该为服务器后台也添加提示功能
                return Suggestions.empty();
            } else {
                var main = ClientModelManager.getDefaultModel().playerModel().animations();
                Set<String> animations = Sets.newHashSet();
                animations.addAll(main.keySet().stream().map(CommandRegistry::filterSuggestionStr).toList());
                animations.add("stop");
                return SharedSuggestionProvider.suggest(animations, builder);
            }
        } else {
            return Suggestions.empty();
        }
    });

    public static final SuggestionProvider<CommandSourceStack> ALL_TEXTURES = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "textures"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider) {
            String modelId = source.getArgument("model_id", String.class);
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                if (ServerModelManager.getModels().containsKey(modelId)) {
                    List<String> textures = ServerModelManager.getModels().get(modelId).playerModel().textures().stream()
                            .map(CommandRegistry::filterSuggestionStr)
                            .collect(Collectors.toList());
                    textures.add(0, "-");
                    return SharedSuggestionProvider.suggest(textures, builder);
                }
            } else {
                if (ClientModelManager.getModels().containsKey(modelId)) {
                    List<String> textures = ClientModelManager.getModel(modelId)
                            .map(model -> model.playerModel().textures().keyList().stream()
                                .map(CommandRegistry::filterSuggestionStr)
                                .collect(Collectors.toList()))
                            .orElseGet(Lists::newArrayList);
                    textures.add(0, "-");
                    return SharedSuggestionProvider.suggest(textures, builder);
                }
            }
        }
        return Suggestions.empty();
    });

    @SubscribeEvent
    public static void onServerStaring(RegisterCommandsEvent event) {
        if (!YesSteveModel.isAvailable()) {
            RootCommand.registerPlaceholder(event.getDispatcher());
            return;
        }

        RootCommand.register(event.getDispatcher());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientRootCommand.register(event.getDispatcher());
        }
    }

    private static String filterSuggestionStr(String str) {
        if (str.chars().allMatch(c -> StringReader.isAllowedInUnquotedString((char) c))) {
            return str;
        }
        return String.format("\"%s\"", str.replace("\"", "\\\"").replace("'", "\\'"));
    }
}
