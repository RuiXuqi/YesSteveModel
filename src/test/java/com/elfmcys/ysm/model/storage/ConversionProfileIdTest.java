package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.Hash256;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConversionProfileIdTest {
    private static final Hash256 ANIMATION_PROFILE = new Hash256(new byte[Hash256.SIZE]);

    @Test
    void everySemanticInputChangesTheProfile() {
        var base = ConversionProfileInputs.production(ANIMATION_PROFILE);

        assertChanged(base, value -> new ConversionProfileInputs(
                value.containerMajor() + 1, value.containerMinor(), value.containerPatch(),
                value.containerQualifier(), value.schemaId(), value.schemaVersion(),
                value.canonicalizerVersion(), value.parserVersion(), value.imagePolicyVersion(),
                value.defaultAnimationProfile(), value.legacyImporterVersion()));
        assertChanged(base, value -> new ConversionProfileInputs(
                value.containerMajor(), value.containerMinor() + 1, value.containerPatch(),
                value.containerQualifier(), value.schemaId(), value.schemaVersion(),
                value.canonicalizerVersion(), value.parserVersion(), value.imagePolicyVersion(),
                value.defaultAnimationProfile(), value.legacyImporterVersion()));
        assertChanged(base, value -> new ConversionProfileInputs(
                value.containerMajor(), value.containerMinor(), value.containerPatch() + 1,
                value.containerQualifier(), value.schemaId(), value.schemaVersion(),
                value.canonicalizerVersion(), value.parserVersion(), value.imagePolicyVersion(),
                value.defaultAnimationProfile(), value.legacyImporterVersion()));
        assertChanged(base, value -> replace(value, "qualifier", null, null, null, null, null));
        assertChanged(base, value -> new ConversionProfileInputs(
                value.containerMajor(), value.containerMinor(), value.containerPatch(),
                value.containerQualifier(), "another-schema", value.schemaVersion(),
                value.canonicalizerVersion(), value.parserVersion(), value.imagePolicyVersion(),
                value.defaultAnimationProfile(), value.legacyImporterVersion()));
        assertChanged(base, value -> replace(value, null, "schema-version", null, null, null, null));
        assertChanged(base, value -> replace(value, null, null, "canonicalizer", null, null, null));
        assertChanged(base, value -> replace(value, null, null, null, "parser", null, null));
        assertChanged(base, value -> replace(value, null, null, null, null, "image", null));
        assertChanged(base, value -> {
            var changedProfile = new byte[Hash256.SIZE];
            changedProfile[0] = 1;
            return new ConversionProfileInputs(
                    value.containerMajor(), value.containerMinor(), value.containerPatch(),
                    value.containerQualifier(), value.schemaId(), value.schemaVersion(),
                    value.canonicalizerVersion(), value.parserVersion(), value.imagePolicyVersion(),
                    new Hash256(changedProfile), value.legacyImporterVersion());
        });
        assertChanged(base, value -> replace(value, null, null, null, null, null, "legacy"));
    }

    private static void assertChanged(ConversionProfileInputs base,
                                      UnaryOperator<ConversionProfileInputs> change) {
        assertNotEquals(ConversionProfileId.from(base), ConversionProfileId.from(change.apply(base)));
    }

    private static ConversionProfileInputs replace(ConversionProfileInputs value,
                                                    String qualifier,
                                                    String schemaVersion,
                                                    String canonicalizer,
                                                    String parser,
                                                    String image,
                                                    String legacy) {
        return new ConversionProfileInputs(
                value.containerMajor(), value.containerMinor(), value.containerPatch(),
                qualifier == null ? value.containerQualifier() : qualifier,
                value.schemaId(), schemaVersion == null ? value.schemaVersion() : schemaVersion,
                canonicalizer == null ? value.canonicalizerVersion() : canonicalizer,
                parser == null ? value.parserVersion() : parser,
                image == null ? value.imagePolicyVersion() : image,
                value.defaultAnimationProfile(),
                legacy == null ? value.legacyImporterVersion() : legacy);
    }
}
