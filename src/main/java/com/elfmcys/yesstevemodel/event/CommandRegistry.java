package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.command.ClientRootCommand;
import com.elfmcys.yesstevemodel.client.model.CustomPlayerModel;
import com.elfmcys.yesstevemodel.command.RootCommand;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.ModelIdUtil;
import com.google.common.collect.Sets;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class CommandRegistry {
    public static final SuggestionProvider<CommandSourceStack> ALL_MODELS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "models"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider) {
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                return SharedSuggestionProvider.suggest(ServerModelManager.getModels().keySet(), builder);
            } else {
                return SharedSuggestionProvider.suggest(ClientModelManager.getModelInfo().keySet().stream().map(ResourceLocation::getPath), builder);
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
                AnimationFile main = GeckoLibCache.getInstance().getAnimations().get(CustomPlayerModel.DEFAULT_MAIN_ANIMATION);
                Set<String> animations = Sets.newHashSet();
                animations.addAll(main.getAnimations().keySet());
                animations.add("stop");
                return SharedSuggestionProvider.suggest(animations, builder);
            }
        } else {
            return Suggestions.empty();
        }
    });

    public static final SuggestionProvider<CommandSourceStack> ALL_TEXTURES = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "textures"), (source, builder) -> {
        if (source.getSource() instanceof SharedSuggestionProvider) {
            String modelName = source.getArgument("model_id", String.class);
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                if (ServerModelManager.getModels().containsKey(modelName)) {
                    Set<String> textures = ServerModelManager.getModels().get(modelName).textures();
                    return SharedSuggestionProvider.suggest(textures, builder);
                }
            } else {
                ResourceLocation modelId = new ResourceLocation(YesSteveModel.MOD_ID, modelName);
                if (ClientModelManager.getModelInfo().containsKey(modelId)) {
                    List<ResourceLocation> textures = ClientModelManager.getModelInfo().get(modelId).textureIds();
                    Stream<String> stream = textures.stream().map(ModelIdUtil::getSubNameFromId).filter(StringUtils::isNoneBlank);
                    return SharedSuggestionProvider.suggest(stream, builder);
                }
            }
        }
        return Suggestions.empty();
    });

    public static void onServerStaring(RegisterCommandsEvent event) {
        RootCommand.register(event.getDispatcher());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientRootCommand.register(event.getDispatcher());
        }
    }
}
