package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.parser.pojo.manifest.ModelManifest;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ConfigForms;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ExtraAnimationButton;
import mixel.manifest.ManifestOuterClass;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelInfoBuilderTest {
    @Test
    void leavesNullAndEmptyConfigFormLabelsAbsent() throws Exception {
        var nullLabels = new ConfigForms();
        nullLabels.labels = null;
        assertFalse(writeForm(nullLabels).hasLabels());

        var emptyLabels = new ConfigForms();
        emptyLabels.labels = new LinkedHashMap<>();
        assertFalse(writeForm(emptyLabels).hasLabels());
    }

    @Test
    void writesAllConfigFormLabels() throws Exception {
        var source = new ConfigForms();
        source.labels.put("low", "Low");
        source.labels.put("high", "High");

        var result = writeForm(source);

        assertTrue(result.hasLabels());
        assertEquals(2, result.getLabels().length());
        assertEquals("low", result.getLabels().get(0).getKey());
        assertEquals("Low", result.getLabels().get(0).getValue());
        assertEquals("high", result.getLabels().get(1).getKey());
        assertEquals("High", result.getLabels().get(1).getValue());
    }

    private static mixel.manifest.info.SettingsOuterClass.ConfigForms
    writeForm(ConfigForms form) throws Exception {
        var source = new ModelManifest();
        var button = new ExtraAnimationButton();
        button.configForms.add(form);
        source.properties.extraAnimationButtons.add(button);
        var manifest = ManifestOuterClass.Manifest.newInstance();

        ModelInfoBuilder.writeInfo(manifest, source, "test", (path, hashType, policy, consumer) -> {
        });

        return manifest.getInfo().getSettings().getExtraAnimationButtons().get(0).getConfigForms().get(0);
    }
}
