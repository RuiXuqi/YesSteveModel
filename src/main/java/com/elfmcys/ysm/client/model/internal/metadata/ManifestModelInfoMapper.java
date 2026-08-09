package com.elfmcys.ysm.client.model.internal.metadata;

import com.elfmcys.ysm.info.ModelAuthor;
import com.elfmcys.ysm.info.ModelFormatVersion;
import com.elfmcys.ysm.info.ModelInfo;
import com.elfmcys.ysm.info.ModelLicense;
import com.elfmcys.ysm.info.ModelMetadata;
import com.elfmcys.ysm.info.ModelProperties;
import com.elfmcys.ysm.info.roulette.ExtraAnimationButton;
import com.elfmcys.ysm.info.roulette.ExtraAnimationClassify;
import com.elfmcys.ysm.info.roulette.forms.CheckboxForms;
import com.elfmcys.ysm.info.roulette.forms.ConfigForms;
import com.elfmcys.ysm.info.roulette.forms.RadioForms;
import com.elfmcys.ysm.info.roulette.forms.RangeForms;
import com.elfmcys.ysm.info.stats.PlayerMainModelStats;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.RenderTargetIds;
import com.elfmcys.ysm.network.message.model.ModelAssetPlan;
import mixel.common.StringPairOuterClass;
import mixel.manifest.ManifestOuterClass;
import mixel.manifest.info.InfoOuterClass;
import mixel.manifest.info.SettingsOuterClass;
import com.elfmcys.ysm.util.FifoHashMap;

import java.util.ArrayList;

public final class ManifestModelInfoMapper {
    private ManifestModelInfoMapper() {
    }

    public static ModelInfo map(Hash256 hash, ManifestOuterClass.Manifest manifest) {
        var info = manifest.getInfo();
        var player = ModelAssetPlan.target(manifest, RenderTargetIds.PLAYER);
        var settings = info.getSettings();
        var modelSettings = player.getSettings();
        var properties = info.getProperties();
        var stats = player.getStats();

        var runtimeProperties = new ModelProperties(
                modelSettings.getWidthScale(), modelSettings.getHeightScale(),
                settings.getDefaultTexture(), settings.getPreviewAnimation(),
                settings.hasExtraAnimation() ? fifo(settings.getExtraAnimation()) : fifo(),
                buttons(settings), classifications(settings),
                properties.getFree(), modelSettings.getRenderLayersFirst(),
                settings.getDisablePreviewRotation());
        return new ModelInfo(metadata(info), runtimeProperties,
                new PlayerMainModelStats(stats.getBones(), stats.getCubes(), stats.getFaces()),
                ModelFormatVersion.PLAIN, hash.toString(), "", 0, "");
    }

    private static ModelMetadata metadata(InfoOuterClass.Info info) {
        if (!info.hasMetadata()) {
            return null;
        }
        var source = info.getMetadata();
        var license = source.hasLicense()
                ? new ModelLicense(source.getLicense().getType(), source.getLicense().getDesc())
                : new ModelLicense("", "");
        var authors = new ModelAuthor[source.hasAuthors() ? source.getAuthors().length() : 0];
        for (var index = 0; index < authors.length; index++) {
            var author = source.getAuthors().get(index);
            authors[index] = new ModelAuthor(author.getName(), author.getRole(),
                    author.hasContacts() ? fifo(author.getContacts()) : fifo(), author.getComment());
        }
        return new ModelMetadata(source.getName(), source.getTips(), license, authors,
                source.hasLinks() ? fifo(source.getLinks()) : fifo());
    }

    private static ExtraAnimationButton[] buttons(
            SettingsOuterClass.Settings settings) {
        var result = new ExtraAnimationButton[
                settings.hasExtraAnimationButtons() ? settings.getExtraAnimationButtons().length() : 0];
        for (var index = 0; index < result.length; index++) {
            var source = settings.getExtraAnimationButtons().get(index);
            var forms = new ConfigForms[source.hasConfigForms() ? source.getConfigForms().length() : 0];
            for (var formIndex = 0; formIndex < forms.length; formIndex++) {
                forms[formIndex] = form(source.getConfigForms().get(formIndex));
            }
            // Audio distribution is intentionally absent from the new model protocol.
            result[index] = new ExtraAnimationButton(source.getId(), source.getName(), "", forms);
        }
        return result;
    }

    private static ConfigForms form(SettingsOuterClass.ConfigForms source) {
        return switch (source.getType()) {
            case CheckboxForms.TYPE -> new CheckboxForms(source.getTitle(), source.getDescription(), source.getValue());
            case RadioForms.TYPE -> new RadioForms(source.getTitle(), source.getDescription(), source.getValue(),
                    source.hasLabels() ? fifo(source.getLabels()) : fifo());
            case RangeForms.TYPE -> new RangeForms(source.getTitle(), source.getDescription(), source.getValue(),
                    source.getStep(), source.getMin(), source.getMax());
            default -> throw new IllegalArgumentException("Unknown extra-animation form type: " + source.getType());
        };
    }

    private static ExtraAnimationClassify[] classifications(
            SettingsOuterClass.Settings settings) {
        var result = new ExtraAnimationClassify[
                settings.hasExtraAnimationClassify() ? settings.getExtraAnimationClassify().length() : 0];
        for (var index = 0; index < result.length; index++) {
            var source = settings.getExtraAnimationClassify().get(index);
            result[index] = new ExtraAnimationClassify(source.getId(),
                    source.hasExtraAnimation() ? fifo(source.getExtraAnimation()) : fifo());
        }
        return result;
    }

    private static FifoHashMap<String, String> fifo(
            Iterable<StringPairOuterClass.StringPair> values) {
        var keys = new ArrayList<String>();
        var items = new ArrayList<String>();
        for (var entry : values) {
            keys.add(entry.getKey());
            items.add(entry.getValue());
        }
        return new FifoHashMap<>(keys.toArray(String[]::new), items.toArray(String[]::new));
    }

    private static FifoHashMap<String, String> fifo() {
        return new FifoHashMap<>(new String[0], new String[0]);
    }
}
