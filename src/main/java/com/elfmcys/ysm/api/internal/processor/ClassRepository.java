package com.elfmcys.ysm.api.internal.processor;

import com.elfmcys.ysm.api.internal.processor.asm.ClassReader;
import com.elfmcys.ysm.api.internal.processor.asm.tree.ClassNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;

final class ClassRepository {
    private final Filer filer;
    private final Map<String, byte[]> generatedBytes = new HashMap<>();
    private final Map<String, Optional<ClassNode>> nodes = new HashMap<>();
    private final Set<String> currentOutput = new LinkedHashSet<>();

    ClassRepository(Filer filer) {
        this.filer = filer;
    }

    void recordGenerated(String internalName, byte[] bytes) {
        generatedBytes.put(internalName, bytes);
        currentOutput.add(internalName);
        nodes.remove(internalName);
    }

    void markCurrentOutput(String internalName) {
        currentOutput.add(internalName);
    }

    boolean isCurrentOutput(String internalName) {
        return currentOutput.contains(internalName);
    }

    Set<String> currentOutputNames() {
        return Set.copyOf(currentOutput);
    }

    Map<String, byte[]> generatedBytesSnapshot() {
        Map<String, byte[]> snapshot = new HashMap<>();
        generatedBytes.forEach((name, bytes) -> snapshot.put(name, bytes.clone()));
        return Map.copyOf(snapshot);
    }

    Optional<ClassNode> find(String internalName) {
        return nodes.computeIfAbsent(internalName, this::loadNode);
    }

    private Optional<ClassNode> loadNode(String internalName) {
        byte[] bytes = generatedBytes.get(internalName);
        if (bytes == null) {
            bytes = read(StandardLocation.CLASS_OUTPUT, internalName);
        }
        if (bytes == null) {
            bytes = read(StandardLocation.CLASS_PATH, internalName);
        }
        if (bytes == null) {
            bytes = readFromProcessorLoader(internalName);
        }
        if (bytes == null) {
            return Optional.empty();
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return Optional.of(node);
    }

    private byte[] read(StandardLocation location, String internalName) {
        if (filer == null) {
            return null;
        }
        try {
            var resource = filer.getResource(location, "", internalName + ".class");
            try (InputStream input = resource.openInputStream()) {
                return input.readAllBytes();
            }
        } catch (IllegalArgumentException | IOException ignored) {
            return null;
        }
    }

    private byte[] readFromProcessorLoader(String internalName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassRepository.class.getClassLoader();
        }
        try (InputStream input = loader.getResourceAsStream(internalName + ".class")) {
            return input == null ? null : input.readAllBytes();
        } catch (IOException ignored) {
            return null;
        }
    }
}
