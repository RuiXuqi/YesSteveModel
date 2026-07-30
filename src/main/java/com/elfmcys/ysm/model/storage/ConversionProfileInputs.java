package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.format.container.AssetContainerConstant;
import com.elfmcys.ysm.format.parser.ModelParser;
import com.elfmcys.ysm.format.schema.model.ModelFileConstant;
import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.importer.LegacyImporter;

import java.util.Objects;

/** Explicit semantic inputs that determine raw-model conversion output bytes. */
public record ConversionProfileInputs(int containerMajor,
                                      int containerMinor,
                                      int containerPatch,
                                      String containerQualifier,
                                      String schemaId,
                                      String schemaVersion,
                                      String canonicalizerVersion,
                                      String parserVersion,
                                      String imagePolicyVersion,
                                      ModelHash defaultAnimationProfile,
                                      String legacyImporterVersion) {
    public ConversionProfileInputs {
        if (containerMajor < 0 || containerMinor < 0 || containerPatch < 0) {
            throw new IllegalArgumentException("Container version components must be non-negative");
        }
        Objects.requireNonNull(containerQualifier, "containerQualifier");
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(canonicalizerVersion, "canonicalizerVersion");
        Objects.requireNonNull(parserVersion, "parserVersion");
        Objects.requireNonNull(imagePolicyVersion, "imagePolicyVersion");
        Objects.requireNonNull(defaultAnimationProfile, "defaultAnimationProfile");
        Objects.requireNonNull(legacyImporterVersion, "legacyImporterVersion");
    }

    public static ConversionProfileInputs production(ModelHash defaultAnimationProfile) {
        return new ConversionProfileInputs(
                AssetContainerConstant.CURRENT_MAJOR_VER,
                AssetContainerConstant.CURRENT_MINOR_VER,
                AssetContainerConstant.CURRENT_PATCH_VER,
                AssetContainerConstant.CURRENT_QUALIFIER_VER,
                ModelFileConstant.SCHEMA_ID,
                ModelFileConstant.CURRENT_VERSION.toString(),
                ModelParser.CANONICALIZER_PROFILE_VERSION,
                ModelParser.PARSER_PROFILE_VERSION,
                ModelParser.IMAGE_POLICY_PROFILE_VERSION,
                defaultAnimationProfile,
                LegacyImporter.PROFILE_VERSION);
    }
}
