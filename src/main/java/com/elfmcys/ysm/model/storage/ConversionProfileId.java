package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.Hash256;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Stable identity of every semantic input that can change converted bytes. */
public record ConversionProfileId(Hash256 hash) implements Comparable<ConversionProfileId> {
    private static final int FORMAT_VERSION = 2;

    public ConversionProfileId {
        Objects.requireNonNull(hash, "hash");
    }

    public static ConversionProfileId from(ConversionProfileInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");
        try {
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) {
                output.writeInt(FORMAT_VERSION);
                writeField(output, "container-major", Integer.toString(inputs.containerMajor()));
                writeField(output, "container-minor", Integer.toString(inputs.containerMinor()));
                writeField(output, "container-patch", Integer.toString(inputs.containerPatch()));
                writeField(output, "container-qualifier", inputs.containerQualifier());
                writeField(output, "schema-id", inputs.schemaId());
                writeField(output, "schema-version", inputs.schemaVersion());
                writeField(output, "canonicalizer", inputs.canonicalizerVersion());
                writeField(output, "parser", inputs.parserVersion());
                writeField(output, "image-policy", inputs.imagePolicyVersion());
                writeField(output, "default-animation-profile",
                        inputs.defaultAnimationProfile().toString());
                writeField(output, "legacy-importer", inputs.legacyImporterVersion());
            }
            return new ConversionProfileId(ModelHashing.blake3(bytes.toByteArray()));
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static ConversionProfileId parse(String value) {
        return new ConversionProfileId(Hash256.parse(value));
    }

    public String pathComponent() {
        return hash.toString();
    }

    @Override
    public int compareTo(ConversionProfileId other) {
        return hash.compareTo(other.hash);
    }

    @Override
    public String toString() {
        return hash.toString();
    }

    private static void writeField(DataOutputStream output, String name, String value)
            throws IOException {
        writeString(output, name);
        writeString(output, value);
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        var bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }
}
