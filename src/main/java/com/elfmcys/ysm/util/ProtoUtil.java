package com.elfmcys.ysm.util;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;

public class ProtoUtil {
    public static ProtoSink sink(ArrayBuffer buffer) {
        return ProtoSink.newInstance(buffer.array(), buffer.arrayOffset(), buffer.size());
    }

    public static ProtoSource source(ArrayBuffer buffer) {
        return ProtoSource.newInstance(buffer.array(), buffer.arrayOffset(), buffer.size());
    }

    public static byte[] serializeToArray(ProtoMessage<?> msg) throws IOException {
        var array = new byte[msg.getSerializedSize()];
        var sink = ProtoSink.newInstance(array);
        msg.writeTo(sink);
        return array;
    }

    public static ArrayBuffer serializeToBuffer(ProtoMessage<?> message) throws IOException {
        var result = ArrayBuffer.allocate(message.getSerializedSize());
        try {
            message.writeTo(sink(result));
            return result;
        } catch (IOException | RuntimeException error) {
            result.close();
            throw error;
        }
    }
}
