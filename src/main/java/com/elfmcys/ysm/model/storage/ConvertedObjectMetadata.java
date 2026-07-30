package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.ModelHash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

record ConvertedObjectMetadata(ConvertedObjectKey key, ModelHash descriptorHash,
                               long containerSize) {
    private static final int MAGIC = 0x59534D4F;
    private static final int VERSION = 1;

    ConvertedObjectMetadata {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(descriptorHash, "descriptorHash");
        if (containerSize < 0) {
            throw new IllegalArgumentException("containerSize must not be negative");
        }
    }

    static ConvertedObjectMetadata read(Path file) throws IOException {
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported converted object metadata");
            }
            var profile = new byte[ModelHash.SIZE];
            var modelHash = new byte[ModelHash.SIZE];
            var descriptorHash = new byte[ModelHash.SIZE];
            input.readFully(profile);
            input.readFully(modelHash);
            input.readFully(descriptorHash);
            var size = input.readLong();
            if (size < 0 || input.read() != -1) {
                throw new IOException("Invalid converted object metadata size or trailing data");
            }
            return new ConvertedObjectMetadata(
                    new ConvertedObjectKey(new ConversionProfileId(new ModelHash(profile)),
                            new ModelHash(modelHash)),
                    new ModelHash(descriptorHash), size);
        }
    }

    static void write(Path file, ConvertedObjectMetadata metadata) throws IOException {
        try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.write(metadata.key.profile().hash().bytes());
            output.write(metadata.key.modelHash().bytes());
            output.write(metadata.descriptorHash.bytes());
            output.writeLong(metadata.containerSize);
        }
    }
}
