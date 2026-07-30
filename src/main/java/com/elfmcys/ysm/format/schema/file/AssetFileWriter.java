package com.elfmcys.ysm.format.schema.file;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.buffer.UniBuffer;
import com.elfmcys.ysm.format.container.AssetContainerWriter;
import com.elfmcys.ysm.natives.image.Image;
import com.elfmcys.ysm.util.ProtoUtil;
import it.unimi.dsi.fastutil.ints.IntReferencePair;
import it.unimi.dsi.fastutil.shorts.ShortReferencePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import us.hebi.quickbuf.ProtoMessage;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AssetFileWriter implements AutoCloseable {
    private String schemaId;
    private String summary = "";
    private final List<ShortReferencePair<String>> properties = new ArrayList<>(4);

    private final List<Triple<String, Integer, UniBuffer>> rawList = new ArrayList<>(4);
    private final List<Pair<String, Image>> images = new ArrayList<>(4);   // 不包含 texture
    private final List<IntReferencePair<UniBuffer>> blobList = new ArrayList<>(4);
    private final List<UniBuffer> streamList = new ArrayList<>(4);

    protected void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    protected void addProtoChunk(String type, ProtoMessage<?> value, int compressLevel) throws IOException {
        try (var bufScope = ArrayBuffer.allocateWithScope(value.getSerializedSize())) {
            value.writeTo(ProtoUtil.sink(bufScope.get()));
            rawList.add(Triple.of(type, compressLevel, bufScope.release()));
        }
    }

    protected void addRawChunk(String type, UniBuffer value, int compressLevel) {
        rawList.add(Triple.of(type, compressLevel, value.acquire()));
    }

    public int addProtoBlob(ProtoMessage<?> value, int compressLevel) throws IOException {
        try (var bufScope = ArrayBuffer.allocateWithScope(value.getSerializedSize())) {
            value.writeTo(ProtoUtil.sink(bufScope.get()));
            return addBlob(bufScope.get(), compressLevel);
        }
    }

    public int addBlob(UniBuffer buffer, int compressLevel) {
        blobList.add(IntReferencePair.of(compressLevel, buffer.acquire()));
        return blobList.size();
    }

    public int addStream(UniBuffer buffer) {
        streamList.add(buffer.acquire());
        return streamList.size();
    }

    protected void addImage(String type, Image image) {
        images.add(Pair.of(type, image.share()));
    }

    protected void setProperty(short type, String value) {
        properties.add(ShortReferencePair.of(type, value));
    }

    protected void setSummary(String summary) {
        this.summary = Objects.requireNonNull(summary, "summary");
    }

    public void write(WritableByteChannel output) throws IOException {
        try (var writer = new AssetContainerWriter()) {
            writer.setSchema(schemaId);
            writer.setSummary(summary);
            for (var prop : properties) {
                writer.setSchemaProperty(prop.leftShort(), prop.right());
            }
            for (var proto : rawList) {
                writer.addChunk(proto.getLeft(), "", 0, 0, 0,
                        proto.getRight(), proto.getMiddle());
            }
            for (var img : images) {
                writer.addChunk(img.getKey(),
                        img.getValue().format().name(), packImageSize(img.getRight()), 0, 0,
                        img.getValue().data(), 0);
            }
            for (var i = 0; i < blobList.size(); i++) {
                var pair = blobList.get(i);
                writer.addChunk(AssetFileConstant.BLOB_CHUNK_PREFIX + (i + 1), "", 0, 0, 0,
                        pair.right(), pair.leftInt());
            }
            for (var i = 0; i < streamList.size(); i++) {
                var buf = streamList.get(i);
                writer.addChunk(AssetFileConstant.STREAM_CHUNK_PREFIX + (i + 1), "", 0, 0, 0,
                        buf, 0);
            }
            writer.write(output);
        }
    }

    private int packImageSize(Image image) {
        return (image.width() << 16) | (image.height() & 0xFFFF);
    }

    @Override
    public void close() {
        for (var pair : rawList) {
            pair.getRight().close();
        }
        for (var pair : blobList) {
            pair.right().close();
        }
        for (var buf : streamList) {
            buf.close();
        }
        for (var pair : images) {
            pair.getRight().close();
        }
    }
}
