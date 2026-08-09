package com.elfmcys.ysm.model.storage;

import com.elfmcys.ysm.model.domain.Hash256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.UUID;

/** Immutable identity of the bytes that back one published model representation. */
public sealed interface ModelBackingIdentity permits ModelBackingIdentity.DirectContainer,
        ModelBackingIdentity.ConvertedObject, ModelBackingIdentity.RemoteSession,
        ModelBackingIdentity.ResidentDefault {

    Kind kind();

    enum Kind {
        DIRECT_CONTAINER,
        CONVERTED_OBJECT,
        REMOTE_SESSION,
        RESIDENT_DEFAULT
    }

    record DirectContainer(Path file, long size, long lastModifiedMillis, String fileKey)
            implements ModelBackingIdentity {
        public DirectContainer {
            file = normalize(file);
            fileKey = Objects.requireNonNullElse(fileKey, "");
        }

        public static DirectContainer capture(Path file) throws IOException {
            var normalized = normalize(file);
            var attributes = Files.readAttributes(normalized, BasicFileAttributes.class);
            return new DirectContainer(normalized, attributes.size(),
                    attributes.lastModifiedTime().toMillis(),
                    Objects.toString(attributes.fileKey(), ""));
        }

        @Override
        public Kind kind() {
            return Kind.DIRECT_CONTAINER;
        }
    }

    record ConvertedObject(Path object, Hash256 modelHash) implements ModelBackingIdentity {
        public ConvertedObject {
            object = normalize(object);
            Objects.requireNonNull(modelHash, "modelHash");
        }

        @Override
        public Kind kind() {
            return Kind.CONVERTED_OBJECT;
        }
    }

    record RemoteSession(UUID epoch, Hash256 modelHash, Hash256 descriptorHash)
            implements ModelBackingIdentity {
        public RemoteSession {
            Objects.requireNonNull(epoch, "epoch");
            Objects.requireNonNull(modelHash, "modelHash");
            Objects.requireNonNull(descriptorHash, "descriptorHash");
        }

        @Override
        public Kind kind() {
            return Kind.REMOTE_SESSION;
        }
    }

    record ResidentDefault(Hash256 modelHash) implements ModelBackingIdentity {
        public ResidentDefault {
            Objects.requireNonNull(modelHash, "modelHash");
        }

        @Override
        public Kind kind() {
            return Kind.RESIDENT_DEFAULT;
        }
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
