package com.elfmcys.ysm.tool;

import com.elfmcys.ysm.model.catalog.BuiltinModelIndex;
import com.elfmcys.ysm.model.catalog.DefaultAnimationKey;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.model.domain.ModelPath;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinModelIndexToolTest {
    private static final ModelPath DEFAULT_PATH = new ModelPath("default");

    @Test
    void defaultContractContainsOnlyTheDefaultModelAndCurrentAnimationHashes()
            throws Exception {
        var key = new DefaultAnimationKey("player/main", "idle");
        var current = hash(2);

        var contract = BuiltinModelIndexTool.createDefaultContract(
                hash(1), Map.of(key, current));

        assertEquals(List.of(DEFAULT_PATH), contract.entries().stream()
                .map(BuiltinModelIndex.Entry::path).toList());
        assertEquals(List.of(current), contract.animationEntries().get(0)
                .acceptedPayloadHashes());
    }

    @Test
    void indexGenerationMergesHistoryWithoutWeakeningTheDefaultProof() throws Exception {
        var key = new DefaultAnimationKey("player/main", "idle");
        var defaultHash = hash(1);
        var current = hash(2);
        var historical = hash(3);
        var contract = BuiltinModelIndexTool.createDefaultContract(
                defaultHash, Map.of(key, current));

        var index = BuiltinModelIndexTool.createIndex(Map.of(
                        DEFAULT_PATH, defaultHash,
                        new ModelPath("misc/example"), hash(4)),
                contract, Map.of(key, Set.of(historical)));

        assertTrue(index.accepts(key, current));
        assertTrue(index.accepts(key, historical));
        assertDoesNotThrow(() -> BuiltinModelIndexTool.validateDefaultContract(index, contract));
        assertThrows(IOException.class, () -> BuiltinModelIndexTool.createIndex(
                Map.of(DEFAULT_PATH, hash(5)), contract, Map.of()));
    }

    @Test
    void rejectsAContractWithExtraModelsOrHistoricalAnimationHashes() throws Exception {
        var key = new DefaultAnimationKey("player/main", "idle");
        var defaultHash = hash(1);
        var current = hash(2);
        var historical = hash(3);
        var modelHashes = Map.of(DEFAULT_PATH, defaultHash);
        var extraModelContract = BuiltinModelIndex.of(Map.of(
                DEFAULT_PATH, defaultHash,
                new ModelPath("misc/example"), hash(4)));
        var historicalContract = BuiltinModelIndex.of(
                modelHashes, Map.of(key, current), Map.of(key, Set.of(historical)));

        assertThrows(IOException.class, () -> BuiltinModelIndexTool.createIndex(
                modelHashes, extraModelContract, Map.of()));
        assertThrows(IOException.class, () -> BuiltinModelIndexTool.createIndex(
                modelHashes, historicalContract, Map.of()));
    }

    @Test
    void writesDeterministicVerificationReceipt() {
        assertEquals("""
                {
                  "formatVersion": 1,
                  "builtinIndexHash": "%s",
                  "verifiedModelCount": 27
                }
                """.formatted(hash(9)), BuiltinModelIndexTool.verificationReceipt(hash(9), 27));
        assertThrows(IllegalArgumentException.class,
                () -> BuiltinModelIndexTool.verificationReceipt(hash(9), 0));
    }

    private static Hash256 hash(int marker) {
        var bytes = new byte[Hash256.SIZE];
        bytes[bytes.length - 1] = (byte) marker;
        return new Hash256(bytes);
    }
}
