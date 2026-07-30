package com.elfmcys.ysm.model.storage;

import java.nio.file.Path;

public final class SharedCachePaths {
    private final Path root;

    public SharedCachePaths(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public static SharedCachePaths userDefault() {
        var home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            throw new IllegalStateException("The user.home system property is unavailable");
        }
        return new SharedCachePaths(Path.of(home).resolve(".ysm").resolve("unstable"));
    }

    public Path root() {
        return root;
    }

    public Path convertedObjects() {
        return root.resolve("converted/objects");
    }

    public Path convertedRoot() {
        return root.resolve("converted");
    }

    public Path convertedObjects(ConversionProfileId profile) {
        return convertedObjects().resolve(profile.pathComponent());
    }

    public Path convertedIndex() {
        return root.resolve("converted/index");
    }

    public Path convertedIndex(ConversionProfileId profile) {
        return convertedIndex().resolve(profile.pathComponent());
    }

    public Path convertedTemporary() {
        return root.resolve("converted/tmp");
    }

    public Path convertedQuarantine() {
        return root.resolve("converted/quarantine");
    }

    public Path convertedLifecycleLock() {
        return locks().resolve("converted-lifecycle.lck");
    }

    public Path remoteModels() {
        return root.resolve("remote/v0/models");
    }

    public Path remoteChunks() {
        return root.resolve("remote/v0/chunks");
    }

    public Path baked() {
        return root.resolve("baked");
    }

    public Path locks() {
        return root.resolve("locks");
    }

    public Path temporary() {
        return root.resolve("tmp");
    }
}
