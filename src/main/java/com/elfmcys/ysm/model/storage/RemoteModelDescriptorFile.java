package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.format.schema.model.ModelFileView;
import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.ModelHash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and writes process-shared remote descriptor metadata files. */
final class RemoteModelDescriptorFile {
    private static final int MAGIC = 0x59534D52;
    private static final int VERSION = 2;
    private static final int MAX_PART_SIZE = 32 * 1024 * 1024;

    private RemoteModelDescriptorFile() {
    }

    static ModelDescriptor create(ModelHash modelHash, ModelHash descriptorHash,
                                  byte[] containerPreamble, byte[] manifest) throws IOException {
        if (!ModelHashing.descriptorHash(containerPreamble, manifest).equals(descriptorHash)) {
            throw new IOException("Remote model descriptor hash mismatch");
        }
        var view = openView(containerPreamble, manifest);
        if (!view.getModelHash().equals(modelHash)) {
            throw new IOException("Remote model manifest hash mismatch");
        }
        return new ModelDescriptor(modelHash, descriptorHash, containerPreamble, manifest, view);
    }

    static boolean validate(Path file, ModelDescriptor expected) {
        try {
            return read(file).sameRepresentation(expected);
        } catch (Exception ignored) {
            return false;
        }
    }

    static void write(Path file, ModelDescriptor descriptor) throws IOException {
        try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.write(descriptor.modelHash().bytes());
            output.write(descriptor.descriptorHash().bytes());
            writeBytes(output, descriptor.containerPreamble());
            writeBytes(output, descriptor.schemaManifest());
        }
    }

    static ModelDescriptor read(Path file) throws IOException {
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported remote model metadata");
            }
            var modelHash = new ModelHash(readExact(input, ModelHash.SIZE, "model hash"));
            var descriptorHash = new ModelHash(readExact(input, ModelHash.SIZE, "descriptor hash"));
            var containerPreamble = readBytes(input);
            var manifest = readBytes(input);
            if (input.read() != -1) {
                throw new IOException("Trailing remote model metadata data");
            }
            return create(modelHash, descriptorHash, containerPreamble, manifest);
        }
    }

    private static ModelFileView openView(byte[] containerPreamble, byte[] manifest) throws IOException {
        try {
            return ModelFileView.readMetadata(containerPreamble, manifest);
        } catch (UncheckedIOException error) {
            throw error.getCause();
        }
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        var size = input.readInt();
        if (size < 0 || size > MAX_PART_SIZE) {
            throw new IOException("Invalid remote metadata part size");
        }
        return readExact(input, size, "metadata payload");
    }

    private static byte[] readExact(DataInputStream input, int size, String part) throws IOException {
        var value = input.readNBytes(size);
        if (value.length != size) {
            throw new IOException("Truncated remote model " + part);
        }
        return value;
    }
}
