package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.YesSteveModel;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;

public final class ModelCatalogSources {
    private ModelCatalogSources() {
    }

    public static List<ModelCatalogSource> sources() {
        var reloadable = reloadableSources();
        return List.of(builtin(), reloadable.get(0), reloadable.get(1));
    }

    public static ModelCatalogSource builtin() {
        var builtin = ModList.get().getModFileById(YesSteveModel.MOD_ID).getFile()
                .findResource("assets", YesSteveModel.MOD_ID, "builtin");
        return new ModelCatalogSource(CatalogRootKind.BUILTIN, builtin, false);
    }

    public static Path builtinIndex() {
        return ModList.get().getModFileById(YesSteveModel.MOD_ID).getFile()
                .findResource("assets", YesSteveModel.MOD_ID, "builtin-index.json");
    }

    public static List<ModelCatalogSource> reloadableSources() {
        var modelRoot = FMLPaths.GAMEDIR.get().resolve(YesSteveModel.MOD_ID);
        return List.of(
                new ModelCatalogSource(CatalogRootKind.CUSTOM, modelRoot.resolve("custom"), true),
                new ModelCatalogSource(CatalogRootKind.AUTH, modelRoot.resolve("auth"), true));
    }

    public static Path customPath() {
        return FMLPaths.GAMEDIR.get().resolve(YesSteveModel.MOD_ID).resolve("custom");
    }
}
