package com.elfmcys.ysm.model.catalog;

import com.elfmcys.ysm.model.domain.ModelDescriptor;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.storage.ModelFileHandle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record CatalogDiff(List<Operation> operations, boolean replacePacks) {
    public CatalogDiff {
        operations = List.copyOf(operations);
    }

    public static CatalogDiff between(ServerCatalogSnapshot previous, ServerCatalogSnapshot next) {
        var operations = new ArrayList<Operation>();

        previous.models().forEach((hash, oldHandle) -> {
            var nextHandle = next.models().get(hash);
            if (nextHandle == null) {
                operations.add(Operation.remove(hash, oldHandle.location()));
                return;
            }
            var oldDescriptor = oldHandle.descriptor();
            var newDescriptor = nextHandle.descriptor();
            if (!oldDescriptor.sameRepresentation(newDescriptor)) {
                operations.add(Operation.remove(hash, oldHandle.location()));
                operations.add(Operation.add(nextHandle));
            } else if (!oldHandle.location().equals(nextHandle.location())) {
                operations.add(Operation.move(nextHandle, oldHandle.location()));
            }
        });
        next.models().forEach((hash, handle) -> {
            if (!previous.models().containsKey(hash)) {
                operations.add(Operation.add(handle));
            }
        });
        operations.sort(Operation::compareTo);
        return new CatalogDiff(operations, !samePacks(previous, next));
    }

    private static boolean samePacks(ServerCatalogSnapshot left, ServerCatalogSnapshot right) {
        if (left.packs().size() != right.packs().size()) {
            return false;
        }
        for (var index = 0; index < left.packs().size(); index++) {
            var a = left.packs().get(index);
            var b = right.packs().get(index);
            if (a.rootKind() != b.rootKind() || !a.hierarchy().equals(b.hierarchy()) || !a.name().equals(b.name())
                    || !a.description().equals(b.description()) || !a.translations().equals(b.translations())
                    || !Arrays.equals(a.coverHash(), b.coverHash())
                    || !a.coverFormat().equals(b.coverFormat()) || a.coverSize() != b.coverSize()) {
                return false;
            }
        }
        return true;
    }

    public enum Type {
        ADD,
        REMOVE,
        MOVE
    }

    public record Operation(Type type, Hash256 hash, ModelDescriptor descriptor,
                            CatalogModelLocation location, CatalogModelLocation previousLocation)
            implements Comparable<Operation> {
        public static Operation add(ModelFileHandle handle) {
            return new Operation(Type.ADD, handle.descriptor().modelHash(), handle.descriptor(),
                    handle.location(), null);
        }

        public static Operation remove(Hash256 hash, CatalogModelLocation previousLocation) {
            return new Operation(Type.REMOVE, hash, null, null, previousLocation);
        }

        public static Operation move(ModelFileHandle handle,
                                     CatalogModelLocation previousLocation) {
            return new Operation(Type.MOVE, handle.descriptor().modelHash(), handle.descriptor(),
                    handle.location(), previousLocation);
        }

        @Override
        public int compareTo(Operation other) {
            var byHash = hash.compareTo(other.hash);
            return byHash != 0 ? byHash : Integer.compare(order(type), order(other.type));
        }

        private static int order(Type type) {
            return switch (type) {
                case REMOVE -> 0;
                case MOVE -> 1;
                case ADD -> 2;
            };
        }
    }
}
