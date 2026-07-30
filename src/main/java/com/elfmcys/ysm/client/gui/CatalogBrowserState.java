package com.elfmcys.ysm.client.gui;

import com.elfmcys.ysm.client.lang.LanguageManager;
import com.elfmcys.ysm.client.model.ModelPackInfo;
import com.elfmcys.ysm.client.model.catalog.CatalogModelMetadata;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogEntry;
import com.elfmcys.ysm.client.model.catalog.ClientCatalogSnapshot;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.domain.ModelPath;
import com.elfmcys.ysm.model.source.PackOffer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class CatalogBrowserState {
    private static final String AUTHOR_SEARCH_PREFIX = "@";
    private static final String PACK_SEARCH_PREFIX = "#";
    private static final Object2IntMap<String> PAGE_BY_PACK = new Object2IntOpenHashMap<>();
    private static String currentPack = "";

    private final Map<String, ModelPackInfo> allPacks = new LinkedHashMap<>();
    private ClientCatalogSnapshot catalog;
    private List<ClientCatalogEntry> models = List.of();
    private List<ModelPackInfo> packs = List.of();
    private Category category = Category.ALL;
    private int maxPage;

    void rebuild(ClientCatalogSnapshot next, Function<PackOffer, ModelPackInfo> packFactory) {
        catalog = next;
        allPacks.clear();
        next.packs().forEach(pack -> allPacks.putIfAbsent(pack.subject().hierarchy(), packFactory.apply(pack)));
        next.models().values().forEach(entry ->
                addSyntheticPacks(new ModelPath(entry.displayPath()).parentHierarchy()));
        if (!currentPack.isBlank() && !allPacks.containsKey(currentPack)) {
            currentPack = "";
        }
    }

    void filter(String query, String locale, Set<String> hiddenPaths,
                Predicate<ModelHash> authorized, Predicate<ModelHash> starred) {
        var allModels = new ArrayList<>(catalog.models().values());
        var search = query.strip().toLowerCase(Locale.ENGLISH);
        allModels.removeIf(entry -> !isVisible(entry, search, locale, hiddenPaths, authorized, starred));
        allModels.sort(Comparator.comparing(ClientCatalogEntry::displayPath));
        models = List.copyOf(allModels);

        if (category != Category.ALL || (!search.isBlank() && !search.startsWith(PACK_SEARCH_PREFIX))) {
            packs = List.of();
        } else {
            var packSearch = search.startsWith(PACK_SEARCH_PREFIX) ? search.substring(1) : search;
            packs = allPacks.values().stream()
                    .filter(pack -> search.isBlank() ? directChild(currentPack, pack.hierarchy())
                            : matches(pack, packSearch))
                    .sorted(Comparator.comparing(ModelPackInfo::hierarchy))
                    .toList();
        }
        maxPage = Math.max(0, (models.size() + packs.size() - 1) / 10);
        if (page() > maxPage) {
            resetPage();
        }
    }

    private boolean isVisible(ClientCatalogEntry entry, String search, String locale,
                              Set<String> hiddenPaths, Predicate<ModelHash> authorized,
                              Predicate<ModelHash> starred) {
        var metadata = CatalogModelMetadata.from(entry);
        if (hiddenPaths.contains(metadata.path())) {
            return false;
        }
        if (category == Category.AUTH && entry.authorizationRequired() && !authorized.test(entry.modelHash())) {
            return false;
        }
        if (category == Category.STAR && !starred.test(entry.modelHash())) {
            return false;
        }
        if (search.isBlank()) {
            return category != Category.ALL
                    || new ModelPath(entry.displayPath()).parentHierarchy().equals(currentPack);
        }
        return matches(metadata, search, locale);
    }

    private static boolean matches(CatalogModelMetadata metadata, String search, String locale) {
        if (search.startsWith(PACK_SEARCH_PREFIX)) {
            return false;
        }
        var modelMetadata = metadata.info().metadata();
        if (search.startsWith(AUTHOR_SEARCH_PREFIX)) {
            if (modelMetadata == null) {
                return false;
            }
            var authorSearch = search.substring(1);
            for (var index = 0; index < modelMetadata.authors().size(); index++) {
                var author = modelMetadata.authors().get(index);
                if (metadata.localized(locale, "metadata.authors.%d.name".formatted(index), author.name())
                        .toLowerCase(Locale.ENGLISH).contains(authorSearch)) {
                    return true;
                }
            }
            return false;
        }
        if (metadata.path().toLowerCase(Locale.ENGLISH).contains(search)) {
            return true;
        }
        if (modelMetadata == null) {
            return false;
        }
        if (metadata.localized(locale, "metadata.name", modelMetadata.name())
                .toLowerCase(Locale.ENGLISH).contains(search)
                || metadata.localized(locale, "metadata.tips", modelMetadata.tips())
                .toLowerCase(Locale.ENGLISH).contains(search)) {
            return true;
        }
        for (var index = 0; index < modelMetadata.authors().size(); index++) {
            var author = modelMetadata.authors().get(index);
            if (metadata.localized(locale, "metadata.authors.%d.name".formatted(index), author.name())
                    .toLowerCase(Locale.ENGLISH).contains(search)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ModelPackInfo pack, String search) {
        return pack.hierarchy().toLowerCase(Locale.ENGLISH).contains(search)
                || LanguageManager.getI18n(pack, "name", pack.name()).toLowerCase(Locale.ENGLISH).contains(search)
                || LanguageManager.getI18n(pack, "description", pack.desc())
                .toLowerCase(Locale.ENGLISH).contains(search);
    }

    ClientCatalogSnapshot catalog() {
        return catalog;
    }

    List<ClientCatalogEntry> models() {
        return models;
    }

    List<ModelPackInfo> packs() {
        return packs;
    }

    Iterable<ModelPackInfo> allPacks() {
        return allPacks.values();
    }

    ModelPackInfo pack(String hierarchy) {
        return allPacks.get(hierarchy);
    }

    String currentPack() {
        return currentPack;
    }

    void enterPack(String hierarchy) {
        currentPack = hierarchy;
        resetPage();
    }

    void backToParent() {
        var trimmed = currentPack.endsWith("/")
                ? currentPack.substring(0, currentPack.length() - 1) : currentPack;
        var slash = trimmed.lastIndexOf('/');
        var previous = currentPack;
        currentPack = slash < 0 ? "" : trimmed.substring(0, slash + 1);
        PAGE_BY_PACK.removeInt(previous);
    }

    Category category() {
        return category;
    }

    void category(Category category) {
        this.category = category;
        resetPage();
    }

    int page() {
        return PAGE_BY_PACK.getOrDefault(currentPack, 0);
    }

    void page(int page) {
        PAGE_BY_PACK.put(currentPack, page);
    }

    void resetPage() {
        page(0);
    }

    int maxPage() {
        return maxPage;
    }

    private void addSyntheticPacks(String hierarchy) {
        var current = new StringBuilder();
        for (var part : hierarchy.split("/")) {
            if (part.isBlank()) {
                continue;
            }
            current.append(part).append('/');
            var value = current.toString();
            allPacks.putIfAbsent(value, new ModelPackInfo(value, part, "", null, Map.of()));
        }
    }

    private static boolean directChild(String parent, String candidate) {
        if (parent.equals(candidate) || !candidate.startsWith(parent)) {
            return false;
        }
        var remainder = candidate.substring(parent.length());
        return remainder.indexOf('/') == remainder.length() - 1;
    }

    enum Category {
        ALL,
        AUTH,
        STAR
    }
}
