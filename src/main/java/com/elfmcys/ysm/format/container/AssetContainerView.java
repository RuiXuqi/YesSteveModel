package com.elfmcys.ysm.format.container;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import org.jetbrains.annotations.Nullable;

/** Immutable container metadata. Use AssetContainerReader for parsing and ChunkDataSource for payload I/O. */
public final class AssetContainerView {
    private final int minorVer;
    private final int patchVer;
    private final String qualifierVer;
    private final String schema;
    private final int alignmentShift;
    private final int containerPreambleSize;
    private final Short2ReferenceMap<String> schemaProperties;
    private final Object2ReferenceMap<String, ChunkInfo> chunkTable;

    AssetContainerView(int minorVer, int patchVer, String qualifierVer, String schema,
                       int alignmentShift, int containerPreambleSize,
                       Short2ReferenceMap<String> schemaProperties,
                       Object2ReferenceMap<String, ChunkInfo> chunkTable) {
        this.minorVer = minorVer;
        this.patchVer = patchVer;
        this.qualifierVer = qualifierVer;
        this.schema = schema;
        this.alignmentShift = alignmentShift;
        this.containerPreambleSize = containerPreambleSize;
        this.schemaProperties = schemaProperties;
        this.chunkTable = chunkTable;
    }

    public boolean isAcceptedVersion() {
        return minorVer == AssetContainerConstant.CURRENT_MINOR_VER
                && patchVer == AssetContainerConstant.CURRENT_PATCH_VER
                && qualifierVer.equals(AssetContainerConstant.CURRENT_QUALIFIER_VER);
    }

    public int getMinorVer() {
        return minorVer;
    }

    public int getPatchVer() {
        return patchVer;
    }

    public String getQualifierVer() {
        return qualifierVer;
    }

    @Nullable
    public ChunkInfo getChunkInfo(String type) {
        return chunkTable.get(type);
    }

    public int getAlignmentShift() {
        return alignmentShift;
    }

    public int getContainerPreambleSize() {
        return containerPreambleSize;
    }

    public Object2ReferenceMap<String, ChunkInfo> getChunkTable() {
        return chunkTable;
    }

    public String getSchema() {
        return schema;
    }

    @Nullable
    public String getSchemaProperty(short value) {
        return schemaProperties.get(value);
    }

    public record ChunkInfo(String type, String encoding, int offset, int size,
                            int decodeSize, int flags, int alignSize,
                            int alignmentShift, byte @Nullable [] hash) {
    }
}
