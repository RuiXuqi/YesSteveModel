package com.elfmcys.ysm.model.source;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelSourceAggregatorTest {
    @Test
    void sortsPacksByConfiguredPriorityThenStableIdentity() {
        var lower = new SourceId("external.lower");
        var higher = new SourceId("external.higher");
        var lowerSource = source(lower, 10, List.of(pack(lower, "z")));
        var higherSource = source(higher, 20, List.of(pack(higher, "a")));

        var merged = ModelSourceAggregator.aggregate(List.of(lowerSource, higherSource));

        assertEquals(List.of(higher, lower), merged.packs().stream().map(PackOffer::sourceId).toList());
        assertEquals(Map.of(lower, cursor(lower), higher, cursor(higher)), merged.cursors());
    }

    @Test
    void rejectsTwoCatalogsClaimingTheSameSource() {
        var source = new SourceId("duplicate");
        assertThrows(IllegalArgumentException.class, () -> ModelSourceAggregator.aggregate(List.of(
                source(source, 1, List.of()), source(source, 2, List.of()))));
    }

    @Test
    void rejectsACatalogWhoseCursorDoesNotMatchItsDescriptor() {
        var descriptorId = new SourceId("descriptor");
        var catalogId = new SourceId("catalog");
        var source = new TestSource(new ModelSourceDescriptor(
                descriptorId, SourceKind.EXTERNAL, 1),
                new ResolvedCatalog(cursor(catalogId), List.of(), List.of()));

        assertThrows(IllegalArgumentException.class,
                () -> ModelSourceAggregator.aggregate(List.of(source)));
    }

    private static CatalogCursor cursor(SourceId source) {
        return new CatalogCursor(source, new byte[16], 1);
    }

    private static PackOffer pack(SourceId source, String hierarchy) {
        return new PackOffer(source, new ModelAssetSubject.Pack("models", hierarchy),
                hierarchy, "", Map.of(), null, "", 0, AccessPolicy.PUBLIC);
    }

    private static ModelSource source(SourceId sourceId, int priority, List<PackOffer> packs) {
        return new TestSource(new ModelSourceDescriptor(sourceId, SourceKind.EXTERNAL, priority),
                new ResolvedCatalog(cursor(sourceId), List.of(), packs));
    }

    private record TestSource(ModelSourceDescriptor descriptor,
                              ResolvedCatalog catalog) implements ModelSource {
    }
}
