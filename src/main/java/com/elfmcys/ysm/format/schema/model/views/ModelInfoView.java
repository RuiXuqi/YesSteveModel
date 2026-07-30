package com.elfmcys.ysm.format.schema.model.views;

import com.elfmcys.ysm.format.schema.file.AssetFileView;
import com.elfmcys.ysm.format.schema.file.ChunkDataSource;
import com.elfmcys.ysm.natives.image.Image;
import mixel.manifest.info.ExportInfoOuterClass;
import mixel.manifest.info.InfoOuterClass;
import mixel.manifest.info.MetadataOuterClass;
import mixel.manifest.info.PropertiesOuterClass;
import mixel.manifest.info.SettingsOuterClass;
import com.elfmcys.ysm.task.TaskContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ModelInfoView {
    private final AssetFileView view;
    private final InfoOuterClass.Info info;
    private final HashMap<String, HashMap<String, String>> langs;
    private final HashMap<String, MetadataOuterClass.Author> authors;

    public ModelInfoView(InfoOuterClass.Info info, AssetFileView view) {
        this.view = view;
        this.info = info;

        langs = new HashMap<>(info.hasLanguageFiles() ? info.getLanguageFiles().length() : 0);
        if (info.hasLanguageFiles()) {
            for (var langFile : info.getLanguageFiles()) {
                var lang = new HashMap<String, String>(
                        langFile.hasEntries() ? langFile.getEntries().length() : 0);
                if (langFile.hasEntries()) {
                    for (var item : langFile.getEntries()) {
                        lang.put(item.getKey(), item.getValue());
                    }
                }
                langs.put(langFile.getLocale(), lang);
            }
        }

        var hasAuthors = info.hasMetadata() && info.getMetadata().hasAuthors();
        authors = new HashMap<>(hasAuthors ? info.getMetadata().getAuthors().length() : 0);
        if (hasAuthors) {
            for (var author : info.getMetadata().getAuthors()) {
                authors.put(author.getName(), author);
            }
        }
    }

    @NotNull
    public String translateOr(String key, String locale, @NotNull String defaultValue) {
        return Objects.requireNonNullElse(translate(key, locale), defaultValue);
    }

    @Nullable
    public String translate(String key, String locale) {
        var value = translateInner(key, locale);
        if (value != null) {
            return value;
        }
        return translateInner(key, "en_us");
    }

    @Nullable
    private String translateInner(String key, String locale) {
        var lang = langs.get(locale);
        if (lang != null) {
            return lang.get(key);
        }
        return null;
    }

    public MetadataOuterClass.Metadata getMetadata() {
        return info.getMetadata();
    }

    public ExportInfoOuterClass.ExportInfo getExportInfo() {
        return info.getExport();
    }

    public PropertiesOuterClass.Properties getProperties() {
        return info.getProperties();
    }

    public SettingsOuterClass.Settings getSettings() {
        return info.getSettings();
    }

    public CompletableFuture<@Nullable Image> readAvatar(TaskContext ctx, ChunkDataSource source, String authorName) {
        var author = authors.get(authorName);
        if (author == null || !author.hasAvatar()) {
            return CompletableFuture.completedFuture(null);
        }
        return view.readImageBlob(ctx, source, author.getAvatar());
    }
}
